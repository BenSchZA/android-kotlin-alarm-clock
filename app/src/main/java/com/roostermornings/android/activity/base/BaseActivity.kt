/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity.base

import android.accounts.Account
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.DiscoverFragmentActivity
import com.roostermornings.android.activity.FriendsFragmentActivity
import com.roostermornings.android.activity.MessageStatusFragmentActivity
import com.roostermornings.android.activity.MyAlarmsFragmentActivity
import com.roostermornings.android.activity.NewAudioRecordActivity
import com.roostermornings.android.activity.SplashActivity
import com.roostermornings.android.apis.GoogleIHTTPClient
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.database.Alarm
import com.roostermornings.android.domain.database.User
import com.roostermornings.android.firebase.AuthManager
import com.roostermornings.android.firebase.UserMetrics
import com.roostermornings.android.fragment.base.BaseFragment
import com.roostermornings.android.apis.NodeIHTTPClient
import com.roostermornings.android.receiver.BackgroundTaskReceiver
import com.roostermornings.android.service.FirebaseListenerService
import com.roostermornings.android.sqlutil.AudioTableManager
import com.roostermornings.android.sqlutil.DeviceAlarmController
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager

import java.util.Calendar

import javax.inject.Inject
import javax.inject.Named

import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Optional
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseUser
import com.roostermornings.android.firebase.FA
import com.roostermornings.android.firebase.FirebaseNetwork
import com.roostermornings.android.realm.RealmAlarmFailureLog
import com.roostermornings.android.service.ForegroundService
import com.roostermornings.android.util.*
import com.roostermornings.android.util.Constants.APP_BROUGHT_FOREGROUND

import com.roostermornings.android.util.Constants.AUTHORITY

abstract class BaseActivity : AppCompatActivity(), Validator.ValidationListener, BaseFragment.BaseActivityListener {

    var mAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private var roosterNotificationReceiver: BroadcastReceiver? = null
    private var requestNotificationReceiver: BroadcastReceiver? = null
    private var foregroundReceiver: BroadcastReceiver? = null

    @Inject lateinit var sharedPreferences: SharedPreferences
    @Inject @Named("default") lateinit var defaultSharedPreferences: SharedPreferences
    @Inject lateinit var deviceAlarmController: DeviceAlarmController
    @Inject lateinit var audioTableManager: AudioTableManager
    @Inject lateinit var deviceAlarmTableManager: DeviceAlarmTableManager
    @Inject lateinit var backgroundTaskReceiver: BackgroundTaskReceiver
    @Inject lateinit var mDatabase: DatabaseReference
    @Inject lateinit var mAccount: Account
    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var lifeCycle: LifeCycle
    @Inject lateinit var realmAlarmFailureLog: RealmAlarmFailureLog

    var firebaseUser: FirebaseUser? = null
    @Inject
    fun BaseActivity(firebaseUser: FirebaseUser?) {
        this.firebaseUser = firebaseUser
    }

    init {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance()
        mCurrentUser = BaseApplication.mCurrentUser
    }

    abstract fun inject(component: RoosterApplicationComponent)

    public override fun onStart() {
        super.onStart()
        mAuthListener?.let { mAuth?.addAuthStateListener(it) }
    }

    public override fun onStop() {
        super.onStop()
        mAuthListener?.let { mAuth?.removeAuthStateListener(it) }
        foregroundReceiver?.let {
            unregisterReceiver(foregroundReceiver)
            foregroundReceiver = null
        }
    }

