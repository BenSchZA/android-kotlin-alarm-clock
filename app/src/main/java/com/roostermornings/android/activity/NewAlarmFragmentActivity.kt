/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity

import android.content.ContentResolver
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.google.firebase.database.FirebaseDatabase
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.firebase.FA
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.database.Alarm
import com.roostermornings.android.fragment.IAlarmSetListener
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment1
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment2
import com.roostermornings.android.sync.DownloadSyncAdapter
import com.roostermornings.android.util.StrUtils

import java.util.Calendar

import butterknife.BindView
import com.roostermornings.android.keys.Extra
import com.roostermornings.android.keys.PrefsKey

import com.roostermornings.android.util.Constants.AUTHORITY
import com.roostermornings.android.util.Toaster
import kotlinx.android.synthetic.main.activity_new_alarm.*

class NewAlarmFragmentActivity : BaseActivity(), IAlarmSetListener, NewAlarmFragment1.NewAlarmInterface {

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private lateinit var mViewPager: ViewPager
    private var mAlarm = Alarm()
    private var mCalendar = Calendar.getInstance()
    private var mFragment1: Fragment? = null
    private var mFragment2: Fragment? = null
    private var menu: Menu? = null

    @BindView(R.id.toolbar_title)
    lateinit var toolbarTitle: TextView

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(R.layout.activity_new_alarm)
        BaseApplication.roosterApplicationComponent.inject(this)

        // Only performed for android M version, with Doze mode
        if(!requestPermissionIgnoreBatteryOptimization(this)) {
            lifeCycle.directUserToFAQs(false, this, main_layout)
            sharedPreferences.edit()
                    .putBoolean(PrefsKey.USER_VIEWED_FAQS.name, true)
                    .apply()
        }

        val mHour = mCalendar.get(Calendar.HOUR_OF_DAY)
        val mMinute = mCalendar.get(Calendar.MINUTE)

