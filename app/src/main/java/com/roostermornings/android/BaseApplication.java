/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.stetho.Stetho;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.apis.GoogleIHTTPClient;
import com.roostermornings.android.dagger.DaggerRoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationModule;
import com.roostermornings.android.domain.database.User;
import com.roostermornings.android.apis.NodeIHTTPClient;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.receiver.NetworkChangeReceiver;
import com.roostermornings.android.util.AppTesting;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.Toaster;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class BaseApplication extends android.app.Application {

    private static final String TAG = "BaseApplication";
    private static RoosterApplicationComponent roosterApplicationComponent;
    private PlaceDetectionClient mPlaceDetectionClient;
    public Retrofit mRetrofitNode;
    public NodeIHTTPClient mAPIServiceNode;
    public Retrofit mRetrofitGoogle;
    public GoogleIHTTPClient mAPIServiceGoogle;
    protected FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // Global flag set from FirebaseListenerService to indicate new notification
    private static int notificationFlag;
    private BroadcastReceiver receiver;

    private static int roosterCount = 0;
    private static int friendRequests = 0;

    public static User mCurrentUser;

    @Inject BackgroundTaskReceiver backgroundTaskReceiver;
    @Inject SharedPreferences sharedPreferences;
    @Inject DatabaseReference mDatabase;
    public static FirebaseAnalytics firebaseAnalytics;

    public static Boolean isAppForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable StrictMode, with logging of all errors
        // .penaltyDialog()
        // https://code.tutsplus.com/tutorials/android-best-practices-strictmode--mobile-7581
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                .penaltyLog()
                .build());

        // Set database persistence to keep offline alarm edits synced
        // Calls to setPersistenceEnabled() must be made before any other usage of FirebaseDatabase instance
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Get static FBAnalytics instance
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Boolean debuggable = AppTesting.Companion.isDebuggable(this);
        // Activate Crashlytics instance
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG || debuggable).build();
        if("BetaFlavour".equals(BuildConfig.FLAVOR)) {
            Fabric.with(new Fabric.Builder(this).kits(new Crashlytics.Builder().core(core).build()).appIdentifier("com.roostermornings.android.beta").build());
        } else {
            Fabric.with(this, new Crashlytics.Builder().core(core).build(), new Crashlytics());
        }

        // If in debug mode...
        if(BuildConfig.DEBUG || debuggable) {
            firebaseAnalytics.setAnalyticsCollectionEnabled(false);
            // Stetho: http://facebook.github.io/stetho/ - debug bridge for Android (activityContentView SQL etc.)
            // Go to chrome://inspect/ in Chrome to inspect
            Stetho.initializeWithDefaults(this);
        }

        // Initialize Realm database
        Realm.init(this);

        RealmConfiguration alarmFailureLogRealmConfig = new RealmConfiguration.Builder()
                .name("alarm_failure_log.realm")
                .schemaVersion(2)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(alarmFailureLogRealmConfig);

        /* Component implementations are primarily instantiated via a generated builder.
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

        // Register receiver to listen for network changes
        NetworkChangeReceiver.Companion.registerReceiverSelf(this);

        // Activate Facebook app connection
        AppEventsLogger.activateApp(this, getResources().getString(R.string.facebook_app_id));

        // Create Retrofit API class for managing Node API
        mRetrofitNode = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.node_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mAPIServiceNode = mRetrofitNode.create(NodeIHTTPClient.class);

        // Create Retrofit API class for managing Google API
        mRetrofitGoogle = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.google_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mAPIServiceGoogle = mRetrofitGoogle.create(GoogleIHTTPClient.class);

        updateNotification();

        // Add Firebase auth state listener
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                    // Retrieve static User for current user
                    retrieveMyUserDetails();
                    startBackgroundServices();
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
                Toaster.makeToast(getApplicationContext(), "Failed to load user.", Toast.LENGTH_SHORT).checkTastyToast();
            }
        };

        if (getFirebaseUser() != null) {
            DatabaseReference thisUserReference = mDatabase
                    .child("users").child(getFirebaseUser().getUid());
            thisUserReference.keepSynced(true);
            thisUserReference.addValueEventListener(userListener);
        }
    }

    public static DatabaseReference getFbDbRef() {
        return FirebaseDatabase.getInstance().getReference();
    }

    private FirebaseUser getFirebaseUser() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }

    private void startBackgroundServices() {
        backgroundTaskReceiver.scheduleBackgroundDailyTask(getApplicationContext(), true);
    }

    private void updateNotification() {
        // Flag check for UI changes on load, broadcastreceiver for changes while activity running
        // Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction(Constants.ACTION_REQUESTNOTIFICATION);
        firebaseListenerServiceFilter.addAction(Constants.ACTION_ROOSTERNOTIFICATION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Do something based on the intent's action
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

    public static RoosterApplicationComponent getRoosterApplicationComponent() {
        return roosterApplicationComponent;
    }

    public NodeIHTTPClient getNodeAPIService() {
        return mAPIServiceNode;
    }

    public GoogleIHTTPClient getGoogleAPIService() {
        return mAPIServiceGoogle;
    }

    public static int getNotificationFlag(String flag) {
        if(flag.contentEquals(Constants.FLAG_ROOSTERCOUNT)){
            notificationFlag = roosterCount;
        }
        if(flag.contentEquals(Constants.FLAG_FRIENDREQUESTS)){
            notificationFlag = friendRequests;
        }
        return notificationFlag;
    }

    public static void setNotificationFlag(int value, String flag) {
        if(flag.contentEquals(Constants.FLAG_ROOSTERCOUNT)){
            roosterCount = value;
        }
        if(flag.contentEquals(Constants.FLAG_FRIENDREQUESTS)){
            friendRequests = value;
        }
    }
}
