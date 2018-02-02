/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.dagger

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager

import javax.inject.Named
import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import io.realm.Realm

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.adapter_data.RoosterAlarmManager
import com.roostermornings.android.adapter_data.ChannelManager
import com.roostermornings.android.firebase.AuthManager
import com.roostermornings.android.realm.RealmAlarmFailureLog
import com.roostermornings.android.realm.RealmScheduledSnackbar
import com.roostermornings.android.receiver.BackgroundTaskReceiver
import com.roostermornings.android.sqlutil.AudioTableManager
import com.roostermornings.android.sqlutil.DeviceAlarmController
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager
import com.roostermornings.android.geolocation.GeoHashUtils
import com.roostermornings.android.util.JSONPersistence
import com.roostermornings.android.keys.PrefsKey
import com.squareup.otto.Bus

import com.roostermornings.android.sync.DownloadSyncAdapter.CreateSyncAccount
import com.roostermornings.android.util.*
import com.roostermornings.android.util.Constants.AUTHORITY

/**
 * Created by Abdul on 6/14/2016.
 * Dagger injection module, providing instances of the application classes and satisfying their dependencies
 */
@Module
class RoosterApplicationModule//pass the base application into the constructor for context
(internal var baseApplication: BaseApplication) {

    //For cases where @Inject is insufficient or awkward,
    //an @Provides-annotated method satifies a dependency.
    //The method's return type defines which dependency it satisfies.

    //All @Provides methods must belong to a module.
    //These are just classes that have an @Module annotation.

    //By convention, @Provides methods are named with a 'provide' prefix
    //and module classes are named with a 'Module' suffix.

    @Provides
    @Singleton
    internal fun provideContext(): Context {
        return baseApplication.baseContext
    }

    @Provides
    @Singleton
    internal fun providesBaseApplication(): BaseApplication {
        return baseApplication
    }

    @Provides
    @Singleton
    internal fun provideSyncAdapterAccount(context: Context): Account {
        //Create sync account
        val mAccount = CreateSyncAccount(context)
        ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1)
        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true)
        ContentResolver.addPeriodicSync(mAccount, AUTHORITY, Bundle(), 3600)
        return mAccount
    }

    @Provides
    @Singleton
    internal fun provideFirebaseAnalytics(): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(baseApplication)
    }

    @Provides
    internal fun provideFirebaseUser(): FirebaseUser? {
        val mAuth = FirebaseAuth.getInstance()
        return mAuth.currentUser
    }

    @Provides
    @Singleton
    internal fun provideAuthManager(): AuthManager {
        return AuthManager(baseApplication)
    }

    @Provides
    internal fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    internal fun provideFirebaseDatabaseReference(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    @Provides
    @Singleton
    internal fun provideFirebaseStorageReference(): StorageReference {
        return FirebaseStorage.getInstance().reference
    }

    @Provides
    @Singleton
    internal fun provideSharedPreferences(): SharedPreferences {
        return baseApplication.getSharedPreferences(PrefsKey.SHARED_PREFS_KEY.name,
                Context.MODE_PRIVATE)
    }

    @Singleton
    @Provides
    @Named("default")
    internal fun provideDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    internal fun providesJSONPersistence(): JSONPersistence {
        return JSONPersistence()
    }

    @Provides
    @Singleton
    internal fun providesMyContactsController(baseApplication: BaseApplication): MyContactsController {
        return MyContactsController(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideDeviceAlarmTableManager(baseApplication: BaseApplication): DeviceAlarmTableManager {
        return DeviceAlarmTableManager(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideDeviceAlarmController(baseApplication: BaseApplication): DeviceAlarmController {
        return DeviceAlarmController(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideAudioTableManager(baseApplication: BaseApplication): AudioTableManager {
        return AudioTableManager(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideGeoHashUtils(baseApplication: BaseApplication): GeoHashUtils {
        return GeoHashUtils(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideConnectivityUtils(baseApplication: BaseApplication): ConnectivityUtils {
        return ConnectivityUtils(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideLifeCycle(baseApplication: BaseApplication): LifeCycle {
        return LifeCycle(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideBackgroundTaskReceiver(): BackgroundTaskReceiver {
        return BackgroundTaskReceiver()
    }

    @Provides
    @Singleton
    internal fun provideChannelManager(baseApplication: BaseApplication): ChannelManager {
        return ChannelManager(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideRoosterAlarmManager(baseApplication: BaseApplication): RoosterAlarmManager {
        return RoosterAlarmManager(baseApplication)
    }

    @Provides
    @Singleton
    internal fun provideOttoBus(): Bus {
        return Bus()
    }

    @Provides
    internal fun provideDefaultRealm(): Realm {
        return Realm.getDefaultInstance()
    }

    @Provides
    internal fun provideRealmManagerAlarmFailureLog(baseApplication: BaseApplication): RealmAlarmFailureLog {
        return RealmAlarmFailureLog(baseApplication)
    }

    @Provides
    internal fun provideRealmManagerScheduledSnackbar(baseApplication: BaseApplication): RealmScheduledSnackbar {
        return RealmScheduledSnackbar()
    }
}