    public override fun onResume() {
        super.onResume()
        sharedPreferences.edit()
                .putBoolean(Constants.IS_ACTIVITY_FOREGROUND, true)
                .apply()

        /*The foreground receiver is intended to run methods once on app launch,
        the ForegroundService sends a broadcast when this occurs. */
        foregroundReceiver = object: BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                performOnceOnForeground()
            }
        }
        val foregroundIntentFilter = IntentFilter()
        foregroundIntentFilter.addAction(APP_BROUGHT_FOREGROUND)

        registerReceiver(foregroundReceiver, foregroundIntentFilter)

        startService(Intent(this, ForegroundService::class.java))
    }

    public override fun onPause() {
        super.onPause()
        sharedPreferences.edit()
                .putBoolean(Constants.IS_ACTIVITY_FOREGROUND, false)
                .apply()
    }

    private fun performOnceOnForeground() {
        // Log last seen in user metrics, to enable clearing stagnant data
        UserMetrics.updateLastSeen()
        // Log active day
        UserMetrics.logActiveDays()
        // Process any alarm failures
        realmAlarmFailureLog.processAlarmFailures(true)
        // Set shared pref to indicate whether mobile number is valid
        FirebaseNetwork.flagValidMobileNumber(this, false)
        // Check if first entry
        lifeCycle.performInception()
        //Setup day/night theme selection (based on settings, and time)
        setDayNightTheme()

        // Log new Crashlytics user
        firebaseUser?.let {
            // Check user sign in method and set Firebase user prop
            firebaseUser?.providerData?.forEach { user ->
                when {
                    user.providerId.toLowerCase().contains(FA.UserProp.sign_in_method.Google.toLowerCase()) -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java,
                            FA.UserProp.sign_in_method.Google)
                    user.providerId.toLowerCase().contains(FA.UserProp.sign_in_method.Facebook.toLowerCase()) -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java,
                            FA.UserProp.sign_in_method.Facebook)
                    user.providerId.toLowerCase().contains(FA.UserProp.sign_in_method.Email.toLowerCase()) -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java,
                            FA.UserProp.sign_in_method.Email)
                    else -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java,
                            FA.UserProp.sign_in_method.Unknown)
                }
            }

            // You can call any combination of these three methods
            Crashlytics.setUserIdentifier(firebaseUser?.uid)
            Crashlytics.setUserEmail(firebaseUser?.email)
            Crashlytics.setUserName(firebaseUser?.displayName)

            FirebaseAnalytics.getInstance(this).setUserId(firebaseUser?.uid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Inject Dagger dependencies
        BaseApplication.getRoosterApplicationComponent().inject(this)

        //Set default application settings preferences - don't overwrite existing if false
        setPreferenceManagerDefaultSettings(false)

        startFirebaseListenerService()
    }

    private fun startFirebaseListenerService() {
        // Start Firebase listeners applicable to all activities - primarily to update notifications
        if (!isServiceRunning(FirebaseListenerService::class.java))
            startService(Intent(applicationContext, FirebaseListenerService::class.java))
    }

    override fun checkInternetConnection(): Boolean {
        if (!checkMobileDataConnection()) {
            Toaster.makeToast(applicationContext, "'Download/upload on mobile data' is disabled in Settings, " + "please enable this and try again.", Toast.LENGTH_LONG)
        }
        if (this.noInternetConnection()) {
            Toaster.makeToast(applicationContext, "No internet connection was found, please " + "connect and try again.", Toast.LENGTH_LONG)
            return false
        }
        return true
    }

    private fun checkMobileDataConnection(): Boolean {
        return !(mobileDataConnection() && !defaultSharedPreferences.getBoolean(Constants.USER_SETTINGS_DOWNLOAD_ON_DATA, true))
    }

    private fun noInternetConnection(): Boolean {
        return InternetHelper.noInternetConnection(this)
    }

    private fun mobileDataConnection(): Boolean {
        return InternetHelper.mobileDataConnection(this)
    }

    protected fun startHomeActivity() {
        val homeIntent = Intent(this, MyAlarmsFragmentActivity::class.java)
        startActivity(homeIntent)
    }

    protected fun initialize(layoutId: Int) {
        setContentView(layoutId)

        //Bind to butterknife delegate
        //Calls to ButterKnife.bind can be made anywhere you would otherwise put findViewById calls.
        ButterKnife.bind(this)
    }

    override fun nodeApiService(): NodeIHTTPClient {
        val baseApplication = application as BaseApplication
        return baseApplication.nodeAPIService
    }

    override fun googleApiService(): GoogleIHTTPClient {
        val baseApplication = application as BaseApplication
        return baseApplication.googleAPIService
    }

    override fun onValidationSucceeded() {}

    override fun onValidationFailed(errors: List<ValidationError>) {
        errors.forEach {
            val view = it.view
            val message = it.getCollatedErrorMessage(this)

            if (view is TextInputEditText) {
                val parent = view.getParent() as TextInputLayout
                parent.error = message
            } else {
                Toaster.makeToast(this, message, Toast.LENGTH_LONG)
            }
        }
    }

    fun signOut() {
        try {
            //Ensure no audio remaining from old user
            audioTableManager.removeAllSocialAudioItems()
            audioTableManager.removeAllChannelAudioFiles()
            //Ensure no alarms left from old user
            deviceAlarmController.deleteAllLocalAlarms()
            //Cancel background task intents
            backgroundTaskReceiver.scheduleBackgroundDailyTask(this, false)
            ContentResolver.cancelSync(mAccount, AUTHORITY)
            //Set default application settings preferences - don't overwrite existing if false
            setPreferenceManagerDefaultSettings(true)
            sharedPreferences.edit().clear().apply()
            //Clear specific persistent collections from shared prefs
            clearJSONPersistence()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

        //End user session - auth state listener in BaseApplication will be triggered
        // and necessary signout procedure performed
        authManager.signOut()

        //Go to splash activity and onboarding
        val intent = Intent(this@BaseActivity, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun setPreferenceManagerDefaultSettings(overwrite: Boolean) {
        // ensure user settings are set to default once when new user
        // As long as you set the third argument to false, you can safely call this method
        // every time your activity starts without overriding the user's saved preferences
        // by resetting them to the defaults. However, if you set it to true, you will
        // override any previous values with the defaults.

        if (overwrite) {
            val editor = defaultSharedPreferences.edit()
            editor.remove("pref_key_user_settings").apply()
        }
        PreferenceManager.setDefaultValues(this, R.xml.application_user_settings, overwrite)
    }

    private fun clearJSONPersistence() {
        //Clear specific persistent collections from shared prefs
        val editor = defaultSharedPreferences.edit()

        editor
                .remove(JSONPersistence.SharedPrefsKeys.KEY_ALARMS_ARRAY)
                .remove(JSONPersistence.SharedPrefsKeys.KEY_MEDIA_ITEMS_ARRAY)
                .remove(JSONPersistence.SharedPrefsKeys.KEY_USER_CONTACTS_NUMBER_NAME_PAIRS_MAP)
                .remove(JSONPersistence.SharedPrefsKeys.KEY_USER_FRIENDS_ARRAY)
                .remove(JSONPersistence.SharedPrefsKeys.KEY_USER_INVITABLE_CONTACTS_ARRAY)
                .apply()
    }

    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { serviceClass.name == it.service.className }
    }

    fun requestPermissionIgnoreBatteryOptimization(context: Context) {
        if (!sharedPreferences.getBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false)
                && (Build.BRAND.toLowerCase().contains("huawei")
                || Build.BRAND.toLowerCase().contains("sony")
                || Build.BRAND.toLowerCase().contains("xiaomi"))) {
            Log.d("Brand: ", Build.BRAND)

            //Set instructions and settings intent
            var settingsNavigationString = "Try: For example go to power, battery, or optimizations settings page."
            val intent = Intent()
            when {
                Build.BRAND.toLowerCase().contains("huawei") -> {
                    settingsNavigationString = ""
                    intent.component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                    if (!isIntentCallable(intent)) {
                        settingsNavigationString = "Try: Go to 'Battery manager'>'Protected apps'"
                        intent.component = null
                        intent.action = android.provider.Settings.ACTION_SETTINGS
                    }

                    //Build content string
                    val dialogContent = (resources.getString(R.string.dialog_background_settings_1)
                            + Build.BRAND + resources.getString(R.string.dialog_background_settings_2)
                            + settingsNavigationString)

                    MaterialDialog.Builder(context)
                            .theme(Theme.LIGHT)
                            .content(dialogContent)
                            .positiveText(R.string.take_to_settings)
                            .negativeText(R.string.later)
                            .onPositive { _, _ ->
                                val editor = sharedPreferences.edit()
                                editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, true)
                                editor.apply()
                                startActivityForResult(intent, 0)
                            }
                            .onNegative { _, _ ->
                                val editor = sharedPreferences.edit()
                                editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false)
                                editor.apply()
                                startHomeActivity()
                            }
                            .canceledOnTouchOutside(false)
                            .show()
                }
                Build.BRAND.toLowerCase().contains("sony") -> {
                    settingsNavigationString = "Try: Go to 'Battery'>Settings menu>'Battery optimization'>Select Rooster app checkbox"

                    //Build content string
                    val dialogContent = (resources.getString(R.string.dialog_background_settings_sony_1)
                            + Build.BRAND + resources.getString(R.string.dialog_background_settings_sony_2))

                    MaterialDialog.Builder(context)
                            .theme(Theme.LIGHT)
                            .content(dialogContent)
                            .positiveText("Got it")
                            .onPositive { _, _ ->
                                val editor = sharedPreferences.edit()
                                editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, true)
                                editor.apply()
                            }
                            .canceledOnTouchOutside(false)
                            .show()
                }
                Build.BRAND.toLowerCase().contains("xiaomi") -> {
                    intent.action = android.provider.Settings.ACTION_SETTINGS

                    //Build content string
                    val dialogContent = (resources.getString(R.string.dialog_background_settings_xiaomi_1)
                            + Build.BRAND + resources.getString(R.string.dialog_background_settings_xiaomi_2))

                    MaterialDialog.Builder(context)
                            .theme(Theme.LIGHT)
                            .content(dialogContent)
                            .positiveText(R.string.take_to_settings)
                            .negativeText(R.string.later)
                            .onPositive { _, _ ->
                                val editor = sharedPreferences.edit()
                                editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, true)
                                editor.apply()
                                startActivityForResult(intent, 0)
                            }
                            .onNegative { _, _ ->
                                val editor = sharedPreferences.edit()
                                editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false)
                                editor.apply()
                                startHomeActivity()
                            }
                            .canceledOnTouchOutside(false)
                            .show()
                }
                Build.BRAND.toLowerCase().contains("zte") -> {
                    intent.action = android.provider.Settings.ACTION_SETTINGS

                    //Build content string
                    val dialogContent = (resources.getString(R.string.dialog_background_settings_zte_1)
                            + Build.BRAND + resources.getString(R.string.dialog_background_settings_zte_2))

                    MaterialDialog.Builder(context)
                            .theme(Theme.LIGHT)
                            .content(dialogContent)
                            .positiveText(R.string.take_to_settings)
                            .negativeText(R.string.later)
                            .onPositive { _, _ ->
                                val editor = sharedPreferences.edit()
                                editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, true)
                                editor.apply()
                                startActivityForResult(intent, 0)
                            }
                            .onNegative { _, _ ->
                                val editor = sharedPreferences.edit()
                                editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false)
                                editor.apply()
                                startHomeActivity()
                            }
                            .canceledOnTouchOutside(false)
                            .show()
                }
            }
        }
    }

    private fun isIntentCallable(intent: Intent): Boolean {
        val list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY)
        return list.size > 0
    }

    fun setupToolbar(toolbarTitle: TextView?, title: String?): Toolbar? {
        return try {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            if (title != null) toolbarTitle?.text = title
            toolbar
        } catch (e: NullPointerException) {
            e.printStackTrace()
            null
        }
    }

    @Optional
    @OnClick(R.id.home_my_alarms)
    fun manageMyAlarm() {
        val roosterCount = audioTableManager.countUnheardSocialAudioFiles()
        if (this is MyAlarmsFragmentActivity && roosterCount > 0 && deviceAlarmTableManager.nextPendingSocialAlarm == null) {
            showAlarmSocialRoostersExplainer(this, null, roosterCount)
        } else startHomeActivity()
    }

    @Optional
    @OnClick(R.id.home_record_audio)
    open fun recordNewAudio() {
        if (!checkInternetConnection()) return
        startActivity(Intent(this, NewAudioRecordActivity::class.java))
    }

    @Optional
    @OnClick(R.id.home_discover)
    fun onClickDiscover() {
        startActivity(Intent(this, DiscoverFragmentActivity::class.java))
    }

    @Optional
    @OnClick(R.id.home_friends)
    open fun manageFriends() {
        startActivity(Intent(this, FriendsFragmentActivity::class.java))
    }

    @Optional
    @OnClick(R.id.home_my_uploads)
    open fun manageUploads() {
        startActivity(Intent(this, MessageStatusFragmentActivity::class.java))
    }

    fun updateRoosterNotification() {
        setButtonBarNotification(R.id.notification_roosters, false)
        val roosterCount = audioTableManager.countUnheardSocialAudioFiles()

        if (roosterCount > 0) {
            val deviceAlarm = deviceAlarmTableManager.nextPendingSocialAlarm

            if (deviceAlarm == null) {
                (this as? MyAlarmsFragmentActivity)?.clearRoosterNotificationFlags()
                setButtonBarNotification(R.id.notification_roosters, true)
            } else {
                (this as? MyAlarmsFragmentActivity)?.allocateRoosterNotificationFlags(deviceAlarm.setId, roosterCount!!)
            }
        }

        //Broadcast receiver filter to receive UI updates
        val firebaseListenerServiceFilter = IntentFilter()
        firebaseListenerServiceFilter.addAction(Constants.ACTION_ROOSTERNOTIFICATION)

        roosterNotificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //do something based on the intent's action
                setButtonBarNotification(R.id.notification_roosters, false)
                val roosterCountRcv = audioTableManager.countUnheardSocialAudioFiles()
                if (roosterCountRcv > 0) {
                    val deviceAlarm = deviceAlarmTableManager.nextPendingSocialAlarm
                    if (deviceAlarm == null) {
                        setButtonBarNotification(R.id.notification_roosters, true)
                    }
                }
            }
        }
        registerReceiver(roosterNotificationReceiver, firebaseListenerServiceFilter)
    }

    fun updateRequestNotification() {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //If notifications waiting, display new friend request notification
        if (BaseApplication.getNotificationFlag(Constants.FLAG_FRIENDREQUESTS) > 0)
            setButtonBarNotification(R.id.notification_friends, true)

        //Broadcast receiver filter to receive UI updates
        val firebaseListenerServiceFilter = IntentFilter()
        firebaseListenerServiceFilter.addAction(Constants.ACTION_REQUESTNOTIFICATION)

        requestNotificationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //do something based on the intent's action
                try {
                    when (intent.action) {
                        Constants.ACTION_REQUESTNOTIFICATION -> setButtonBarNotification(R.id.notification_friends, true)
                        else -> {}
                    }
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }
        }
        registerReceiver(requestNotificationReceiver, firebaseListenerServiceFilter)
    }

    override fun onDestroy() {
        roosterNotificationReceiver?.let {
            unregisterReceiver(roosterNotificationReceiver)
            roosterNotificationReceiver = null
        }
        requestNotificationReceiver?.let {
            unregisterReceiver(requestNotificationReceiver)
            requestNotificationReceiver = null
        }

        super.onDestroy()
    }

    fun setButtonBarNotification(notificationId: Int, visible: Boolean) {
        val buttonBarLayout = findViewById<LinearLayout>(R.id.button_bar)
        val buttonBarNotification = buttonBarLayout?.findViewById<ImageView>(notificationId)

        if (visible) buttonBarNotification?.visibility = View.VISIBLE
        else buttonBarNotification?.visibility = View.GONE
    }

    fun setButtonBarSelection(): Boolean {
        try {
            when {
                this is MyAlarmsFragmentActivity -> findViewById<View>(R.id.home_my_alarms)?.isSelected = true
                this is MessageStatusFragmentActivity -> findViewById<View>(R.id.home_my_uploads)?.isSelected = true
                this is NewAudioRecordActivity -> findViewById<View>(R.id.home_record_audio)?.isSelected = true
                this is DiscoverFragmentActivity -> findViewById<View>(R.id.home_discover)?.isSelected = true
                this is FriendsFragmentActivity -> findViewById<View>(R.id.home_friends)?.isSelected = true
                else -> return false
            }
            return true
        } catch (e: NullPointerException) {
            e.printStackTrace()
            return false
        }
    }

    fun setDayNightTheme(): Boolean {
        val calendar = Calendar.getInstance()
        try {
            val dayNightThemeArrayEntries = resources.getStringArray(R.array.user_settings_day_night_theme_entry_values)

            if (dayNightThemeArrayEntries[0] == defaultSharedPreferences.getString(Constants.USER_SETTINGS_DAY_NIGHT_THEME, "")) {
                if (calendar.get(Calendar.HOUR_OF_DAY) >= 18 || calendar.get(Calendar.HOUR_OF_DAY) < 7) {
                    return setThemeNight()
                } else if (calendar.get(Calendar.HOUR_OF_DAY) >= 7) {
                    return setThemeDay()
                }
            } else if (dayNightThemeArrayEntries[1] == defaultSharedPreferences.getString(Constants.USER_SETTINGS_DAY_NIGHT_THEME, "")) {
                return setThemeDay()
            } else if (dayNightThemeArrayEntries[2] == defaultSharedPreferences.getString(Constants.USER_SETTINGS_DAY_NIGHT_THEME, "")) {
                return setThemeNight()
            } else {
                if (calendar.get(Calendar.HOUR_OF_DAY) >= 18 || calendar.get(Calendar.HOUR_OF_DAY) < 7) {
                    return setThemeNight()
                } else if (calendar.get(Calendar.HOUR_OF_DAY) >= 7) {
                    return setThemeDay()
                }
            }
            return true
        } catch (e: NullPointerException) {
            e.printStackTrace()
            return false
        }
    }

    private fun setThemeDay(): Boolean {
        return try {
            findViewById<View>(R.id.main_content)?.isSelected = false
            findViewById<View>(R.id.toolbar)?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.rooster_blue, null))
            findViewById<View>(R.id.tabs)?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.rooster_blue, null))
            true
        } catch (e: NullPointerException) {
            e.printStackTrace()
            false
        }
    }

    private fun setThemeNight(): Boolean {
        return try {
            findViewById<View>(R.id.main_content)?.isSelected = true
            findViewById<View>(R.id.toolbar)?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.rooster_dark_blue, null))
            findViewById<View>(R.id.tabs)?.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.rooster_dark_blue, null))
            true
        } catch (e: NullPointerException) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private val TAG = BaseActivity::class.java.simpleName

        var mCurrentUser: User? = null

        fun setBadge(context: Context, count: Int) {
            val launcherClassName = getLauncherClassName(context) ?: return
            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
            intent.putExtra("badge_count", count)
            intent.putExtra("badge_count_package_name", context.packageName)
            intent.putExtra("badge_count_class_name", launcherClassName)
            context.sendBroadcast(intent)
        }

        private fun getLauncherClassName(context: Context): String? {
            val pm = context.packageManager

            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            pm.queryIntentActivities(intent, 0).forEach {
                val pkgName = it.activityInfo.applicationInfo.packageName
                if (pkgName.equals(context.packageName, ignoreCase = true)) {
                    return it.activityInfo.name
                }
            }
            return null
        }

        fun showAlarmSocialRoostersExplainer(context: Context, alarm: Alarm?, count: Int?) {
            var unseenRoosters = count
            if (context is MyAlarmsFragmentActivity) {

                //If accessed from button bar
                if (alarm != null) {
                    unseenRoosters = alarm.unseen_roosters
                }

                var dialogText = "Social roosters are voice notes from your friends that wake you up."
                dialogText = when (unseenRoosters) {
                    0 -> "Social roosters are voice notes from your friends that wake you up."
                    1 -> "You have received " + unseenRoosters.toString() + " rooster from a friend to wake you up."
                    else -> "You have received " + unseenRoosters.toString() + " roosters from friends to wake you up."
                }

                MaterialDialog.Builder(context)
                        .theme(Theme.LIGHT)
                        .content(dialogText)
                        .show()
            }
        }
    }
}
