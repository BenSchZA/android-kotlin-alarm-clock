/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.NewAlarmFragment1;
import com.roostermornings.android.fragment.NewAlarmFragment2;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;

import static com.roostermornings.android.util.RoosterUtils.hasGingerbread;

public class NewAlarmFragmentActivity extends BaseActivity implements IAlarmSetListener {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    public final static String TAG = NewAlarmFragmentActivity.class.getSimpleName();
    private ViewPager mViewPager;
    Alarm mAlarm = new Alarm();
    private String mEditAlarmId = "";
    Calendar mCalendar = Calendar.getInstance();
    private Fragment mFragment1;
    private Fragment mFragment2;
    private Menu menu;
    private DeviceAlarmController deviceAlarmController = new DeviceAlarmController(this);


    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_new_alarm);

        if (hasGingerbread()) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //Only performed for android M version, with Doze mode
        requestPermissionIgnoreBatteryOptimization();

        int mHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = mCalendar.get(Calendar.MINUTE);

        mAlarm.setMinute(mMinute);
        mAlarm.setHour(mHour);
        mAlarm.setRecurring(false);
        mAlarm.setVibrate(true);
        mAlarm.setAllow_friend_audio_files(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("alarmId")) {
            mEditAlarmId = extras.getString("alarmId", "");
        }
        if (mEditAlarmId.length() == 0) {
            toolbarTitle.setText(getString(R.string.create_alarm));
        } else {
            toolbarTitle.setText(getString(R.string.edit_alarm));
        }

    }


    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 0) {
            super.onBackPressed();
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

                    alarmTime.clear(Calendar.HOUR);
                    alarmTime.clear(Calendar.MINUTE);

                    alarmTime.set(Calendar.HOUR_OF_DAY, mAlarm.getHour());
                    alarmTime.set(Calendar.MINUTE, mAlarm.getMinute());

                    Log.d(TAG, String.valueOf(currentTime.get(Calendar.DAY_OF_WEEK)));
                    Log.d(TAG, String.valueOf(alarmTime.get(Calendar.DAY_OF_WEEK)));
                    Log.d(TAG, String.valueOf(alarmTime.get(Calendar.HOUR_OF_DAY)));

                    if (currentTime.compareTo(alarmTime) > 0) {
                        alarmTime.add(Calendar.HOUR, 24);
                    }

                    setAlarmDay(alarmTime);
                }

                List<Integer> alarmDays = new ArrayList<>();
                alarmDays = mAlarm.getDays();

                FirebaseDatabase database = FirebaseDatabase.getInstance();

                //only do the push to create the new alarm if this is NOT an existing alarm
                String alarmKey = "";
                if (mEditAlarmId.length() == 0) {
                    alarmKey = mDatabase.child("alarms").push().getKey();
                    mAlarm.setUid(alarmKey);
                } else {
                    alarmKey = mEditAlarmId;
                }

                //Extract data from Alarm mAlarm and create new alarm set DeviceAlarm
                AlarmChannel alarmChannel = mAlarm.getChannel();
                String alarmChannelUID = "";
                if(alarmChannel != null) alarmChannelUID = alarmChannel.getId();

                DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(this);
                Integer iteration = deviceAlarmTableManager.getChannelStoryIteration(alarmChannelUID);
                //if this is an existing alarm, delete from local storage before inserting another record
                if (mEditAlarmId.length() != 0
                        && mAlarm.getUid().length() > 0) {
                    deviceAlarmController.deleteAlarmSetGlobal(mAlarm.getUid());
                }

                //Set enabled flag to true on new or edited alarm
                mAlarm.setEnabled(true);

                deviceAlarmController.registerAlarmSet(alarmKey, mAlarm.getHour(), mAlarm.getMinute(), alarmDays, mAlarm.isRecurring(), mAlarm.isVibrate(), alarmChannelUID, mAlarm.isAllow_friend_audio_files());
                //Ensure iteration is not overwritten TODO: maybe don't set in SQL db initialisation and set defaults SQL entry value
                if(iteration != null) deviceAlarmTableManager.setChannelStoryIteration(alarmChannelUID, iteration);

                database.getReference(String.format("alarms/%s/%s", mAuth.getCurrentUser().getUid(), alarmKey)).setValue(mAlarm);

                Toast.makeText(getBaseContext(), (mEditAlarmId.length() == 0) ? "Alarm created!" : "Alarm edited!",
                        Toast.LENGTH_LONG).show();
                startHomeActivity();
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setAlarmDetails(Alarm alarm) {
        mAlarm = alarm;
    }

    @Override
    public Alarm getAlarmDetails() {
        return mAlarm;
    }

    private void setAlarmDay(Calendar alarmTime) {

        switch (alarmTime.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                mAlarm.setSunday(true);
                break;
            case Calendar.MONDAY:
                mAlarm.setMonday(true);
                break;
            case Calendar.TUESDAY:
                mAlarm.setTuesday(true);
                break;
            case Calendar.WEDNESDAY:
                mAlarm.setWednesday(true);
                break;
            case Calendar.THURSDAY:
                mAlarm.setThursday(true);
                break;
            case Calendar.FRIDAY:
                mAlarm.setFriday(true);
                break;
            case Calendar.SATURDAY:
                mAlarm.setSaturday(true);
                break;
        }

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
                    mFragment1 = NewAlarmFragment1.newInstance(getFirebaseUser().getUid());
                    return mFragment1;
                case 1:
                    mFragment2 = NewAlarmFragment2.newInstance(getFirebaseUser().getUid());
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
    public void retrieveAlarmDetailsFromFirebase() {

        if (mEditAlarmId.length() == 0) return;

        DatabaseReference alarmReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(getFirebaseUser().getUid()).child(mEditAlarmId);

        ValueEventListener alarmListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mAlarm = dataSnapshot.getValue(Alarm.class);
                mAlarm.setUid(mEditAlarmId);

                if (mFragment1 instanceof NewAlarmFragment1) {
                    ((NewAlarmFragment1) mFragment1).setEditedAlarmSettings();
                }
                if (mFragment2 instanceof NewAlarmFragment2) {
                    ((NewAlarmFragment2) mFragment2).selectEditedAlarmChannel();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(NewAlarmFragmentActivity.this, "Failed to load mAlarms.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        alarmReference.addListenerForSingleValueEvent(alarmListener);


    }

    @Override
    public void setNextButtonCaption(String text) {
        MenuItem itemNext = menu.findItem(R.id.action_next);
        itemNext.setTitle(text);
    }
}