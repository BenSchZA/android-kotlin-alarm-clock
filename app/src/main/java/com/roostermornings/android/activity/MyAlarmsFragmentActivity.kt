/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity

import android.accounts.Account
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import com.google.firebase.auth.FirebaseUser
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.adapter.MyAlarmsListAdapter
import com.roostermornings.android.adapter_data.RoosterAlarmManager
import com.roostermornings.android.custom_ui.SquareFrameLayout
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.Alarm
import com.roostermornings.android.firebase.AuthManager
import com.roostermornings.android.firebase.FA
import com.roostermornings.android.firebase.FirebaseNetwork
import com.roostermornings.android.realm.RealmManager_AlarmFailureLog
import com.roostermornings.android.snackbar.SnackbarManager
import com.roostermornings.android.sqlutil.AudioTableManager
import com.roostermornings.android.sqlutil.DeviceAlarmController
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager
import com.roostermornings.android.sync.DownloadSyncAdapter
import com.roostermornings.android.util.ConnectivityUtils
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.JSONPersistence
import com.roostermornings.android.util.LifeCycle
import com.roostermornings.android.widgets.AlarmToggleWidget

import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject

import butterknife.BindView
import butterknife.OnClick
import com.roostermornings.android.onboarding.CustomCommandInterface
import com.roostermornings.android.onboarding.InterfaceCommands
import com.roostermornings.android.onboarding.ProfileCreationFragment
import me.grantland.widget.AutofitTextView

import com.roostermornings.android.util.Constants.AUTHORITY
import kotlinx.android.synthetic.main.custom_toolbar.*

class MyAlarmsFragmentActivity : BaseActivity(), CustomCommandInterface {

    @BindView(R.id.my_alarms_coordinator_layout)
    lateinit var myAlarmsCoordinatorLayout: CoordinatorLayout
    @BindView(R.id.home_alarmsListView)
    lateinit var mRecyclerView: RecyclerView
    @BindView(R.id.toolbar_title)
    lateinit var toolbarTitle: TextView
    @BindView(R.id.button_bar)
    lateinit var buttonBarLayout: LinearLayout
    @BindView(R.id.add_alarm)
    lateinit var buttonAddAlarm: FloatingActionButton

    @BindView(R.id.add_alarm_filler)
    lateinit var addAlarmFiller: SquareFrameLayout
    @BindView(R.id.add_alarm_filler_text)
    lateinit var addAlarmFillerText: AutofitTextView

    @BindView(R.id.swiperefresh)
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val mAlarms = ArrayList<Alarm>()
    private var mAdapter: RecyclerView.Adapter<*>? = null

    private var receiver: BroadcastReceiver? = null

    private var toolbar: Toolbar? = null

    private var snackbarManager: SnackbarManager? = null

    private var profileCreationFragment: ProfileCreationFragment? = null

    @Inject
    lateinit var deviceAlarmController: DeviceAlarmController
    @Inject
    lateinit var deviceAlarmTableManager: DeviceAlarmTableManager
    @Inject
    lateinit var audioTableManager: AudioTableManager
    @Inject
    lateinit var roosterAlarmManager: RoosterAlarmManager
    @Inject
    lateinit var mAccount: Account
    @Inject
    lateinit var lifeCycle: LifeCycle
    @Inject
    lateinit var jsonPersistence: JSONPersistence
    @Inject
    lateinit var realmManagerAlarmFailureLog: RealmManager_AlarmFailureLog
    @Inject
    lateinit var connectivityUtils: ConnectivityUtils
    @Inject
    lateinit var authManager: AuthManager

    private var firebaseUser: FirebaseUser? = null

    @Inject
    fun MyAlarmsFragmentActivity(firebaseUser: FirebaseUser?) {
        this.firebaseUser = firebaseUser
    }

    companion object {
        private val TAG = MyAlarmsFragmentActivity::class.java.simpleName
    }

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    override fun onStart() {
        super.onStart()
        //Display notifications
        updateRoosterNotification()
        updateRequestNotification()
        //Setup day/night theme selection (based on settings, and time)
        setDayNightTheme()
    }

