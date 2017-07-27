/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment1;
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment2;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sync.DownloadSyncAdapter;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.StrUtils;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

import static com.roostermornings.android.util.Constants.AUTHORITY;
import static com.roostermornings.android.util.RoosterUtils.hasGingerbread;

public class NewAlarmFragmentActivity extends BaseActivity implements IAlarmSetListener, NewAlarmFragment1.NewAlarmInterface {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    public final static String TAG = NewAlarmFragmentActivity.class.getSimpleName();
    private ViewPager mViewPager;
    Alarm mAlarm = new Alarm();
    public static String mEditAlarmId = "";
    Calendar mCalendar = Calendar.getInstance();
    private Fragment mFragment1;
    private Fragment mFragment2;
    private Menu menu;

    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;

    @Inject DeviceAlarmController deviceAlarmController;
    @Inject DeviceAlarmTableManager deviceAlarmTableManager;
    @Inject @Nullable FirebaseUser mCurrentUser;
    @Inject Account mAccount;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_new_alarm);
        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

        setDayNightTheme();

        if (hasGingerbread()) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //Only performed for android M version, with Doze mode
        requestPermissionIgnoreBatteryOptimization(this);

        int mHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = mCalendar.get(Calendar.MINUTE);

        mAlarm.setMinute(mMinute);
        mAlarm.setHour(mHour);
        mAlarm.setRecurring(false);
        mAlarm.setAllow_friend_audio_files(true);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.main_content);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        Bundle extras = getIntent().getExtras();
        //Static variable, so clear on new instance
        mEditAlarmId = "";
        if (extras != null && extras.containsKey(Constants.EXTRA_ALARMID)) {
            mEditAlarmId = extras.getString(Constants.EXTRA_ALARMID, "");
        }
        if (mEditAlarmId.length() == 0) {
            setupToolbar(toolbarTitle, getString(R.string.create_alarm));
            FA.Log(FA.Event.alarm_creation_begin.class, null, null);
        } else {
            setupToolbar(toolbarTitle, getString(R.string.edit_alarm));
            FA.Log(FA.Event.alarm_edit_begin.class, null, null);
        }
    }

    public static String getCurrentAlarmId() {
        if(mEditAlarmId != null) return mEditAlarmId;
        else return "";
    }

    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 0) {

            new MaterialDialog.Builder(this)
                    .theme(Theme.LIGHT)
                    .content(R.string.dialog_confirm_changes)
                    .positiveText("Save")
                    .negativeText("Discard")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            saveAndExit();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            NewAlarmFragmentActivity.super.onBackPressed();
                        }
                    })
                    .show();
        } else {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_alarm, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_next) {
            if (mViewPager.getCurrentItem() == 0) mViewPager.setCurrentItem(1);
            else {
                saveAndExit();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void saveAndExit() {
        //Leave activity with thread running
        startHomeActivity();
        //Context for use in thread
        final Context context = this;
        new Thread() {
            @Override
            public void run() {
                //save alarm!
                //if no day set, automatically set to correct day of week
                if (!mAlarm.isMonday()
                        && !mAlarm.isTuesday()
                        && !mAlarm.isWednesday()
                        && !mAlarm.isThursday()
                        && !mAlarm.isFriday()
                        && !mAlarm.isSaturday()
                        && !mAlarm.isSunday()) {

                    Calendar currentTime = Calendar.getInstance();
                    Calendar alarmTime = Calendar.getInstance();

                    //Ensure both instances have same Millis time
                    alarmTime.setTimeInMillis(currentTime.getTimeInMillis());

                    alarmTime.clear(Calendar.HOUR_OF_DAY);
                    alarmTime.clear(Calendar.MINUTE);

                    alarmTime.set(Calendar.HOUR_OF_DAY, mAlarm.getHour());
                    alarmTime.set(Calendar.MINUTE, mAlarm.getMinute());

                    Log.d(TAG, String.valueOf(currentTime.get(Calendar.DAY_OF_WEEK)));
                    Log.d(TAG, String.valueOf(alarmTime.get(Calendar.DAY_OF_WEEK)));
                    Log.d(TAG, String.valueOf(alarmTime.get(Calendar.HOUR_OF_DAY)));

                    //Roll alarm time forward by one day until greater than current time
                    if (currentTime.compareTo(alarmTime) >= 0) {
                        alarmTime.roll(Calendar.DATE, true);
                    }

                    mAlarm.setAlarmDayFromCalendar(alarmTime);
                }

                List<Integer> alarmDays;
                alarmDays = mAlarm.getDays();

                FirebaseDatabase database = FirebaseDatabase.getInstance();

                //only do the push to create the new alarm if this is NOT an existing alarm
                String alarmKey = "";
                if(StrUtils.notNullOrEmpty(mEditAlarmId)) {
                    alarmKey = mEditAlarmId;
                } else {
                    alarmKey = mDatabase.child("alarms").push().getKey();
                    mAlarm.setUid(alarmKey);
                }

                //Extract data from Alarm mAlarm and create new alarm set DeviceAlarm
                AlarmChannel alarmChannel = mAlarm.getChannel();
                String alarmChannelUID = "";
                if(alarmChannel != null) alarmChannelUID = alarmChannel.getId();

                //if this is an existing alarm, delete from local storage before inserting another record
                if (StrUtils.notNullOrEmpty(mEditAlarmId)
                        && StrUtils.notNullOrEmpty(mAlarm.getUid())) {
                    deviceAlarmController.deleteAlarmSetGlobal(mAlarm.getUid());
                }

                //Set enabled flag to true on new or edited alarm
                mAlarm.setEnabled(true);

                deviceAlarmController.registerAlarmSet(mAlarm.isEnabled(), alarmKey, mAlarm.getHour(), mAlarm.getMinute(), alarmDays, mAlarm.isRecurring(), alarmChannelUID, mAlarm.isAllow_friend_audio_files());

                //Update firebase
                database.getReference(String.format("alarms/%s/%s", mCurrentUser.getUid(), alarmKey)).setValue(mAlarm);

                //Download any social or channel audio files
                ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle());

                FA.Log(FA.Event.alarm_creation_completed.class,
                        FA.Event.alarm_creation_completed.Param.social_roosters_enabled,
                        mAlarm.isAllow_friend_audio_files());
                FA.Log(FA.Event.alarm_creation_completed.class,
                        FA.Event.alarm_creation_completed.Param.channel_selected,
                        !"".equals(mAlarm.getChannel().getName()));

                if(StrUtils.notNullOrEmpty(mAlarm.getChannel().getName())) {
                    FA.Log(FA.Event.channel_selected.class,
                            FA.Event.channel_selected.Param.channel_title,
                            mAlarm.getChannel().getName());
                }
            }
        }.run();
    }

    @Override
    public void setAlarmDetails(Alarm alarm) {
        mAlarm = alarm;
    }

    @Override
    public Alarm getAlarmDetails() {
        return mAlarm;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment fragment = null;

            switch (position) {
                case 0:
                    mFragment1 = NewAlarmFragment1.newInstance(mCurrentUser.getUid());
                    return mFragment1;
                case 1:
                    mFragment2 = NewAlarmFragment2.newInstance(mCurrentUser.getUid());
                    return mFragment2;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Create an alarm";
                case 1:
                    return "Create an alarm";
            }
            return null;
        }
    }

    @Override
    public void retrieveAlarmDetailsFromSQL() {

        if (mEditAlarmId.length() == 0) return;

        List<DeviceAlarm> tempAlarms = deviceAlarmTableManager.getAlarmSet(mEditAlarmId);
        if(tempAlarms.size() < 1) return;
        mAlarm.fromDeviceAlarm(tempAlarms.get(0), deviceAlarmTableManager.isSetEnabled(mEditAlarmId));
        if(mAlarm.isRecurring()) {
            List<Integer> alarmDays = deviceAlarmTableManager.getAlarmClassDays(mEditAlarmId);
            mAlarm.setAlarmDayFromDeviceAlarm(alarmDays);
        }

        if (mFragment1 instanceof NewAlarmFragment1) {
            ((NewAlarmFragment1) mFragment1).setEditedAlarmSettings();
        }
        if (mFragment2 instanceof NewAlarmFragment2) {
            ((NewAlarmFragment2) mFragment2).selectEditedAlarmChannel();
        }
    }

    @Override
    public void setNextButtonCaption(String text) {
        if(menu != null) {
            MenuItem itemNext = menu.findItem(R.id.action_next);
            itemNext.setTitle(text);
        }
    }
}