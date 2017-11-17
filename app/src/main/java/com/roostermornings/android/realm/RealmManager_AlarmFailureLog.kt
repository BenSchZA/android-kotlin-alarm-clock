package com.roostermornings.android.realm

import android.content.Context
import android.content.SharedPreferences
import android.support.design.widget.Snackbar
import com.crashlytics.android.Crashlytics
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager
import io.realm.Realm
import java.util.*
import javax.inject.Inject
import com.google.gson.FieldAttributes
import com.google.gson.ExclusionStrategy
import com.google.gson.GsonBuilder
import com.roostermornings.android.activity.MyAlarmsFragmentActivity
import com.roostermornings.android.firebase.FA
import com.roostermornings.android.util.Constants
import com.roostermornings.android.snackbar.SnackbarManager
import io.realm.RealmObject
import java.lang.reflect.Modifier


/**
 * Created by bscholtz on 2017/11/04.
 */

class RealmManager_AlarmFailureLog(val context: Context) {

    @Inject lateinit var realm: Realm
    @Inject lateinit var realmManagerScheduledSnackbar: RealmManager_ScheduledSnackbar
    @Inject lateinit var alarmTableManager: DeviceAlarmTableManager
    @Inject lateinit var sharedPreferences: SharedPreferences

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    fun closeRealm() {
        realm.close()
    }

    fun getAllAlarmFailureLogs(): List<AlarmFailureLog> {
        return realm.where(AlarmFailureLog::class.java)
                .findAll().toList()
    }

    fun processAlarmFailures(clear: Boolean) {
        val currentTime = Calendar.getInstance().timeInMillis

        realm.executeTransaction {
            // Query Realm, looking for alarm failures, as defined in AlarmFailureLog comments
            var tempResults = realm
                    .where(AlarmFailureLog::class.java)
                    .lessThan("scheduledTime", currentTime)
                    .not()
                    .beginGroup()
                        .equalTo("fired", true)
                        .equalTo("activated", true)
                        .equalTo("running", false)
                    .endGroup().findAll()

            // Find all results where alarm delayed by more than 5 minutes
            val delayedAlarms = tempResults.filter { Math.abs(it.scheduledTime - it.firedTime) >= Constants.TIME_MILLIS_1_MINUTE*5 }

            tempResults = tempResults.where()
                    .beginGroup()
                        .equalTo("fired", false)
                        .or()
                        .equalTo("activated", false)
                        .or()
                        .equalTo("running", false)
                        .or()
                        .equalTo("seen", false)
                        .or()
                        .equalTo("heard", false)
                        .or()
                        .equalTo("failsafe", true)
                        .or()
                        .equalTo("stream", true)
                        .or()
                        .equalTo("def", true)
                            .beginGroup()
                                .equalTo("content", true)
                                .or()
                                .equalTo("channel", true)
                            .endGroup()
                        .or()
                        .equalTo("channel", true)
                            .beginGroup()
                                .equalTo("content", false)
                            .endGroup()
                    .endGroup().findAll()

            // Find all objects matching query
            val failures: List<AlarmFailureLog> = tempResults + delayedAlarms.filterNot { tempResults.contains(it) }

            // For each alarm failure, mark it as such for easy fetching later
            failures.forEach { alarmFailure ->
                alarmFailure.failure = true
            }
        }
        // Send relevant logs to Crashlytics
        sendAlarmFailureLogs(clear)
        // Clear all logs older than current time in millis
        if(clear) clearOldAlarmFailureLogs()
    }

    private fun sendAlarmFailureLogs(clear: Boolean): List<AlarmFailureLog> {
        // Ignore serialization of RealmObject subclass to avoid StackOverFlow error
        val GSON = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .excludeFieldsWithModifiers(Modifier.ABSTRACT)
                .addSerializationExclusionStrategy(object : ExclusionStrategy {
            override fun shouldSkipField(f: FieldAttributes): Boolean {
                return f.declaringClass == RealmObject::class.java
            }

            override fun shouldSkipClass(clazz: Class<*>): Boolean {
                return false
            }
        }).create()

        // Log each alarm failure to Crashlytics before deleting from Realm
        getAlarmFailures().onEach { alarmFailure ->
            val unmanagedAlarmFailure = realm.copyFromRealm(alarmFailure)
            Crashlytics.log("Alarm Failure: \n" + GSON.toJson(unmanagedAlarmFailure))

            generateScheduledSnackbarForAlarmFailure(unmanagedAlarmFailure)

            realm.executeTransaction {
                if(clear) alarmFailure.deleteFromRealm()
            }

            //Log a firebase analytics event indicating whether an attempt was/should have been made to play audio content
            FA.LogMany(FA.Event.default_alarm_play::class.java,
                    arrayOf(FA.Event.default_alarm_play.Param.attempt_to_play, FA.Event.default_alarm_play.Param.fatal_failure),
                    arrayOf<Any>(unmanagedAlarmFailure.content, (!unmanagedAlarmFailure.seen || !unmanagedAlarmFailure.heard) && !unmanagedAlarmFailure.interaction))
        }.takeIf {
            // If the list is not empty, continue
            it.isNotEmpty()
        }?.also {
            // Send Crashlytics log and return list from method sendAlarmFailureLogs()
            Crashlytics.logException(Throwable("Alarm Failure Report"))
            return@sendAlarmFailureLogs it
        }
        return emptyList()
    }

