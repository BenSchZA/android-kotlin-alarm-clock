/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.accounts.Account;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
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
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.custom_ui.SquareFrameLayout;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.firebase.FirebaseNetwork;
import com.roostermornings.android.service.MediaService;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sync.DownloadSyncAdapter;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.InternetHelper;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.LifeCycle;
import com.roostermornings.android.util.StrUtils;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import me.grantland.widget.AutofitTextView;

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

    @BindView(R.id.add_alarm_filler)
    SquareFrameLayout addAlarmFiller;
    @BindView(R.id.add_alarm_filler_text)
    AutofitTextView addAlarmFillerText;

    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    private ArrayList<Alarm> mAlarms = new ArrayList<>();
    private ArrayList<Alarm> mTempAlarms = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;

    private BroadcastReceiver receiver;

    Toolbar toolbar;

    @Inject DeviceAlarmController deviceAlarmController;
    @Inject DeviceAlarmTableManager deviceAlarmTableManager;
    @Inject AudioTableManager audioTableManager;
    @Inject BaseApplication baseApplication;
    @Inject @Nullable FirebaseUser firebaseUser;
    @Inject Account mAccount;
    @Inject
    SharedPreferences sharedPreferences;
    @Inject
    LifeCycle lifeCycle;
    @Inject
    JSONPersistence jsonPersistence;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        //Display notifications
        updateRoosterNotification();
        updateRequestNotification();
        //Setup day/night theme selection (based on settings, and time)
        setDayNightTheme();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Persist alarms for seamless loading
        jsonPersistence.setAlarms(mAlarms);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_my_alarms);
        inject(BaseApplication.getRoosterApplicationComponent());

        //Final context to be used in threads
        final Context context = this;

        //Set shared pref to indicate whether mobile number is valid
        FirebaseNetwork.flagValidMobileNumber(this, false);

        //Check if first entry
        lifeCycle.performInception();

        //TODO: remove
        Intent intent = new Intent(this, MediaService.class);
        intent.setAction(MediaService.ACTION_PAUSE);
        ContextCompat.startForegroundService(this, intent);

        //Download any social or channel audio files
        ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle());

        /*
        * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
        * performs a swipe-to-refresh gesture.
        */
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        refreshAlarms();
                        refreshDownloadIndicator();
                    }
                }
        );

        //UI setup thread
        new Thread() {
            @Override
            public void run() {

                //Set highlighting of button bar
                setButtonBarSelection();
                //Animate FAB with pulse
                buttonAddAlarm.setAnimation(AnimationUtils.loadAnimation(context, R.anim.pulse));
                //Set toolbar title
                toolbar = setupToolbar(toolbarTitle, getString(R.string.my_alarms));
                //Set download indicator
                refreshDownloadIndicator();

                //Set up adapter for monitoring alarm objects
                mAdapter = new MyAlarmsListAdapter(mAlarms, MyAlarmsFragmentActivity.this);
                //Use a linear layout manager
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mAdapter);

                //Check for, and load, persisted data
                if(!jsonPersistence.getAlarms().isEmpty()) {
                    mAlarms.addAll(jsonPersistence.getAlarms());
                    mAdapter.notifyDataSetChanged();
                } else if(checkInternetConnection()) {
                    if(!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
                }

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

        //Refresh alarms list from background thread
        refreshAlarms();
    }

    private void refreshDownloadIndicator() {
        if(toolbar != null && !deviceAlarmTableManager.isAlarmTableEmpty()) {

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    refreshDownloadIndicator();
                    //Download any social or channel audio files
                    ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle());
                }
            });

            if(deviceAlarmTableManager.getNextPendingAlarm() == null
                    || deviceAlarmTableManager.isNextPendingAlarmSynced()) {
                toolbar.setNavigationIcon(R.drawable.ic_cloud_done_white_24dp);
            } else if(!InternetHelper.noInternetConnection(this)) {
                animateRefreshDownloadIndicator();
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_cloud_off_white_24dp);
            }

            //Listen for channel download complete notices
            DownloadSyncAdapter.setOnChannelDownloadListener(new DownloadSyncAdapter.OnChannelDownloadListener() {
                @Override
                public void onChannelDownloadStarted(String channelId) {
                    if (!deviceAlarmTableManager.isNextPendingAlarmSynced()) {
                        animateRefreshDownloadIndicator();
                    }
                }

                @Override
                public void onChannelDownloadComplete(boolean valid, String channelId) {
                    if (deviceAlarmTableManager.isNextPendingAlarmSynced()) {
                        toolbar.setNavigationIcon(R.drawable.ic_cloud_done_white_24dp);
                    }
                }
            });
        } else if(toolbar != null) {
            toolbar.setNavigationIcon(null);
        }

        //Download any social or channel audio files
        ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle());
    }

    private void animateRefreshDownloadIndicator() {
        Drawable drawableDownloadIndicator = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_cloud_download_white_24dp, null);
        if(drawableDownloadIndicator != null) {
            toolbar.setNavigationIcon(drawableDownloadIndicator);

            ObjectAnimator animator = ObjectAnimator.
                    ofPropertyValuesHolder(
                            drawableDownloadIndicator,
                            PropertyValuesHolder.ofInt("alpha", 255, 120));
            animator.setDuration(1000);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.setRepeatMode(ObjectAnimator.REVERSE);
            animator.start();
        }
    }

    private void refreshAlarms() {

        //Clear old content
        mTempAlarms.clear();

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
                }

                //Check if persisted data is fresh
                if(!mAlarms.equals(mTempAlarms)) {
                    mAlarms.clear();
                    mAlarms.addAll(mTempAlarms);
                }

                //Sort alarms according to time
                sortAlarms(mAlarms);
                toggleAlarmFiller();

                //Recreate all enabled alarms as failsafe
                deviceAlarmController.rebootAlarms();
                //Case: local has an alarm that firebase doesn't Result: delete local alarm
                deviceAlarmController.syncAlarmSetGlobal(mAlarms);

                //Load content and stop refresh indicator
                mAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
                //Configure rooster notification indicator
                updateRoosterNotification();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toaster.makeToast(MyAlarmsFragmentActivity.this, "Failed to load alarms.",
                        Toast.LENGTH_SHORT).checkTastyToast();
            }
        }; mMyAlarmsReference.addListenerForSingleValueEvent(alarmsListener);
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
        mTempAlarms.add(alarm);
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
        toggleAlarmFiller();
        refreshDownloadIndicator();
    }

    public void deleteAlarm(final String alarmId) {
        try {
            //Remove alarm *set* from local SQL database using retrieved Uid from firebase && Remove alarm from firebase
            deviceAlarmController.deleteAlarmSetGlobal(alarmId);
            //Update notification of pending social roosters
            updateRoosterNotification();
            toggleAlarmFiller();
            refreshDownloadIndicator();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void toggleAlarmFiller() {
        if(mAlarms.isEmpty()) {
            //For pre-Lollipop devices use VectorDrawableCompat to get your vector from resources
            VectorDrawableCompat vectorDrawable = VectorDrawableCompat.create(this.getResources(), R.drawable.ic_alarm_add_white_24px, null);
            addAlarmFiller.setBackground(vectorDrawable);
            addAlarmFiller.setVisibility(View.VISIBLE);
            addAlarmFillerText.setVisibility(View.VISIBLE);
        } else {
            addAlarmFiller.setVisibility(View.GONE);
            addAlarmFillerText.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.add_alarm)
    public void onClickAddAlarm() {
        startActivity(new Intent(this, NewAlarmFragmentActivity.class));
    }

    @OnClick(R.id.add_alarm_filler)
    public void onClickAddAlarmFiller() {
        startActivity(new Intent(this, NewAlarmFragmentActivity.class));
    }

    @OnClick(R.id.add_alarm_filler_text)
    public void onClickAddAlarmFillerText() {
        startActivity(new Intent(this, NewAlarmFragmentActivity.class));
    }

    public void editAlarm(String alarmId) {
        Intent intent = new Intent(MyAlarmsFragmentActivity.this, NewAlarmFragmentActivity.class);
        intent.putExtra(Constants.EXTRA_ALARMID, alarmId);
        startActivity(intent);
    }
}
