package com.roostermornings.android.realm

import android.content.Context
import com.crashlytics.android.Crashlytics
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager
import io.realm.Realm
import java.util.*
import javax.inject.Inject
import com.google.gson.FieldAttributes
import com.google.gson.ExclusionStrategy
import com.google.gson.GsonBuilder
import io.realm.RealmObject
import java.lang.reflect.Modifier


/**
 * Created by bscholtz on 2017/11/04.
 */

class RealmManager_AlarmFailureLog(val context: Context) {

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

    fun processAlarmFailures(clear: Boolean) {
        val currentTime = Calendar.getInstance().timeInMillis

        realm.executeTransaction {
            // Query Realm, looking for alarm failures, as defined in AlarmFailureLog comments
            val query = realm
                    .where(AlarmFailureLog::class.java)
                    .lessThan("scheduledTime", currentTime)
                    .not()
                    .beginGroup()
                        .equalTo("fired", true)
                        .equalTo("activated", true)
                        .equalTo("running", false)
                    .endGroup()
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
                        .equalTo("content", true)
                            .beginGroup()
                                .equalTo("def", true)
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
            Crashlytics.log("Alarm Failure: \n" + GSON.toJson(realm.copyFromRealm(alarmFailure)))
            realm.executeTransaction {
                if(clear) alarmFailure.deleteFromRealm()
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