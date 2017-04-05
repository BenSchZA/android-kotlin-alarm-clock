/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.util.Constants;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

public class MyAlarmsFragmentActivity extends BaseActivity {

    public static final String TAG = MyAlarmsFragmentActivity.class.getSimpleName();
    @BindView(R.id.home_alarmsListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.button_bar)
    LinearLayout buttonBarLayout;

    private DatabaseReference mMyAlarmsReference;
    private ArrayList<Alarm> mAlarms = new ArrayList<>();
    private DeviceAlarmController deviceAlarmController;
    private DeviceAlarmTableManager deviceAlarmTableManager;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_my_alarms);

        setDayNight();

        //Set toolbar title
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbarTitle.setText(getString(R.string.my_alarms));

        mMyAlarmsReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(getFirebaseUser().getUid());

        //Keep local and Firebase alarm dbs synced, and enable offline persistence
        mMyAlarmsReference.keepSynced(true);

        mAdapter = new MyAlarmsListAdapter(mAlarms, MyAlarmsFragmentActivity.this, getApplication());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        //Check for new Firebase datachange notifications and register broadcast receiver
        updateRequestNotification();

        deviceAlarmController = new DeviceAlarmController(this);
        deviceAlarmTableManager = new DeviceAlarmTableManager(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("message")) {
            String fcm_message = extras.getString("message", "");
            if (fcm_message.length() > 0) {

                new MaterialDialog.Builder(getApplicationContext())
                        .title("Hey, we thought you should know!")
                        .content(fcm_message)
                        .positiveText(R.string.ok)
                        .negativeText("")
                        .show();
            }
        }

        ValueEventListener alarmsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Alarm alarm = postSnapshot.getValue(Alarm.class);

                    //Register alarm sets on login
                    //Extract data from Alarm "alarm" and create new alarm set DeviceAlarm
                    AlarmChannel alarmChannel = alarm.getChannel();
                    String alarmChannelUID = "";
                    if(alarmChannel != null) alarmChannelUID = alarmChannel.getId();

                    //If alarm from firebase does not exist locally, create it
                    if(!deviceAlarmTableManager.isSetInDB(alarm.getUid()) && alarm.isEnabled()) {
                        deviceAlarmController.registerAlarmSet(alarm.getUid(), alarm.getHour(), alarm.getMinute(),
                                alarm.getDays(), alarm.isRecurring(), alarm.isVibrate(), alarmChannelUID, alarm.isAllow_friend_audio_files());
                    }

                    //Check SQL db to see if all alarms in set have fired
                    alarm.setEnabled(deviceAlarmTableManager.isSetEnabled(alarm.getUid()));
                    deviceAlarmController.setSetEnabled(alarm.getUid(), alarm.isEnabled());

                    //Add alarm to display array
                    mAlarms.add(alarm);
                    mAdapter.notifyItemInserted(mAlarms.size() - 1);
                    updateRoosterNotification();
                }
                //Recreate all enabled alarms as failsafe
                deviceAlarmController.rebootAlarms();
                //Case: local has an alarm that firebase doesn't Result: delete local alarm
                deviceAlarmController.syncAlarmSetGlobal(mAlarms);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                if(BuildConfig.DEBUG) Toast.makeText(MyAlarmsFragmentActivity.this, "Failed to load mAlarms.",
                        Toast.LENGTH_SHORT).show();
            }
        }; mMyAlarmsReference.addListenerForSingleValueEvent(alarmsListener);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void updateRoosterNotification() {
        AudioTableManager audioTableManager = new AudioTableManager(this);
        Integer roosterCount = audioTableManager.countSocialAudioFiles();

        if (roosterCount > 0) {
            DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(getApplicationContext());
            DeviceAlarm deviceAlarm  = deviceAlarmTableManager.getNextPendingAlarm();

            if(deviceAlarm == null) for (Alarm alarm: mAlarms) alarm.setUnseen_roosters(0);
            else {
                for (Alarm alarm : mAlarms) {
                    alarm.setUnseen_roosters(0);
                    if (alarm.getUid().equals(deviceAlarm.getSetId()))
                        alarm.setUnseen_roosters(roosterCount);
                }
            }
            mAdapter.notifyDataSetChanged();
        }

        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction(Constants.ACTION_ROOSTERNOTIFICATION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                Integer roosterCount = ((BaseApplication) getApplication()).getNotificationFlag(Constants.FLAG_ROOSTERCOUNT);

                if(roosterCount > 0){
                    DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(getApplicationContext());
                    DeviceAlarm deviceAlarm  = deviceAlarmTableManager.getNextPendingAlarm();

                    if(deviceAlarm == null) return;
                    for (Alarm alarm:
                         mAlarms) {
                        alarm.setUnseen_roosters(0);
                        if(alarm.getUid().equals(deviceAlarm.getSetId())) {
                            alarm.setUnseen_roosters(roosterCount);
                            mAdapter.notifyDataSetChanged();
                            return;
                        }
                    }
                }
            }
        }; registerReceiver(receiver, firebaseListenerServiceFilter);
    }

    private void updateRequestNotification() {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //If notifications waiting, display new friend request notification
        if (((BaseApplication) getApplication()).getNotificationFlag(Constants.FLAG_FRIENDREQUESTS) > 0)
            setButtonBarNotification(true);

        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction(Constants.ACTION_REQUESTNOTIFICATION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                setButtonBarNotification(true);
            }
        };
        registerReceiver(receiver, firebaseListenerServiceFilter);
    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_add_alarm) {
            startActivity(new Intent(MyAlarmsFragmentActivity.this, NewAlarmFragmentActivity.class));
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_profile) {
            return true;
        }
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_signout) {
            signOut();
        }

        return super.onOptionsItemSelected(item);
    }

    public void toggleAlarmSetEnable(Alarm alarm, boolean enabled) {
        deviceAlarmController.setSetEnabled(alarm.getUid(), enabled);
        mAlarms.get(mAlarms.indexOf(alarm)).setEnabled(enabled);
        updateRoosterNotification();
        mAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.home_record_audio)
    public void recordNewAudio() {
        if (!checkInternetConnection()) return;
        startActivity(new Intent(MyAlarmsFragmentActivity.this, NewAudioRecordActivity.class));
    }

    @OnClick(R.id.home_friends)
    public void manageFriends() {
        startActivity(new Intent(MyAlarmsFragmentActivity.this, FriendsFragmentActivity.class));
    }

    @OnClick(R.id.home_my_uploads)
    public void manageUploads() {
        startActivity(new Intent(MyAlarmsFragmentActivity.this, MessageStatusActivity.class));
    }

    public void deleteAlarm(String alarmId) {
        try {
            DeviceAlarmController deviceAlarmController = new DeviceAlarmController(this);

            //Remove alarm *set* from local SQL database using retrieved Uid from firebase && Remove alarm from firebase
            deviceAlarmController.deleteAlarmSetGlobal(alarmId);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        //TODO: find out why alarm content being duplicated in recycler view on delete - do not refresh the activity like this
//        Intent intent = getIntent();
//        finish();
//        startActivity(intent);
    }

    public void editAlarm(String alarmId) {
        Intent intent = new Intent(MyAlarmsFragmentActivity.this, NewAlarmFragmentActivity.class);
        intent.putExtra(Constants.EXTRA_ALARMID, alarmId);
        startActivity(intent);
    }

    public void setButtonBarNotification(boolean notification) {
        ImageView buttonBarNotification = (ImageView) buttonBarLayout.findViewById(R.id.notification);
        if (notification) buttonBarNotification.setVisibility(View.VISIBLE);
        else buttonBarNotification.setVisibility(View.GONE);
    }
}
