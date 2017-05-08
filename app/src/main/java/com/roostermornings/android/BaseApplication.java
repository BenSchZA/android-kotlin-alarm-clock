/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android;

import android.app.Activity;
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
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.stetho.Stetho;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.activity.SplashActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.activity.base.BaseActivity_MembersInjector;
import com.roostermornings.android.dagger.DaggerRoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationModule;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.node_api.IHTTPClient;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.service.FirebaseListenerService;
import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.sqldata.DeviceAlarmTableHelper;
import com.roostermornings.android.sqlutil.AudioTableController;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
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

    //Global flag set from FirebaseListenerService to indicate new notification
    private static int notificationFlag;
    private BroadcastReceiver receiver;

    private static int roosterCount = 0;
    private static int friendRequests = 0;

    public static User mCurrentUser;
    public static DatabaseReference mDatabase;

    @Inject BackgroundTaskReceiver backgroundTaskReceiver;
    @Inject SharedPreferences sharedPreferences;
    public static Context AppContext;

    @Override
    public void onCreate() {
        super.onCreate();

        AppContext = getBaseContext();

        //Set database persistence to keep offline alarm edits synced
        //Calls to setPersistenceEnabled() must be made before any other usage of FirebaseDatabase instance
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        //Database reference to be used everywhere
        mDatabase = FirebaseDatabase.getInstance().getReference();

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

        roosterApplicationComponent.inject(this);

        //Create Retrofit API class for managing Node API
        mRetrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.node_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mAPIService = mRetrofit.create(IHTTPClient.class);

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

        // Add Firebase auth state listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                    //retrieve static User for current user
                    retrieveMyUserDetails();

                    startBackgroundServices();
                    // Start Firebase listeners applicable to all activities - primarily to update notifications
                    if(!isServiceRunning(FirebaseListenerService.class))
                        startService(new Intent(getApplicationContext(), FirebaseListenerService.class));
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);
    }

    private void retrieveMyUserDetails() {
        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCurrentUser = dataSnapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Failed to load user.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        if (getFirebaseUser() != null) {
            DatabaseReference thisUserReference = mDatabase
                    .child("users").child(getFirebaseUser().getUid());
            thisUserReference.keepSynced(true);
            thisUserReference.addValueEventListener(userListener);
        }
    }

    private FirebaseUser getFirebaseUser() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
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
