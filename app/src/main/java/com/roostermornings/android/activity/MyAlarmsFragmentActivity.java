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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
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
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

public class MyAlarmsFragmentActivity extends BaseActivity {

    public static final String TAG = MyAlarmsFragmentActivity.class.getSimpleName();
    private DatabaseReference mMyAlarmsReference;
    private ArrayList<Alarm> mAlarms = new ArrayList<>();

    private DeviceAlarmController deviceAlarmController;

    @BindView(R.id.home_alarmsListView)
    RecyclerView mRecyclerView;

    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;

    @BindView(R.id.home_my_alarms)
    ImageButton buttonAddAlarm;

    @BindView(R.id.button_bar)
    LinearLayout buttonBarLayout;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_my_alarms);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbarTitle.setText(getString(R.string.my_alarms));

        //Check for new Firebase datachange notifications and register broadcast receiver
        updateNotifications();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_alarm);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MyAlarmsFragmentActivity.this, NewAlarmFragmentActivity.class));
            }
        });

        buttonAddAlarm.setBackgroundResource(R.drawable.rooster_button_bar_add_alarm_active);

        mMyAlarmsReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(getFirebaseUser().getUid());

        //Keep local and Firebase alarm dbs synced, and enable offline persistence
        mMyAlarmsReference.keepSynced(true);

        mAdapter = new MyAlarmsListAdapter(mAlarms, MyAlarmsFragmentActivity.this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

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
                    mAlarms.add(alarm);
                    mAdapter.notifyItemInserted(mAlarms.size() - 1);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(MyAlarmsFragmentActivity.this, "Failed to load mAlarms.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        mMyAlarmsReference.addValueEventListener(alarmsListener);
    }

    private void updateNotifications() {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //If notifications waiting, display new friend request notification
        if(((BaseApplication)getApplication()).getNotificationFlag() > 0) setButtonBarNotification(true);

        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction("rooster.update.NOTIFICATION");

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_signout) {
            signOut();
        }

        return super.onOptionsItemSelected(item);
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

    public void deleteAlarm(String setId, String alarmId) {
        DeviceAlarmController deviceAlarmController = new DeviceAlarmController(this);

        //Remove alarm from firebase
        DatabaseReference alarmReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(getFirebaseUser().getUid()).child(alarmId);
        alarmReference.removeValue();

        //Remove alarm *set* from local SQL database using retreived setId from firebase
        deviceAlarmController.deleteAlarmSet(Long.valueOf(setId));

        //TODO: find out why alarm content being duplicated in recycler view on delete - do not refresh the activity like this
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    public void editAlarm(String alarmId){
        Intent intent = new Intent(MyAlarmsFragmentActivity.this, NewAlarmFragmentActivity.class);
        intent.putExtra("alarmId", alarmId);
        startActivity(intent);
    }

    public void setButtonBarNotification(boolean notification) {
        ImageView buttonBarNotification = (ImageView) buttonBarLayout.findViewById(R.id.notification);
        if(notification) buttonBarNotification.setVisibility(View.VISIBLE);
        else buttonBarNotification.setVisibility(View.GONE);
    }
}
