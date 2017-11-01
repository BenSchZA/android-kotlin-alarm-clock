package com.roostermornings.android.logging

import com.activeandroid.Model
import com.activeandroid.annotation.Column
import com.activeandroid.annotation.Table
import com.activeandroid.query.Select

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
        // User dismissed, snoozed, or clicked next on alarm
        @Column(name="Interaction")
        var interaction: Boolean = false,
        // Activation activity visible
        @Column(name="Seen")
        var seen: Boolean = false,
        // Audio successfully started
        @Column(name="Heard")
        var heard: Boolean = false,
        // Alarm defined as failure
        @Column(name="Failure", index = true)
        var failure: Boolean = false): Model() {

    //Failure = (currentTime > scheduledTime) && (!seen || !heard)

    fun updateOrCreateAlarmFailureLogEntry() {
        getAlarmFailureLogByPIID(this.pendingIntentID)?.delete()
        this.save()
    }

    companion object {
        fun getAlarmFailureLogByPIID(pendingIntentID: Int): AlarmFailureLog? {
            return Select()
                    .from(AlarmFailureLog::class.java)
                    .where("PIID = ?", pendingIntentID)
                    .executeSingle()
        }

        fun updateOrCreateAlarmFailureLogEntry(alarmFailureLog: AlarmFailureLog) {
            getAlarmFailureLogByPIID(alarmFailureLog.pendingIntentID)?.delete()
            alarmFailureLog.save()
        }

        fun tryDeleteAlarmFailureLogByPIID(pendingIntentID: Int) {
            getAlarmFailureLogByPIID(pendingIntentID)?.delete()
        }
    }
}