    private fun generateScheduledSnackbarForAlarmFailure(alarmFailureLog: AlarmFailureLog) {
        val snackbarQueueElement = SnackbarManager.Companion.SnackbarQueueElement()
        snackbarQueueElement.actionText = "Learn more"
        snackbarQueueElement.priority = 10
        snackbarQueueElement.length = Snackbar.LENGTH_INDEFINITE

        val activityName = MyAlarmsFragmentActivity::class.java.name

        // 88 characters max for text
        alarmFailureLog.takeIf {
            Math.abs(it.scheduledTime - it.firedTime) >= Constants.TIME_MILLIS_1_MINUTE*5}?.let {

            snackbarQueueElement.text = "Your alarm was delayed. Your phone may be doing this to save power."

            realmManagerScheduledSnackbar.updateOrCreateScheduledSnackbarEntry(snackbarQueueElement, activityName, -1L)
            return@generateScheduledSnackbarForAlarmFailure
        }
        alarmFailureLog.takeIf {
            it.def && it.channel && !it.content}?.let {

            snackbarQueueElement.text = "Received a default alarm tone? We need an internet connection when alarm set."
            snackbarQueueElement.dialog = true
            snackbarQueueElement.dialogTitle = "What's this default alarm tone about?"
            snackbarQueueElement.dialogText = "Blah blah blah..."

            realmManagerScheduledSnackbar.updateOrCreateScheduledSnackbarEntry(snackbarQueueElement, activityName, -1L)
            return@generateScheduledSnackbarForAlarmFailure
        }
        alarmFailureLog.takeIf {
            it.stream  && it.channel && !it.content}?.let {

            snackbarQueueElement.text = "Your alarm was streamed. We need an internet connection when your alarm is set."

            realmManagerScheduledSnackbar.updateOrCreateScheduledSnackbarEntry(snackbarQueueElement, activityName, -1L)
            return@generateScheduledSnackbarForAlarmFailure
        }
        alarmFailureLog.takeIf {
            (!it.fired || !it.activated) && !it.running}?.let {

            //Show dialog explainer again by clearing shared pref
            val editor = sharedPreferences.edit()
            editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false)
            editor.apply()

            snackbarQueueElement.text = "Your alarm didn't fire. Your phone may be blocking Rooster."

            realmManagerScheduledSnackbar.updateOrCreateScheduledSnackbarEntry(snackbarQueueElement, activityName, -1L)
            return@generateScheduledSnackbarForAlarmFailure
        }
        /*//Show dialog explainer again by clearing shared pref
        val editor = sharedPreferences.edit()
        editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false)
        editor.apply()*/
    }

    fun clearOldAlarmFailureLogs() {
        // Clear all logs older than current time - i.e. pending intent that has fired (hopefully)
        val currentTime = Calendar.getInstance().timeInMillis

        realm.executeTransaction {
            realm.where(AlarmFailureLog::class.java)
                    .lessThan("scheduledTime", currentTime)
                    .findAll().deleteAllFromRealm()
        }
    }

    fun getAlarmFailures(): List<AlarmFailureLog> {
        // Return all logs with failure flag set. As generated in processAlarmFailures.
        return realm.where(AlarmFailureLog::class.java)
                .equalTo("failure", true)
                .findAll().toList()
    }

    fun getAlarmFailureLogByUIDAndPIID(alarmUid: String, pendingIntentID: Int): AlarmFailureLog? {
        return realm.where(AlarmFailureLog::class.java)
                .equalTo("pendingIntentID", pendingIntentID)
                .equalTo("alarmUid", alarmUid)
                .findFirst()
    }

    fun getAlarmFailureLogMillisSlot(pendingIntentID: Int?, operation: (AlarmFailureLog) -> Unit): AlarmFailureLog? {
        // scheduledTime is a unique key - return the slot where scheduledTime is equal to the
        // millis set for the specific pending intent passed to method
        val millis = alarmTableManager.getMillisOfPendingIntent(pendingIntentID?:-1)

        val alarmFailureLog = realm.where(AlarmFailureLog::class.java)
                .equalTo("scheduledTime", millis)
                .findFirst()

        // For a non-null log, perform the operation from the lambda within a Realm transaction
        // e.g. alarmFailureLog.fired = true
        alarmFailureLog?.let { notNullLog ->
            realm.executeTransaction {
                operation(notNullLog)
            }
        }

        return alarmFailureLog
    }

    fun updateOrCreateAlarmFailureLogEntry(alarmFailureLog: AlarmFailureLog) {
        // Insert a new entry, or update if it exists.
        // Working with an unmanaged Realm object, you're only guaranteed of no duplication if
        // primary key present.
        realm.executeTransaction {
            realm.insertOrUpdate(alarmFailureLog)
        }
    }

    fun tryDeleteAlarmFailureLogByUIDAndPIID(alarmUid: String, pendingIntentID: Int) {
        realm.executeTransaction{
            getAlarmFailureLogByUIDAndPIID(alarmUid, pendingIntentID)?.deleteFromRealm()
        }
    }
}