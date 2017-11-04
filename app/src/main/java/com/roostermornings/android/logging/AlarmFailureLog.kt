package com.roostermornings.android.logging

import android.database.sqlite.SQLiteTransactionListener
import com.activeandroid.ActiveAndroid
import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import com.activeandroid.query.Select
import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import java.util.*

/**
 * Created by bscholtz on 2017/11/01.
 */

@Table(name="AlarmFailureLogs")
class AlarmFailureLog(
        // Pending intent ID
        @Column(name="PIID", index = true)
        var pendingIntentID: Int = -1,
        // Firebase UID of alarm
        @Column(name="UID", index = true)
        var alarmUid: String = "",
        // Time of scheduled alarm
        @Column(name="Time", index = true)
        var scheduledTime: Long = -1,
        // Pending intent triggered
        @Column(name="Fired")
        var fired: Boolean = false,
        // Activation service started
        @Column(name="Activated")
        var activated: Boolean = false,
        // Activation service running
        @Column(name="Running")
        var running: Boolean = false,
        // User dismissed, snoozed, or clicked next on alarm
        @Column(name="Interaction")
        var interaction: Boolean = false,
        // Activation activity visible
        @Column(name="Seen")
        var seen: Boolean = false,
        // Audio successfully started
        @Column(name="Heard")
        var heard: Boolean = false,
        // Audio default received
        @Column(name="Def")
        var default: Boolean = false,
        // Audio fail-safe received
        @Column(name="Failsafe")
        var failsafe: Boolean = false,
        // Audio content received
        @Column(name="Content")
        var content: Boolean = false,
        // Alarm defined as failure
        @Column(name="Failure", index = true)
        var failure: Boolean = false): Model() {

    // Failure = FailureMode0 && (FailureMode1 || FailureMode2)
    // FailureMode0 = (currentTime > scheduledTime) && !(activated & !running)
    // FailureMode1 = (!seen || !heard)
    // FailureMode2 = content && (default || failsafe || !heard)

    companion object {
        fun getAllAlarmFailureLogs(): List<AlarmFailureLog> {
            return Select()
                    .all()
                    .from(AlarmFailureLog::class.java)
                    .execute()?: emptyList()
        }

        fun getAlarmFailureLogByUIDAndPIID(uid: String, pendingIntentID: Int): AlarmFailureLog? {
            return Select()
                    .from(AlarmFailureLog::class.java)
                    .where("PIID = ?", pendingIntentID)
                    .and("UID = ?", uid)
                    .executeSingle()
        }

        fun getAlarmFailureLogByPIID(pendingIntentID: Int): AlarmFailureLog? {
            return Select()
                    .from(AlarmFailureLog::class.java)
                    .where("PIID = ?", pendingIntentID)
                    .executeSingle()
        }

        fun updateOrCreateAlarmFailureLogEntry(alarmFailureLog: AlarmFailureLog) {
            var temp = getAllAlarmFailureLogs()
            getAlarmFailureLogByUIDAndPIID(alarmFailureLog.alarmUid, alarmFailureLog.pendingIntentID)?.run {
                alarmFailureLog.delete()
            }
            alarmFailureLog.save()
            temp = getAllAlarmFailureLogs()
            temp = getAllAlarmFailureLogs()
        }

        fun tryDeleteAlarmFailureLogByPIID(pendingIntentID: Int) {
            getAlarmFailureLogByPIID(pendingIntentID)?.delete()
        }

        fun processAlarmFailures() {
            val currentTime = Calendar.getInstance().timeInMillis

            val failures: List<AlarmFailureLog> = Select()
                .from(AlarmFailureLog::class.java)
                .where("(? > Time)", currentTime)
                .and("(NOT Seen OR NOT Heard) OR (Content AND (Def OR Failsafe OR NOT Heard))")
                .execute()?: emptyList()

            ActiveAndroid.beginTransaction()
            try {
                failures.forEach { alarmFailure ->
                    alarmFailure.failure = true
                    alarmFailure.delete()
                    alarmFailure.save()
                }.let {
                    ActiveAndroid.setTransactionSuccessful()
                }
            } finally {
                ActiveAndroid.endTransaction()
            }
        }

        fun getAlarmFailures(): List<AlarmFailureLog> {
            return Select()
                .from(AlarmFailureLog::class.java)
                .where("Failure")
                .execute()?:emptyList()
        }

        fun sendAlarmFailureLogs(): List<AlarmFailureLog> {
            getAlarmFailures().onEach { alarmFailure ->
                Crashlytics.log("Alarm Failure: \n" + Gson().toJson(alarmFailure))
                alarmFailure.delete()
            }.takeIf {
                it.isNotEmpty()
            }?.also {
                Crashlytics.logException(Throwable("Alarm Failure Report"))
                return@sendAlarmFailureLogs it
            }
            return emptyList()
        }
    }
}