    public override fun onPause() {
        super.onPause()
        // Persist alarms for seamless loading
        jsonPersistence.alarms = mAlarms

        // Remove Realm listeners, and close Realm
        snackbarManager?.destroy()

        // Update app widget
        AlarmToggleWidget.sendUpdateBroadcast(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(R.layout.activity_my_alarms)
        BaseApplication.getRoosterApplicationComponent().inject(this)

        // Final context to be used in threads
        val context = this

        // Process any alarm failures
        realmManagerAlarmFailureLog.processAlarmFailures(true)

        // Set shared pref to indicate whether mobile number is valid
        FirebaseNetwork.flagValidMobileNumber(this, false)

        // Check if first entry
        lifeCycle.performInception()

        snackbarManager = SnackbarManager(this, myAlarmsCoordinatorLayout)

        //FirstMileManager firstMileManager = new FirstMileManager();
        //firstMileManager.createShowcase(this, new ViewTarget(buttonAddAlarm.getId(), this), 1);

        // Download any social or channel audio files
        ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle())

        /*
        * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
        * performs a swipe-to-refresh gesture.
        */
        swipeRefreshLayout.setOnRefreshListener {
            // This method performs the actual data-refresh operation.
            // The method calls setRefreshing(false) when it's finished.
            roosterAlarmManager.fetchAlarms(mAlarms)
            refreshDownloadIndicator()
        }

        // UI setup thread
        object : Thread() {
            override fun run() {

                // Set highlighting of button bar
                setButtonBarSelection()
                // Animate FAB with pulse
                buttonAddAlarm.animation = AnimationUtils.loadAnimation(context, R.anim.pulse)
                // Set toolbar title
                toolbar = setupToolbar(toolbarTitle, getString(R.string.my_alarms))
                // Set download indicator
                refreshDownloadIndicator()

                // Set up adapter for monitoring alarm objects
                mAdapter = MyAlarmsListAdapter(mAlarms, this@MyAlarmsFragmentActivity)
                // Use a linear layout manager
                val mLayoutManager = LinearLayoutManager(context)
                mRecyclerView.layoutManager = mLayoutManager
                mRecyclerView.adapter = mAdapter

                // Check for, and load, persisted data
                if (!jsonPersistence.alarms.isEmpty()) {
                    mAlarms.addAll(jsonPersistence.alarms)
                    mAdapter?.notifyDataSetChanged()
                } else if (checkInternetConnection()) {
                    if (!swipeRefreshLayout.isRefreshing) swipeRefreshLayout.isRefreshing = true
                }

                // Log new Crashlytics user
                firebaseUser?.let {
                    // Check user sign in method and set Firebase user prop
                    firebaseUser?.providerData?.forEach { user ->
                        when {
                            user.providerId.toLowerCase().contains(FA.UserProp.sign_in_method.Google.toLowerCase()) -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java, FA.UserProp.sign_in_method.Google)
                            user.providerId.toLowerCase().contains(FA.UserProp.sign_in_method.Facebook.toLowerCase()) -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java, FA.UserProp.sign_in_method.Facebook)
                            user.providerId.toLowerCase().contains(FA.UserProp.sign_in_method.Email.toLowerCase()) -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java, FA.UserProp.sign_in_method.Email)
                            else -> FA.SetUserProp(FA.UserProp.sign_in_method::class.java, FA.UserProp.sign_in_method.Unknown)
                        }
                    }

                    // You can call any combination of these three methods
                    Crashlytics.setUserIdentifier(firebaseUser?.uid)
                    Crashlytics.setUserEmail(firebaseUser?.email)
                    Crashlytics.setUserName(firebaseUser?.displayName)
                    FirebaseNetwork.updateLastSeen()
                    }
                }
        }.run()

        // Process intent bundle thread
        object : Thread() {
            override fun run() {
                val extras = intent.extras
                if (extras != null && extras.containsKey("message")) {
                    val fcmMessage = extras.getString("message", "")
                    if (fcmMessage.isNotEmpty()) {

                        MaterialDialog.Builder(applicationContext)
                                .title("Hey, we thought you should know!")
                                .content(fcmMessage)
                                .positiveText(R.string.ok)
                                .negativeText("")
                                .show()
                    }
                }

                // Clear snooze if action
                if (Constants.ACTION_CANCEL_SNOOZE == intent.action) {
                    if (extras?.getString(Constants.EXTRA_ALARMID)?.isNotBlank() == true) {
                        deviceAlarmController.snoozeAlarm(extras.getString(Constants.EXTRA_ALARMID), true)
                    }
                }
            }
        }.run()

        RoosterAlarmManager.onFlagAlarmManagerDataListener = object : RoosterAlarmManager.Companion.OnFlagAlarmManagerDataListener {
            override fun onAlarmDataChanged(freshAlarms: ArrayList<Alarm>) {
                // Check if persisted data is fresh
                if (mAlarms !== freshAlarms) {
                    mAlarms.clear()
                    mAlarms.addAll(freshAlarms)
                }
            }

            override fun onSyncFinished() {
                //Sort alarms according to time
                sortAlarms(mAlarms)
                toggleAlarmFiller()

                //Recreate all enabled alarms as failsafe
                deviceAlarmController.rebootAlarms()
                //Case: local has an alarm that firebase doesn't Result: delete local alarm
                deviceAlarmController.syncAlarmSetGlobal(mAlarms)

                //Load content and stop refresh indicator
                mAdapter?.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
                //Configure rooster notification indicator
                updateRoosterNotification()
            }
        }

        //Refresh alarms list from background thread
        roosterAlarmManager.fetchAlarms(mAlarms)
    }

    private fun refreshDownloadIndicator() {
        if (!deviceAlarmTableManager.isAlarmTableEmpty) {

            // When sync icon clicked, try refresh content
            toolbar?.setNavigationOnClickListener {
                refreshDownloadIndicator()
                //Download any social or channel audio files
                ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle())
            }

            /* If there is no pending alarm, or pending alarm is synced,
            indicate with icon and clear no-internet snackbar*/
            if (deviceAlarmTableManager.nextPendingAlarm == null || deviceAlarmTableManager.isNextPendingAlarmSynced) {
                toolbar?.setNavigationIcon(R.drawable.ic_cloud_done_white_24dp)

                // If "no internet" high priority snackbar is showing, dismiss it, we're synced
                if (snackbarManager?.previousState === SnackbarManager.State.NO_INTERNET) {
                    snackbarManager?.dismissSnackbar()
                }
            } else {
                // Check the current connectivity status, display appropriate icon and snackbar
                connectivityUtils.isActive { active ->
                    if (active) {
                        animateRefreshDownloadIndicator()
                    } else {
                        toolbar?.setNavigationIcon(R.drawable.ic_cloud_off_white_24dp)
                        snackbarManager?.generateNoInternetConnection()
                    }
                }
            }

            // Listen for channel download complete notices from sync adapter
            DownloadSyncAdapter.setOnChannelDownloadListener(object : DownloadSyncAdapter.OnChannelDownloadListener {
                override fun onChannelDownloadStarted(channelId: String) {
                    // When download starts, indicate this to user
                    if (!deviceAlarmTableManager.isNextPendingAlarmSynced) {
                        animateRefreshDownloadIndicator()
                    }
                }

                override fun onChannelDownloadComplete(valid: Boolean, channelId: String) {
                    // When download completes, indicate this to user
                    if (deviceAlarmTableManager.isNextPendingAlarmSynced) {
                        toolbar?.setNavigationIcon(R.drawable.ic_cloud_done_white_24dp)
                        if (snackbarManager?.previousState === SnackbarManager.State.SYNCING)
                            snackbarManager?.generateFinished()
                    }
                }
            })
        } else {
            // If there are no alarms, clear navigation icon
            toolbar?.navigationIcon = null
        }

        //Download any social or channel audio files
        ContentResolver.requestSync(mAccount, AUTHORITY, DownloadSyncAdapter.getForceBundle())
    }

    private fun animateRefreshDownloadIndicator() {
        val drawableDownloadIndicator = ResourcesCompat.getDrawable(resources, R.drawable.ic_cloud_download_white_24dp, null)
        if (drawableDownloadIndicator != null) {
            toolbar?.navigationIcon = drawableDownloadIndicator

            val animator = ObjectAnimator.ofPropertyValuesHolder(
                    drawableDownloadIndicator,
                    PropertyValuesHolder.ofInt("alpha", 255, 120))
            animator.duration = 1000
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.repeatMode = ObjectAnimator.REVERSE
            animator.start()
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun sortAlarms(alarms: ArrayList<Alarm>) {
        //Take arraylist and sort by date
        Collections.sort(alarms) { lhs, rhs -> lhs.millis.compareTo(rhs.millis) }
        mAdapter?.notifyDataSetChanged()
    }

    fun clearRoosterNotificationFlags() {
        for (alarm in mAlarms) alarm.unseen_roosters = 0
        mAdapter?.notifyDataSetChanged()
    }

    fun allocateRoosterNotificationFlags(alarmId: String, roosterCount: Int) {
        mAlarms.forEach { alarm ->
            alarm.unseen_roosters = 0
            if (alarm.getUid() == alarmId) {
                alarm.unseen_roosters = roosterCount
            }
        }
        mAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver)
            receiver = null
        }
        // Close Realm object
        realmManagerAlarmFailureLog.closeRealm()
        super.onDestroy()
    }

    private var mMenu: Menu? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu
        menuInflater.inflate(R.menu.menu_main, menu)
        if (!authManager.isUserSignedIn()) {
            menu.findItem(R.id.action_signout_signin)?.setTitle(R.string.action_signin)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        return when(id) {
            R.id.action_profile -> {
                val i = Intent(this, ProfileActivity::class.java)
                startActivity(i)
                true
            }
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                true
            }
            R.id.action_signout_signin -> {
                if (authManager.isUserSignedIn()) {
                    signOut()
                } else {
                    appbar.visibility = View.GONE

                    profileCreationFragment = ProfileCreationFragment.newInstance(ProfileCreationFragment.Companion.Source.HOME_PAGE)
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.fillerContainer, profileCreationFragment)
                            .commit()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCustomCommand(command: InterfaceCommands.Companion.Command) {
        when(command) {
            // When sign-in complete, remove fragment
            InterfaceCommands.Companion.Command.PROCEED -> {
                appbar.visibility = View.VISIBLE

                profileCreationFragment?.let {
                    supportFragmentManager
                            .beginTransaction()
                            .remove(it)
                            .commit()
                }

                if(authManager.isUserSignedIn()) {
                    // Change menu entry to "Sign out"
                    mMenu?.findItem(R.id.action_signout_signin)
                            ?.setTitle(R.string.action_signout)
                }
            }
            else -> {}
        }
    }

    fun toggleAlarmSetEnable(alarm: Alarm, enabled: Boolean) {
        //Toggle alarm set enabled
        deviceAlarmController.setSetEnabled(alarm.getUid(), enabled, true)
        //Set adapter arraylist item to enabled
        val alarmIndex = mAlarms.indexOf(alarm)
        if (alarmIndex > -1) mAlarms[alarmIndex].isEnabled = enabled
        //Update notification of pending social roosters
        updateRoosterNotification()
        toggleAlarmFiller()
        refreshDownloadIndicator()
    }

    fun deleteAlarm(alarmId: String) {
        try {
            //Remove alarm *set* from local SQL database using retrieved Uid from firebase && Remove alarm from firebase
            deviceAlarmController.deleteAlarmSetGlobal(alarmId)
            //Update notification of pending social roosters
            updateRoosterNotification()
            toggleAlarmFiller()
            refreshDownloadIndicator()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    private fun toggleAlarmFiller() {
        if (mAlarms.isEmpty()) {
            //For pre-Lollipop devices use VectorDrawableCompat to get your vector from resources
            val vectorDrawable = VectorDrawableCompat.create(this.resources, R.drawable.ic_alarm_add_white_24px, null)
            addAlarmFiller.background = vectorDrawable
            addAlarmFiller.visibility = View.VISIBLE
            addAlarmFillerText.visibility = View.VISIBLE
        } else {
            addAlarmFiller.visibility = View.GONE
            addAlarmFillerText.visibility = View.GONE
        }
    }

    @OnClick(R.id.add_alarm)
    fun onClickAddAlarm() {
        startActivity(Intent(this, NewAlarmFragmentActivity::class.java))
    }

    @OnClick(R.id.add_alarm_filler)
    fun onClickAddAlarmFiller() {
        startActivity(Intent(this, NewAlarmFragmentActivity::class.java))
    }

    @OnClick(R.id.add_alarm_filler_text)
    fun onClickAddAlarmFillerText() {
        startActivity(Intent(this, NewAlarmFragmentActivity::class.java))
    }

    fun editAlarm(alarmId: String) {
        val intent = Intent(this@MyAlarmsFragmentActivity, NewAlarmFragmentActivity::class.java)
        intent.putExtra(Constants.EXTRA_ALARMID, alarmId)
        startActivity(intent)
    }
}
