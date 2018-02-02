/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.StrictMode
import android.util.Log
import android.widget.Toast

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.facebook.appevents.AppEventsLogger
import com.facebook.stetho.Stetho
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.roostermornings.android.apis.GoogleIHTTPClient
import com.roostermornings.android.dagger.DaggerRoosterApplicationComponent
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.dagger.RoosterApplicationModule
import com.roostermornings.android.domain.database.User
import com.roostermornings.android.apis.NodeIHTTPClient
import com.roostermornings.android.keys.Action
import com.roostermornings.android.keys.Extra
import com.roostermornings.android.keys.Flag
import com.roostermornings.android.receiver.BackgroundTaskReceiver
import com.roostermornings.android.receiver.NetworkChangeReceiver
import com.roostermornings.android.util.AppTesting
import com.roostermornings.android.util.Toaster

import javax.inject.Inject

import io.fabric.sdk.android.Fabric
import io.realm.Realm
import io.realm.RealmConfiguration
import retrofit.GsonConverterFactory
import retrofit.Retrofit

class BaseApplication : android.app.Application() {

    private val mRetrofitNode: Retrofit by lazy {
        // Create Retrofit API class for managing Node API
        Retrofit.Builder()
                .baseUrl(resources.getString(R.string.node_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
    val mNodeAPIService: NodeIHTTPClient by lazy {
        mRetrofitNode.create(NodeIHTTPClient::class.java)
    }
    private val mRetrofitGoogle: Retrofit by lazy {
        // Create Retrofit API class for managing Google API
        Retrofit.Builder()
                .baseUrl(resources.getString(R.string.google_api_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
    val mGoogleAPIService: GoogleIHTTPClient by lazy {
        mRetrofitGoogle.create(GoogleIHTTPClient::class.java)
    }

    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var receiver: BroadcastReceiver? = null

    @Inject lateinit var backgroundTaskReceiver: BackgroundTaskReceiver
    @Inject lateinit var sharedPreferences: SharedPreferences
    @Inject lateinit var mDatabase: DatabaseReference

    private val firebaseUser: FirebaseUser?
        get() {
            if (mAuth == null) mAuth = FirebaseAuth.getInstance()
            return mAuth?.currentUser
        }

    init {
        // Enable StrictMode, with logging of all errors
        // .penaltyDialog()
        // https://code.tutsplus.com/tutorials/android-best-practices-strictmode--mobile-7581
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll()
                .penaltyLog()
                .build())

        /* Component implementations are primarily instantiated via a generated builder.
        An instance of the builder is obtained using the builder() method on the component implementation.
        If a nested @Component.Builder type exists in the component, the builder() method will
        return a generated implementation of that type. If no nested @Component.Builder exists,
        the returned builder has a method to set each of the modules() and component dependencies()
        named with the lower camel case version of the module or dependency type.
         */
        roosterApplicationComponent = DaggerRoosterApplicationComponent
                .builder()
                .roosterApplicationModule(RoosterApplicationModule(this))
                .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Set database persistence to keep offline alarm edits synced
        // Calls to setPersistenceEnabled() must be made before any other usage of FirebaseDatabase instance
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val debuggable = AppTesting.isDebuggable(this)
        // Activate Crashlytics instance
        val core = CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG || debuggable).build()
        if ("BetaFlavour" == BuildConfig.FLAVOR) {
            Fabric.with(Fabric.Builder(this).kits(Crashlytics.Builder().core(core).build()).appIdentifier("com.roostermornings.android.beta").build())
        } else {
            Fabric.with(this, Crashlytics.Builder().core(core).build(), Crashlytics())
        }

        // If in debug mode...
        if (BuildConfig.DEBUG || debuggable) {
            firebaseAnalytics.setAnalyticsCollectionEnabled(false)
            // Stetho: http://facebook.github.io/stetho/ - debug bridge for Android (activityContentView SQL etc.)
            // Go to chrome://inspect/ in Chrome to inspect
            Stetho.initializeWithDefaults(this)
        }

        // Initialize Realm database
        Realm.init(this)

        val alarmFailureLogRealmConfig = RealmConfiguration.Builder()
                .name("alarm_failure_log.realm")
                .schemaVersion(2)
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(alarmFailureLogRealmConfig)

        roosterApplicationComponent.inject(this)

        // Register receiver to listen for network changes
        NetworkChangeReceiver.registerReceiverSelf(this)

        // Activate Facebook app connection
        AppEventsLogger.activateApp(this, resources.getString(R.string.facebook_app_id))

        updateNotifications()

        // Add Firebase auth state listener
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let {
                // User is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in:" + it.uid)

                // Retrieve static User for current user
                retrieveMyUserDetails()
                startBackgroundServices()
            } ?: // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out")
        }

        if (mAuth == null) mAuth = FirebaseAuth.getInstance()
        mAuthListener?.let { mAuth?.addAuthStateListener(it) }
    }

    private fun retrieveMyUserDetails() {
        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                mCurrentUser = dataSnapshot.getValue(User::class.java)?: User()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                Toaster.makeToast(applicationContext, "Failed to load user.", Toast.LENGTH_SHORT).checkTastyToast()
            }
        }

        if (firebaseUser != null) {
            val thisUserReference = mDatabase
                    .child("users").child(firebaseUser?.uid)
            thisUserReference.keepSynced(true)
            thisUserReference.addValueEventListener(userListener)
        }
    }

    private fun startBackgroundServices() {
        backgroundTaskReceiver.scheduleBackgroundDailyTask(applicationContext, true)
    }

    private fun updateNotifications() {
        // Flag check for UI changes on load, broadcast receiver for changes while activity running
        // Broadcast receiver filter to receive UI updates
        val firebaseListenerServiceFilter = IntentFilter()
        firebaseListenerServiceFilter.addAction(Action.REQUEST_NOTIFICATION.name)
        firebaseListenerServiceFilter.addAction(Action.ROOSTER_NOTIFICATION.name)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Do something based on the intent's action
                when (intent.action) {
                    Action.REQUEST_NOTIFICATION.name -> setNotificationFlag(
                            getNotificationFlag(Flag.FRIEND_REQUESTS.name) + 1,
                            Flag.FRIEND_REQUESTS.name)
                    Action.ROOSTER_NOTIFICATION.name-> setNotificationFlag(
                            intent.getIntExtra(Extra.SOCIAL_ROOSTERS.name,
                                    0),
                            Flag.ROOSTER_COUNT.name)
                    else -> {}
                }
            }
        }
        registerReceiver(receiver, firebaseListenerServiceFilter)
    }

    companion object {

        private val TAG = "BaseApplication"
        lateinit var roosterApplicationComponent: RoosterApplicationComponent
            private set

        lateinit var firebaseAnalytics: FirebaseAnalytics

        // Global flag set from FirebaseListenerService to indicate new notification
        private var notificationFlag: Int = 0

        private var roosterCount = 0
        private var friendRequests = 0

        var mCurrentUser: User = User()
        var isAppForeground: Boolean = false

        val fbDbRef: DatabaseReference
            get() = FirebaseDatabase.getInstance().reference

        fun getNotificationFlag(flag: String): Int {
            when {
                flag.contentEquals(Flag.ROOSTER_COUNT.name) -> notificationFlag = roosterCount
                flag.contentEquals(Flag.FRIEND_REQUESTS.name) -> notificationFlag = friendRequests
            }
            return notificationFlag
        }

        fun setNotificationFlag(value: Int, flag: String) {
            when {
                flag.contentEquals(Flag.ROOSTER_COUNT.name) -> roosterCount = value
                flag.contentEquals(Flag.FRIEND_REQUESTS.name) -> friendRequests = value
            }
        }
    }
}
