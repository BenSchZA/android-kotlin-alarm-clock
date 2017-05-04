/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.dagger;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

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
    public Context provideContext() {
        return baseApplication;
    }

    @Provides
    @Singleton
    public BaseApplication providesBaseApplication() {
        return baseApplication;
    }

    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences() {
        return baseApplication.getSharedPreferences(baseApplication.getString(R.string.preferences_key),
                Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    public DeviceAlarmTableManager provideDeviceAlarmTableManager(BaseApplication baseApplication) {
        return new DeviceAlarmTableManager(baseApplication);
    }

    @Provides
    @Singleton
    public DeviceAlarmController provideDeviceAlarmController(BaseApplication baseApplication) {
        return new DeviceAlarmController(baseApplication);
    }

    @Provides
    @Singleton
    public AudioTableManager provideAudioTableManager(BaseApplication baseApplication) {
        return new AudioTableManager(baseApplication);
    }

    @Provides
    @Singleton
    public AudioTableController provideAudioTableController(BaseApplication baseApplication) {
        return new AudioTableController(baseApplication);
    }

    @Provides
    @Singleton
    public BackgroundTaskReceiver provideBackgroundTaskReceiver() {
        return new BackgroundTaskReceiver();
    }
}