        mAlarm.setMinute(mMinute)
        mAlarm.setHour(mHour)
        mAlarm.isRecurring = false
        mAlarm.isAllow_friend_audio_files = true

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.main_content)
        mViewPager.adapter = mSectionsPagerAdapter

        // Static variable, so clear on new instance
        mEditAlarmId = ""
        if (intent.extras?.containsKey(Extra.ALARM_SET_ID.name) == true) {
            mEditAlarmId = intent.extras?.getString(Extra.ALARM_SET_ID.name, "")
        }
        if (isNewAlarm()) {
            setupToolbar(toolbarTitle, getString(R.string.create_alarm))
            FA.Log(FA.Event.alarm_creation_begin::class.java, null, null)
        } else {
            setupToolbar(toolbarTitle, getString(R.string.edit_alarm))
            FA.Log(FA.Event.alarm_edit_begin::class.java, null, null)
        }
    }

    override fun onBackPressed() {
        if (mViewPager.currentItem == 0) {
            MaterialDialog.Builder(this)
                    .theme(Theme.LIGHT)
                    .content(R.string.dialog_confirm_changes)
                    .positiveText("Save")
                    .negativeText("Discard")
                    .onPositive { _, _ -> saveAndExit() }
                    .onNegative { _, _ -> super@NewAlarmFragmentActivity.onBackPressed() }
                    .show()
        } else {
            mViewPager.currentItem = mViewPager.currentItem - 1
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_new_alarm, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when(item.itemId) {
            R.id.action_next -> {
                if (mViewPager.currentItem == 0)
                    mViewPager.currentItem = 1
                else saveAndExit()
                return true
            }
            else -> {}
        }

        return super.onOptionsItemSelected(item)
    }

    override fun saveAndExit() {
        //Leave activity with thread running
        startHomeActivity()

        object : Thread() {
            override fun run() {
                //save alarm!
                //if no day set, automatically set to correct day of week
                if (!mAlarm.isMonday
                        && !mAlarm.isTuesday
                        && !mAlarm.isWednesday
                        && !mAlarm.isThursday
                        && !mAlarm.isFriday
                        && !mAlarm.isSaturday
                        && !mAlarm.isSunday) {

                    val currentTime = Calendar.getInstance()
                    val alarmTime = Calendar.getInstance()

                    //Ensure both instances have same Millis time
                    alarmTime.timeInMillis = currentTime.timeInMillis

                    alarmTime.clear(Calendar.HOUR_OF_DAY)
                    alarmTime.clear(Calendar.MINUTE)

                    alarmTime.set(Calendar.HOUR_OF_DAY, mAlarm.getHour())
                    alarmTime.set(Calendar.MINUTE, mAlarm.getMinute())

                    Log.d(TAG, currentTime.get(Calendar.DAY_OF_WEEK).toString())
                    Log.d(TAG, alarmTime.get(Calendar.DAY_OF_WEEK).toString())
                    Log.d(TAG, alarmTime.get(Calendar.HOUR_OF_DAY).toString())

                    //Roll alarm time forward by one day until greater than current time
                    if (currentTime >= alarmTime) {
                        alarmTime.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    mAlarm.setAlarmDayFromCalendar(alarmTime)
                }

                val alarmDays = mAlarm.days
                val database = FirebaseDatabase.getInstance()

                //only do the push to create the new alarm if this is NOT an existing alarm
                val alarmKey: String?
                if (!isNewAlarm()) {
                    alarmKey = mEditAlarmId
                } else {
                    alarmKey = mDatabase.child("alarms").push().key
                    mAlarm.setUid(alarmKey)
                }

                //Extract data from Alarm mAlarm and create new alarm set DeviceAlarm
                val alarmChannelUID =  mAlarm.channel?.getId() ?: ""

                //if this is an existing alarm, delete from local storage before inserting another record
                if (StrUtils.notNullOrEmpty(mEditAlarmId) && StrUtils.notNullOrEmpty(mAlarm.uid)) {
                    deviceAlarmController.deleteAlarmSetGlobal(mAlarm.uid)
                }

                //Set enabled flag to true on new or edited alarm
                mAlarm.isEnabled = true

                if(alarmKey != null) {
                    deviceAlarmController.registerAlarmSet(mAlarm.isEnabled, alarmKey, mAlarm.getHour(), mAlarm.getMinute(), alarmDays, mAlarm.isRecurring, alarmChannelUID, mAlarm.isAllow_friend_audio_files)
                } else {
                    Toaster.makeToast(applicationContext, "Failed to set alarm, please try again.", Toast.LENGTH_LONG)
                    return
                }

                //Update firebase
                if(firebaseUser?.uid?.isNotBlank() == true && alarmKey.isNotBlank()) {
                    database.getReference("alarms/${firebaseUser?.uid}/$alarmKey").setValue(mAlarm)
                }

                //Download any social or channel audio files
                ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.forceBundle)

                FA.LogMany(FA.Event.alarm_creation_completed::class.java,
                        arrayOf(FA.Event.alarm_creation_completed.Param.social_roosters_enabled, FA.Event.alarm_creation_completed.Param.channel_selected),
                        arrayOf<Any>(mAlarm.isAllow_friend_audio_files, "" != mAlarm.channel.name))

                if (mAlarm.channel.name?.isEmpty() == true) {
                    FA.Log(FA.Event.channel_selected::class.java,
                            FA.Event.channel_selected.Param.channel_title,
                            mAlarm.channel.name)
                }
            }
        }.run()
    }

    override fun setAlarmDetails(alarm: Alarm) {
        mAlarm = alarm
    }

    override fun getAlarmDetails(): Alarm {
        return mAlarm
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment? {

            val fragment: Fragment? = null

            when (position) {
                0 -> {
                    mFragment1 = NewAlarmFragment1.newInstance(firebaseUser?.uid)
                    return mFragment1
                }
                1 -> {
                    mFragment2 = NewAlarmFragment2.newInstance(firebaseUser?.uid)
                    return mFragment2
                }
            }
            return fragment
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "Create an alarm"
                1 -> return "Create an alarm"
            }
            return null
        }
    }

    override fun configureAlarmDetails() {

        if (!isNewAlarm()) {
            // Find alarm set with matching ID in SQL
            val tempAlarms = deviceAlarmTableManager.getAlarmSet(mEditAlarmId)
            val alarmSetExistsInSQL = tempAlarms.size >= 1

            // If alarm set with matching ID in SQL
            if (alarmSetExistsInSQL) {
                // Recreate Alarm object from SQL DeviceAlarm entry
                mAlarm.fromDeviceAlarm(tempAlarms[0], deviceAlarmTableManager.isSetEnabled(mEditAlarmId))
                if (mAlarm.isRecurring) {
                    val alarmDays = deviceAlarmTableManager.getAlarmClassDays(mEditAlarmId)
                    mAlarm.setAlarmDaysFromDeviceAlarm(alarmDays)
                }

                // Configure alarm creation settings UI from Alarm object
                if (mFragment1 is NewAlarmFragment1) {
                    (mFragment1 as NewAlarmFragment1).configureEditedAlarmUISettings()
                }
            }
        }

        // Select alarm channel whether new alarm or not
        if (mFragment2 is NewAlarmFragment2) {
            (mFragment2 as NewAlarmFragment2).selectAlarmChannel()
        }
    }

    private fun isNewAlarm(): Boolean {
        return mEditAlarmId.isNullOrBlank()
    }

    override fun setNextButtonCaption(text: String) {
        menu?.let {
            val itemNext = it.findItem(R.id.action_next)
            itemNext.title = text
        }
    }

    companion object {
        val TAG: String = NewAlarmFragmentActivity::class.java.simpleName
        var mEditAlarmId: String? = ""

        val currentAlarmId: String
            get() = if (mEditAlarmId != null)
                mEditAlarmId as String
            else
                ""
    }
}