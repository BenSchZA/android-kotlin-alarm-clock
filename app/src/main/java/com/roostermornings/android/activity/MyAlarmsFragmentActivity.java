/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.accounts.Account;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.github.amlcurran.showcaseview.MaterialShowcaseDrawer;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.adapter_data.RoosterAlarmManager;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.custom_ui.SquareFrameLayout;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.firebase.FirebaseNetwork;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sync.DownloadSyncAdapter;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.FirstMileManager;
import com.roostermornings.android.util.InternetHelper;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.LifeCycle;
import com.roostermornings.android.util.StrUtils;
import com.roostermornings.android.widgets.AlarmToggleWidget;
import com.roostermornings.android.widgets.AlarmToggleWidgetDataProvider;

import org.jetbrains.annotations.NotNull;

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
    private RecyclerView.Adapter mAdapter;

    private BroadcastReceiver receiver;

    Toolbar toolbar;

    @Inject DeviceAlarmController deviceAlarmController;
    @Inject DeviceAlarmTableManager deviceAlarmTableManager;
    @Inject AudioTableManager audioTableManager;
    @Inject
    RoosterAlarmManager roosterAlarmManager;
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

        //Update app widget
        AlarmToggleWidget.Companion.sendUpdateBroadcast(this);
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

        //FirstMileManager firstMileManager = new FirstMileManager();
        //firstMileManager.createShowcase(this, new ViewTarget(buttonAddAlarm.getId(), this), 1);

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
                        roosterAlarmManager.fetchAlarms(mAlarms);
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

        RoosterAlarmManager.Companion.setOnFlagAlarmManagerDataListener(new RoosterAlarmManager.Companion.OnFlagAlarmManagerDataListener() {
            @Override
            public void onAlarmDataChanged(@NotNull ArrayList<Alarm> freshAlarms) {
                //Check if persisted data is fresh
                if (mAlarms != freshAlarms) {
                    mAlarms.clear();
                    mAlarms.addAll(freshAlarms);
                }
            }

            @Override
            public void onSyncFinished() {
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
        });

        //Refresh alarms list from background thread
        roosterAlarmManager.fetchAlarms(mAlarms);
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
