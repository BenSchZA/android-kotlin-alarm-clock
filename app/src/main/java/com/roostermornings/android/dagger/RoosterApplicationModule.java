/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.dagger;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.sqlutil.AudioTableController;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;

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
    AudioTableController provideAudioTableController(BaseApplication baseApplication) {
        return new AudioTableController(baseApplication);
    }

    @Provides
    @Singleton
    BackgroundTaskReceiver provideBackgroundTaskReceiver() {
        return new BackgroundTaskReceiver();
    }
}


