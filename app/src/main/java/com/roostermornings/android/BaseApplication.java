/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.icu.util.Calendar;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.stetho.Stetho;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roostermornings.android.activity.SplashActivity;
import com.roostermornings.android.dagger.DaggerRoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationModule;
import com.roostermornings.android.node_api.IHTTPClient;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.service.FirebaseListenerService;
import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.sqldata.DeviceAlarmTableHelper;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.FontsOverride;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

import static com.roostermornings.android.util.Constants.FLAG_FRIENDREQUESTS;

public class BaseApplication extends android.app.Application {

    private static final String TAG = "BaseApplication";
    RoosterApplicationComponent roosterApplicationComponent;
    public Retrofit mRetrofit;
    public IHTTPClient mAPIService;
    protected FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    protected DatabaseReference mDatabase;

    public static Context AppContext;

    //Global flag set from FirebaseListenerService to indicate new notification
    private static int notificationFlag;
    private BroadcastReceiver receiver;

    private static int roosterCount;
    private static int friendRequests;

    @Inject
    public SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        //Context to be used in all fragments that could be detached
        AppContext = getApplicationContext();

        sharedPreferences = getSharedPreferences(this.getString(R.string.preferences_key), Context.MODE_PRIVATE);

        Fabric.with(this, new Crashlytics());

        AppEventsLogger.activateApp(this, Constants.FACEBOOK_APP_ID);

        //Override monospace font with custom font
        FontsOverride.setDefaultFont(this, "MONOSPACE", Constants.APP_FONT);

        if (BuildConfig.DEBUG) {
            //Stetho: http://facebook.github.io/stetho/ - debug bridge for Android (view SQL etc.)
            //Go to chrome://inspect/ in Chrome to inspect
            Stetho.initializeWithDefaults(this);
        }

        /*Component implementations are primarily instantiated via a generated builder.
        An instance of the builder is obtained using the builder() method on the component implementation.
        If a nested @Component.Builder type exists in the component, the builder() method will
        return a generated implementation of that type. If no nested @Component.Builder exists,
        the returned builder has a method to set each of the modules() and component dependencies()
        named with the lower camel case version of the module or dependency type.
         */
        roosterApplicationComponent = DaggerRoosterApplicationComponent
                .builder()
                .roosterApplicationModule(new RoosterApplicationModule(this))
                .build();

        //Create Retrofit API class for managing Node API
        mRetrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.node_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mAPIService = mRetrofit.create(IHTTPClient.class);

        //set database persistence to keep offline alarm edits synced
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        updateNotification();

        //TODO: remove
        if (BuildConfig.DEBUG) {

//            AudioTableHelper dbAudioHelper = new AudioTableHelper(this);
//            SQLiteDatabase dbAudio = dbAudioHelper.getWritableDatabase();
//            DeviceAlarmTableHelper dbAlarmHelper = new DeviceAlarmTableHelper(this);
//            SQLiteDatabase dbAlarm = dbAlarmHelper.getWritableDatabase();
//
//            AudioTableHelper audioTableHelper = new AudioTableHelper(this);
//            DeviceAlarmTableHelper deviceAlarmTableHelper = new DeviceAlarmTableHelper(this);
//            audioTableHelper.onUpgrade(dbAudio, 1, 2);
//            deviceAlarmTableHelper.onUpgrade(dbAlarm, 1, 2);
//
//            dbAudio.close();
//            dbAlarm.close();
        }

        //Set default application settings preferences - don't overwrite existing if false
        setPreferenceManagerDefaultSettings(false);

        // Add Firebase auth state listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                    startBackgroundServices();
                    // Start Firebase listeners applicable to all activities - primarily to update notifications
                    if(!isServiceRunning(FirebaseListenerService.class))
                        startService(new Intent(getApplicationContext(), FirebaseListenerService.class));
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    signOut();
                }
            }
        };

        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);
    }

    public void signOut() {
        try {
            //Ensure no audio remaining from old user
            AudioTableManager audioTableManager = new AudioTableManager(this);
            audioTableManager.removeAllSocialAudioItems();
            audioTableManager.removeAllChannelAudioFiles();
            //Ensure no alarms left from old user
            DeviceAlarmController deviceAlarmController = new DeviceAlarmController(this);
            deviceAlarmController.deleteAllLocalAlarms();
            //Cancel background task intents
            BackgroundTaskReceiver backgroundTaskReceiver = new BackgroundTaskReceiver();
            backgroundTaskReceiver.scheduleBackgroundCacheFirebaseData(this, false);
            backgroundTaskReceiver.scheduleBackgroundDailyTask(this, false);
            backgroundTaskReceiver.scheduleBackgroundUpdateNotificationsTask(this, false);
            //Set default application settings preferences - don't overwrite existing if false
            setPreferenceManagerDefaultSettings(true);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        //Go to splash activity and onboarding
        Intent intent = new Intent(BaseApplication.this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setPreferenceManagerDefaultSettings(Boolean overwrite) {
        // ensure user settings are set to default once when new user
        // As long as you set the third argument to false, you can safely call this method
        // every time your activity starts without overriding the user's saved preferences
        // by resetting them to the defaults. However, if you set it to true, you will
        // override any previous values with the defaults.

        if(overwrite) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("pref_key_user_settings").apply();
        }
        PreferenceManager.setDefaultValues(this, R.xml.application_user_settings, overwrite);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startBackgroundServices() {
        BackgroundTaskReceiver backgroundTaskReceiver = new BackgroundTaskReceiver();
        backgroundTaskReceiver.scheduleBackgroundCacheFirebaseData(getApplicationContext(), true);
        backgroundTaskReceiver.scheduleBackgroundDailyTask(getApplicationContext(), true);
        backgroundTaskReceiver.scheduleBackgroundUpdateNotificationsTask(getApplicationContext(), true);
    }

    private void updateNotification() {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction(Constants.ACTION_REQUESTNOTIFICATION);
        firebaseListenerServiceFilter.addAction(Constants.ACTION_ROOSTERNOTIFICATION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                switch(intent.getAction()){
                    case Constants.ACTION_REQUESTNOTIFICATION:
                        setNotificationFlag(getNotificationFlag(Constants.FLAG_FRIENDREQUESTS) + 1, Constants.FLAG_FRIENDREQUESTS);
                        break;
                    case Constants.ACTION_ROOSTERNOTIFICATION:
                        setNotificationFlag(intent.getIntExtra(Constants.EXTRA_SOCIAL_ROOSTERS, 0), Constants.FLAG_ROOSTERCOUNT);
                        break;
                    default:
                        break;

                }
            }
        };
        registerReceiver(receiver, firebaseListenerServiceFilter);
    }

    public RoosterApplicationComponent getRoosterApplicationComponent() {
        return roosterApplicationComponent;
    }

    public IHTTPClient getAPIService() {
        return mAPIService;
    }

    public int getNotificationFlag(String flag) {
        if(flag.contentEquals(Constants.FLAG_ROOSTERCOUNT)){
            notificationFlag = roosterCount;
        }
        if(flag.contentEquals(Constants.FLAG_FRIENDREQUESTS)){
            notificationFlag = friendRequests;
        }
        return notificationFlag;
    }

    public void setNotificationFlag(int value, String flag) {
        if(flag.contentEquals(Constants.FLAG_ROOSTERCOUNT)){
            roosterCount = value;
        }
        if(flag.contentEquals(Constants.FLAG_FRIENDREQUESTS)){
            friendRequests = value;
        }
    }
}
