package com.roostermornings.android.activity

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.database.Alarm
import com.roostermornings.android.firebase.FA
import com.roostermornings.android.keys.Action
import com.roostermornings.android.keys.Extra
import com.roostermornings.android.sqlutil.DeviceAlarmController
import com.roostermornings.android.sync.DownloadSyncAdapter
import com.roostermornings.android.util.*
import java.util.*
import javax.inject.Inject

/**
 * Created by bscholtz on 2018/02/28.
 */
class VoiceActionActivity: BaseActivity() {

    @Inject
    lateinit var jsonPersistence: JSONPersistence

    override fun inject(component: RoosterApplicationComponent) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BaseApplication.roosterApplicationComponent.inject(this)

        val deviceAlarmController = DeviceAlarmController(this)

        when(intent.action) {
            AlarmClock.ACTION_SET_ALARM -> {
                var days = ArrayList<Int>()
                var hour: Int = -1
                var minutes: Int = -1
                var vibrateEnabled = false
                var ringtone = ""

                if (intent.hasExtra(AlarmClock.EXTRA_DAYS)) {
                    days = intent.getIntegerArrayListExtra(AlarmClock.ACTION_SET_ALARM)
                }

                if (intent.hasExtra(AlarmClock.EXTRA_HOUR)) {
                    hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1)
                }

                if (intent.hasExtra(AlarmClock.EXTRA_MINUTES)) {
                    minutes = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1)
                }

                if (RoosterUtils.hasKitKat() && intent.hasExtra(AlarmClock.EXTRA_VIBRATE)) {
                    vibrateEnabled = intent.getBooleanExtra(AlarmClock.EXTRA_VIBRATE, false)
                }

                if (RoosterUtils.hasKitKat() && intent.hasExtra(AlarmClock.EXTRA_RINGTONE)) {
                    ringtone = intent.getStringExtra(AlarmClock.EXTRA_RINGTONE)
                }

                val alarm = Alarm()
                alarm.setHour(hour)
                alarm.setMinute(minutes)
                alarm.setAlarmDaysFromDeviceAlarm(days)
                alarm.isEnabled = true
                alarm.isRecurring = days.isNotEmpty()
                //alarm.channel = jsonPersistence.defaultChannel

                if (!alarm.isMonday
                        && !alarm.isTuesday
                        && !alarm.isWednesday
                        && !alarm.isThursday
                        && !alarm.isFriday
                        && !alarm.isSaturday
                        && !alarm.isSunday) {

                    val currentTime = Calendar.getInstance()
                    val alarmTime = Calendar.getInstance()

                    //Ensure both instances have same Millis time
                    alarmTime.timeInMillis = currentTime.timeInMillis

                    alarmTime.clear(Calendar.HOUR_OF_DAY)
                    alarmTime.clear(Calendar.MINUTE)

                    alarmTime.set(Calendar.HOUR_OF_DAY, hour)
                    alarmTime.set(Calendar.MINUTE, minutes)

                    //Roll alarm time forward by one day until greater than current time
                    if (currentTime >= alarmTime) {
                        alarmTime.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    alarm.setAlarmDayFromCalendar(alarmTime)
                }

                val database = FirebaseDatabase.getInstance()

                //only do the push to create the new alarm if this is NOT an existing alarm
                val alarmKey = mDatabase.child("alarms").push().key
                alarm.setUid(alarmKey)

                //Extract data from Alarm mAlarm and create new alarm set DeviceAlarm
                val alarmChannelUID =  alarm.channel?.getId() ?: ""

                //if this is an existing alarm, delete from local storage before inserting another record
                if (StrUtils.notNullOrEmpty(NewAlarmFragmentActivity.mEditAlarmId) && StrUtils.notNullOrEmpty(alarm.uid)) {
                    deviceAlarmController.deleteAlarmSetGlobal(alarm.uid)
                }

                if(alarmKey != null) {
                    deviceAlarmController.registerAlarmSet(alarm.isEnabled, alarmKey, alarm.hour, alarm.minute, alarm.days, alarm.isRecurring, alarmChannelUID, alarm.isAllow_friend_audio_files)
                } else {
                    Toaster.makeToast(applicationContext, "Failed to set alarm, please try again.", Toast.LENGTH_LONG)
                    return
                }

                //Update firebase
                if(firebaseUser?.uid?.isNotBlank() == true && alarmKey.isNotBlank()) {
                    database.getReference("alarms/${firebaseUser?.uid}/$alarmKey").setValue(alarm)
                }

                //Download any social or channel audio files
                ContentResolver.requestSync(mAccount, Constants.AUTHORITY, DownloadSyncAdapter.forceBundle)

                FA.LogMany(FA.Event.alarm_creation_completed::class.java,
                        arrayOf(FA.Event.alarm_creation_completed.Param.social_roosters_enabled, FA.Event.alarm_creation_completed.Param.channel_selected),
                        arrayOf<Any>(alarm.isAllow_friend_audio_files, "" != alarm.channel.name))

                if (alarm.channel.name?.isEmpty() == true) {
                    FA.Log(FA.Event.channel_selected::class.java,
                            FA.Event.channel_selected.Param.channel_title,
                            alarm.channel.name)
                }

                finish()
            }
            AlarmClock.ACTION_SHOW_ALARMS -> {
                startActivity(Intent(this, MyAlarmsFragmentActivity::class.java))
            }
            AlarmClock.ACTION_DISMISS_ALARM -> {
                val launchIntent = Intent(this, MyAlarmsFragmentActivity::class.java)
                launchIntent.putExtra(Extra.ALARM_SET_ID.name, deviceAlarmTableManager.nextPendingAlarm.setId)
                launchIntent.action = Action.CANCEL_SNOOZE.name
                startActivity(launchIntent)
            }
        }
    }
}