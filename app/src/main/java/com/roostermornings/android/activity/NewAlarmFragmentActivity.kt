/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity

import android.content.ContentResolver
import android.os.Bundle
import android.os.StrictMode
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.google.firebase.auth.FirebaseUser
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
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.StrUtils

import java.util.Calendar

import javax.inject.Inject

import butterknife.BindView

import com.roostermornings.android.util.Constants.AUTHORITY
import com.roostermornings.android.util.RoosterUtils.hasGingerbread

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
        BaseApplication.getRoosterApplicationComponent().inject(this)

        setDayNightTheme()

        if (hasGingerbread()) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        //Only performed for android M version, with Doze mode
        requestPermissionIgnoreBatteryOptimization(this)

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

        //Static variable, so clear on new instance
        mEditAlarmId = ""
        if (intent.extras.containsKey(Constants.EXTRA_ALARMID)) {
            mEditAlarmId = intent.extras.getString(Constants.EXTRA_ALARMID, "")
        }
        if (mEditAlarmId?.isEmpty() == true) {
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
                if (mEditAlarmId?.isEmpty() == true) {
                    alarmKey = mEditAlarmId
                } else {
                    alarmKey = mDatabase.child("alarms").push().key
                    mAlarm.setUid(alarmKey)
                }

                //Extract data from Alarm mAlarm and create new alarm set DeviceAlarm
                val alarmChannel = mAlarm.getChannel()
                var alarmChannelUID = ""
                if (alarmChannel != null) alarmChannelUID = alarmChannel.getId()

                //if this is an existing alarm, delete from local storage before inserting another record
                if (StrUtils.notNullOrEmpty(mEditAlarmId) && StrUtils.notNullOrEmpty(mAlarm.getUid())) {
                    deviceAlarmController.deleteAlarmSetGlobal(mAlarm.getUid())
                }

                //Set enabled flag to true on new or edited alarm
                mAlarm.isEnabled = true

                deviceAlarmController.registerAlarmSet(mAlarm.isEnabled, alarmKey, mAlarm.getHour(), mAlarm.getMinute(), alarmDays, mAlarm.isRecurring, alarmChannelUID, mAlarm.isAllow_friend_audio_files)

                //Update firebase
                database.getReference(String.format("alarms/%s/%s", firebaseUser?.uid, alarmKey)).setValue(mAlarm)

                //Download any social or channel audio files
                ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle())

                FA.LogMany(FA.Event.alarm_creation_completed::class.java,
                        arrayOf(FA.Event.alarm_creation_completed.Param.social_roosters_enabled, FA.Event.alarm_creation_completed.Param.channel_selected),
                        arrayOf<Any>(mAlarm.isAllow_friend_audio_files, "" != mAlarm.getChannel().getName()))

                if (mAlarm.getChannel().getName()?.isEmpty() == true) {
                    FA.Log(FA.Event.channel_selected::class.java,
                            FA.Event.channel_selected.Param.channel_title,
                            mAlarm.getChannel().getName())
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

    override fun retrieveAlarmDetailsFromSQL() {

        if (mEditAlarmId?.isEmpty() == true) return

        val tempAlarms = deviceAlarmTableManager.getAlarmSet(mEditAlarmId)
        if (tempAlarms.size < 1) return

        mAlarm.fromDeviceAlarm(tempAlarms[0], deviceAlarmTableManager.isSetEnabled(mEditAlarmId))

        if (mAlarm.isRecurring) {
            val alarmDays = deviceAlarmTableManager.getAlarmClassDays(mEditAlarmId)
            mAlarm.setAlarmDaysFromDeviceAlarm(alarmDays)
        }

        if (mFragment1 is NewAlarmFragment1) {
            (mFragment1 as NewAlarmFragment1).setEditedAlarmSettings()
        }
        if (mFragment2 is NewAlarmFragment2) {
            (mFragment2 as NewAlarmFragment2).selectEditedAlarmChannel()
        }
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