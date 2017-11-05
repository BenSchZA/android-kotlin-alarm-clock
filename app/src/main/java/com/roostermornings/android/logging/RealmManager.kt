package com.roostermornings.android.logging

import android.content.Context
import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.domain.User
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager
import io.realm.Realm
import io.realm.RealmResults
import java.util.*
import javax.inject.Inject
import com.google.gson.FieldAttributes
import com.google.gson.ExclusionStrategy
import com.google.gson.GsonBuilder
import io.realm.RealmObject


/**
 * Created by bscholtz on 2017/11/04.
 */

class RealmManager(val context: Context) {

    @Inject lateinit var realm: Realm
    @Inject lateinit var alarmTableManager: DeviceAlarmTableManager

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

    fun processAlarmFailures() {
        val currentTime = Calendar.getInstance().timeInMillis

        realm.executeTransaction {
            // Query Realm, looking for alarm failures, as defined in AlarmFailureLog comments
            val query = realm
                    .where(AlarmFailureLog::class.java)
                    .lessThan("scheduledTime", currentTime)
                    .not()
                    .beginGroup()
                        .equalTo("activated", true)
                        .equalTo("running", false)
                    .endGroup()
                    .beginGroup()
                        .equalTo("seen", false)
                        .or()
                        .equalTo("heard", false)
                        .or()
                        .equalTo("content", true)
                            .beginGroup()
                                .equalTo("def", true)
                                .or()
                                .equalTo("failsafe", true)
                                .or()
                                .equalTo("heard", false)
                            .endGroup()
                    .endGroup()

            // Find all objects matching query
            val failures: List<AlarmFailureLog> = query.findAll()

            // For each alarm failure, mark it as such for easy fetching later
            failures.forEach { alarmFailure ->
                alarmFailure.failure = true
            }
        }
        // Send relevant logs to Crashlytics
        sendAlarmFailureLogs()
        // Clear all logs older than current time in millis
        clearOldAlarmFailureLogs()
    }

    private fun sendAlarmFailureLogs(): List<AlarmFailureLog> {
        // Ignore serialization of RealmObject subclass to avoid StackOverFlow error
        val GSON = GsonBuilder().addSerializationExclusionStrategy(object : ExclusionStrategy {
            override fun shouldSkipField(f: FieldAttributes): Boolean {
                return f.declaringClass == RealmObject::class.java
            }

            override fun shouldSkipClass(clazz: Class<*>): Boolean {
                return false
            }
        }).create()

        // Log each alarm failure to Crashlytics before deleting from Realm
        getAlarmFailures().onEach { alarmFailure ->
            Crashlytics.log("Alarm Failure: \n" + GSON.toJson(alarmFailure))
            realm.executeTransaction {
                alarmFailure.deleteFromRealm()
            }
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

    private fun clearOldAlarmFailureLogs() {
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

    fun getAlarmFailureLogMillisSlot(pendingIntentID: Int?, operation: (AlarmFailureLog) -> AlarmFailureLog): AlarmFailureLog? {
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