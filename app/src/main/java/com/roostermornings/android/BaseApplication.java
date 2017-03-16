/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.background.FirebaseListenerService;
import com.roostermornings.android.dagger.DaggerRoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.dagger.RoosterApplicationModule;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.node_api.IHTTPClient;
import com.roostermornings.android.util.FontsOverride;

import io.fabric.sdk.android.Fabric;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class BaseApplication extends android.app.Application {

    private static final String TAG = "BaseApplication";
    RoosterApplicationComponent roosterApplicationComponent;
    public Retrofit mRetrofit;
    public IHTTPClient mAPIService;
    protected FirebaseAuth mAuth;
    protected DatabaseReference mDatabase;

    //Global flag set from FirebaseListenerService to indicate new notification
    private int notificationFlag;
    private BroadcastReceiver receiver;

    private int roosterCount;
    private int friendRequests;

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());

        //Override monospace font with custom font
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/Nunito/Nunito-Bold.ttf");

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

        //Start Firebase listeners applicable to all activities - primarily to update notifications
        startService(new Intent(getApplicationContext(), FirebaseListenerService.class));

        updateRequestNotification();
        updateRoosterNotification();
    }

    private void updateRequestNotification() {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction("rooster.update.REQUEST_NOTIFICATION");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                switch(intent.getAction()){
                    case "rooster.update.REQUEST_NOTIFICATION":
                        setNotificationFlag(getNotificationFlag("friendRequests") + 1, "friendRequests");
                        break;
                    default:
                        break;

                }
            }
        };
        registerReceiver(receiver, firebaseListenerServiceFilter);
    }

    private void updateRoosterNotification() {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction("rooster.update.ROOSTER_NOTIFICATION");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                switch(intent.getAction()){
                    case "rooster.update.ROOSTER_NOTIFICATION":
                        setNotificationFlag(getNotificationFlag("roosterCount") + 1, "roosterCount");
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
        if(flag.contentEquals("roosterCount")){
            notificationFlag = this.roosterCount;
        }
        if(flag.contentEquals("friendRequests")){
            notificationFlag = this.friendRequests;
        }
        return notificationFlag;
    }

    public void setNotificationFlag(int notificationFlag, String flag) {
        if(flag.contentEquals("roosterCount")){
            this.roosterCount = notificationFlag;
        }
        if(flag.contentEquals("friendRequests")){
            this.friendRequests = notificationFlag;
        }
        this.notificationFlag = notificationFlag;
    }
}
