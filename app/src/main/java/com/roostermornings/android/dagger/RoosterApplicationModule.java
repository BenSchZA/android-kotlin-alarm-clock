/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.dagger;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.MyContactsController;

import static com.roostermornings.android.sync.DownloadSyncAdapter.CreateSyncAccount;
import static com.roostermornings.android.util.Constants.AUTHORITY;

/**
 * Created by Abdul on 6/14/2016.
 * Dagger injection module, providing instances of the application classes and satisfying their dependencies
 */
@Module
public class RoosterApplicationModule {

    BaseApplication baseApplication;

    //pass the base application into the constructor for context
    public RoosterApplicationModule(BaseApplication baseApplication) {
        this.baseApplication = baseApplication;
    }

    //For cases where @Inject is insufficient or awkward,
    //an @Provides-annotated method satifies a dependency.
    //The method's return type defines which dependency it satisfies.

    //All @Provides methods must belong to a module.
    //These are just classes that have an @Module annotation.

    //By convention, @Provides methods are named with a 'provide' prefix
    //and module classes are named with a 'Module' suffix.

    @Provides
    @Singleton
    Context provideContext() {
        return baseApplication.getBaseContext();
    }

    @Provides
    @Singleton
    BaseApplication providesBaseApplication() {
        return baseApplication;
    }

    @Provides
    Account provideSyncAdapterAccount(Context context) {
        //Create sync account
        Account mAccount = CreateSyncAccount(context);
        ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);
        ContentResolver.addPeriodicSync(mAccount, AUTHORITY, new Bundle(), 900);
        return mAccount;
    }

    @Provides
    @Singleton
    FirebaseAnalytics provideFirebaseAnalytics() {
        return FirebaseAnalytics.getInstance(baseApplication);
    }

    @Provides
    @Singleton
    FirebaseUser provideFirebaseUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }

    @Provides
    @Singleton
    DatabaseReference provideFirebaseDatabaseReference() {
        return FirebaseDatabase.getInstance().getReference();
    }

    @Provides
    @Singleton
    StorageReference provideFirebaseStorageReference() {
        return FirebaseStorage.getInstance().getReference();
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences() {
        return baseApplication.getSharedPreferences(baseApplication.getString(R.string.preferences_key),
                Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @Named("default")
    SharedPreferences provideDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(baseApplication);
    }

    @Provides
    @Singleton
    JSONPersistence providesJSONPersistence(BaseApplication baseApplication) {
        return new JSONPersistence(baseApplication);
    }

    @Provides
    @Singleton
    MyContactsController providesMyContactsController(BaseApplication baseApplication) {
        return new MyContactsController(baseApplication);
    }

    @Provides
    @Singleton
    DeviceAlarmTableManager provideDeviceAlarmTableManager(BaseApplication baseApplication) {
        return new DeviceAlarmTableManager(baseApplication);
    }

    @Provides
    @Singleton
    DeviceAlarmController provideDeviceAlarmController(BaseApplication baseApplication) {
        return new DeviceAlarmController(baseApplication);
    }

    @Provides
    @Singleton
    AudioTableManager provideAudioTableManager(BaseApplication baseApplication) {
        return new AudioTableManager(baseApplication);
    }

    @Provides
    @Singleton
    BackgroundTaskReceiver provideBackgroundTaskReceiver() {
        return new BackgroundTaskReceiver();
    }
}


