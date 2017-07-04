/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.analytics.FA;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.firebase.FirebaseNetwork;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.sync.DownloadSyncAdapter;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.StrUtils;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

import static com.roostermornings.android.util.Constants.AUTHORITY;

public class MyAlarmsFragmentActivity extends BaseActivity {

    private static final String TAG = MyAlarmsFragmentActivity.class.getSimpleName();

    @BindView(R.id.home_alarmsListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.button_bar)
    LinearLayout buttonBarLayout;
    @BindView(R.id.add_alarm)
    FloatingActionButton buttonAddAlarm;

    private final ArrayList<Alarm> mAlarms = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;

    private BroadcastReceiver receiver;

    @Inject DeviceAlarmController deviceAlarmController;
    @Inject DeviceAlarmTableManager deviceAlarmTableManager;
    @Inject AudioTableManager audioTableManager;
    @Inject BaseApplication baseApplication;
    @Inject FirebaseUser firebaseUser;
    @Inject Account mAccount;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_my_alarms);
        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

        //Final context to be used in threads
        final Context context = this;

        //Download any social or channel audio files
        ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle());

        //UI setup thread
        new Thread() {
            @Override
            public void run() {
                //Setup day/night theme selection (based on settings, and time)
                setDayNightTheme();
                //Set highlighting of button bar
                setButtonBarSelection();
                //Animate FAB with pulse
                buttonAddAlarm.setAnimation(AnimationUtils.loadAnimation(context, R.anim.pulse));
                //Set toolbar title
                setupToolbar(toolbarTitle, getString(R.string.my_alarms));

                //Set up adapter for monitoring alarm objects
                mAdapter = new MyAlarmsListAdapter(mAlarms, MyAlarmsFragmentActivity.this);
                //Use a linear layout manager
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mAdapter);

                //Log new crashlytics user
                if(firebaseUser != null) {
                    //Check user sign in method and set Firebase user prop
                    for (UserInfo user: firebaseUser.getProviderData()) {
                        if(user == null) break;
                        if(user.getProviderId() == null) break;
                        if (user.getProviderId().toLowerCase().contains(FA.UserProp.sign_in_method.Google.toLowerCase())) {
                            FA.SetUserProp(FA.UserProp.sign_in_method.class, FA.UserProp.sign_in_method.Google);
                        } else if (user.getProviderId().toLowerCase().contains(FA.UserProp.sign_in_method.Facebook.toLowerCase())) {
                            FA.SetUserProp(FA.UserProp.sign_in_method.class, FA.UserProp.sign_in_method.Facebook);
                        } else if (user.getProviderId().toLowerCase().contains(FA.UserProp.sign_in_method.Email.toLowerCase())) {
                            FA.SetUserProp(FA.UserProp.sign_in_method.class, FA.UserProp.sign_in_method.Email);
                        } else {
                            FA.SetUserProp(FA.UserProp.sign_in_method.class, FA.UserProp.sign_in_method.Unknown);
                        }
                    }

                    // You can call any combination of these three methods
                    Crashlytics.setUserIdentifier(firebaseUser.getUid());
                    Crashlytics.setUserEmail(firebaseUser.getEmail());
                    Crashlytics.setUserName(firebaseUser.getDisplayName());
                    FirebaseNetwork.updateLastSeen();
                }
            }
        }.run();

        //Process intent bundle thread
        new Thread() {
            @Override
            public void run() {
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

                //Clear snooze if action
                if(Constants.ACTION_CANCEL_SNOOZE.equals(getIntent().getAction())) {
                    try {
                        if(extras != null && StrUtils.notNullOrEmpty(extras.getString(Constants.EXTRA_ALARMID))) {
                            deviceAlarmController.snoozeAlarm(extras.getString(Constants.EXTRA_ALARMID), true);
                        }
                    } catch(NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.run();

        //Firebase listview setup thread
        new Thread() {
            @Override
            public void run() {
                DatabaseReference mMyAlarmsReference = FirebaseDatabase.getInstance().getReference()
                        .child("alarms").child(firebaseUser.getUid());
                //Keep local and Firebase alarm dbs synced
                mMyAlarmsReference.keepSynced(true);

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

                            //Check for a valid Firebase entry, if invalid delete entry, else continue
                            if(alarm.getHour() < 0 || alarm.getMinute() < 0 || alarm.getDays().isEmpty() || !StrUtils.notNullOrEmpty(alarm.getUid())) {
                                FirebaseNetwork.removeFirebaseAlarm(postSnapshot.getKey());
                            } else {
                                Boolean successfulProcessing = false;
                                //If alarm from firebase does not exist locally, create it
                                if(StrUtils.notNullOrEmpty(alarm.getUid()) && !deviceAlarmTableManager.isSetInDB(alarm.getUid())) {
                                    //Try to insert alarm into SQL db - if successful, configure new alarm set element and set successfulProcessing flag = true
                                    if(deviceAlarmController.registerAlarmSet(alarm.isEnabled(), alarm.getUid(), alarm.getHour(), alarm.getMinute(),
                                            alarm.getDays(), alarm.isRecurring(), alarmChannelUID, alarm.isAllow_friend_audio_files())) {
                                        configureAlarmElement(alarm);
                                        successfulProcessing = true;
                                    }
                                }
                                //If alarm exists locally, AND in Firebase, just configure alarm UI element
                                else if(StrUtils.notNullOrEmpty(alarm.getUid()) && deviceAlarmTableManager.isSetInDB(alarm.getUid())) {
                                    configureAlarmElement(alarm);
                                    successfulProcessing = true;
                                }

                                //If alarm exists in Firebase and couldn't be created locally, or is corrupt, delete Firebase entry
                                if(!successfulProcessing) {
                                    FirebaseNetwork.removeFirebaseAlarm(postSnapshot.getKey());
                                }
                            }

                            //Notify adapter of new rooster count data to be displayed
                            updateRoosterNotification();
                        }
                        //Sort alarms according to time
                        sortAlarms(mAlarms);

                        //Recreate all enabled alarms as failsafe
                        deviceAlarmController.rebootAlarms();
                        //Case: local has an alarm that firebase doesn't Result: delete local alarm
                        deviceAlarmController.syncAlarmSetGlobal(mAlarms);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                        Toaster.makeToast(MyAlarmsFragmentActivity.this, "Failed to load alarms.",
                                Toast.LENGTH_SHORT).checkTastyToast();
                    }
                }; mMyAlarmsReference.addListenerForSingleValueEvent(alarmsListener);
            }
        }.run();
    }

    @Override
    public void onStart() {
        super.onStart();
        //Display notifications
        updateRoosterNotification();
        updateRequestNotification();
    }

    private void configureAlarmElement(Alarm alarm) {
        //Check SQL db to see if all alarms in set have fired
        alarm.setEnabled(deviceAlarmTableManager.isSetEnabled(alarm.getUid()));
        //Set set enabled flag and don't notify user
        deviceAlarmController.setSetEnabled(alarm.getUid(), alarm.isEnabled(), false);

        //Set alarm element millis to allow sorting
        Long alarmSetPendingMillis = deviceAlarmTableManager.getMillisOfNextPendingAlarmInSet(alarm.getUid());
        if(alarmSetPendingMillis != null) {
            alarm.setMillis(alarmSetPendingMillis);
        }

        //Add alarm to adapter display arraylist and notify adapter of change
        mAlarms.add(alarm);
        mAdapter.notifyItemInserted(mAlarms.size() - 1);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void sortAlarms(ArrayList<Alarm> alarms){
        //Take arraylist and sort by date
        Collections.sort(alarms, new Comparator<Alarm>() {
            @Override
            public int compare(Alarm lhs, Alarm rhs) {
                return lhs.getMillis().compareTo(rhs.getMillis());
            }
        });
        mAdapter.notifyDataSetChanged();
    }

    public void clearRoosterNotificationFlags() {
        for (Alarm alarm: mAlarms) alarm.setUnseen_roosters(0);
        mAdapter.notifyDataSetChanged();
    }

    public void allocateRoosterNotificationFlags(String alarmId, int roosterCount) {
        for (Alarm alarm:
                mAlarms) {
            alarm.setUnseen_roosters(0);
            if(alarm.getUid().equals(alarmId)) {
                alarm.setUnseen_roosters(roosterCount);
            }
        }
        mAdapter.notifyDataSetChanged();
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
        if (id == R.id.action_profile) {
            Intent i = new Intent(this, ProfileActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_signout) {
            signOut();
        }

        return super.onOptionsItemSelected(item);
    }

    public void toggleAlarmSetEnable(final Alarm alarm, final boolean enabled) {
        //Toggle alarm set enabled
        deviceAlarmController.setSetEnabled(alarm.getUid(), enabled, true);
        //Set adapter arraylist item to enabled
        int alarmIndex = mAlarms.indexOf(alarm);
        if(alarmIndex > -1) mAlarms.get(alarmIndex).setEnabled(enabled);
        //Update notification of pending social roosters
        updateRoosterNotification();
    }

    public void deleteAlarm(final String alarmId) {
        try {
            //Remove alarm *set* from local SQL database using retrieved Uid from firebase && Remove alarm from firebase
            deviceAlarmController.deleteAlarmSetGlobal(alarmId);
            //Update notification of pending social roosters
            updateRoosterNotification();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.add_alarm)
    public void onClickAddAlarm() {
        startActivity(new Intent(this, NewAlarmFragmentActivity.class));
    }

    public void editAlarm(String alarmId) {
        Intent intent = new Intent(MyAlarmsFragmentActivity.this, NewAlarmFragmentActivity.class);
        intent.putExtra(Constants.EXTRA_ALARMID, alarmId);
        startActivity(intent);
    }
}
