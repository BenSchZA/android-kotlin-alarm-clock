package com.roostermornings.android.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import kotlin.jvm.JvmField;

/**
 * Created by bscholtz on 2017/11/04.
 */

public class AlarmFailureLog extends RealmObject {

    // Pending intent ID
    public Integer pendingIntentID = -1;
    // Firebase UID of alarm
    public String alarmUid = "";
    // Time of scheduled alarm
    @PrimaryKey
    public Long scheduledTime = -1L;
    // Pending intent triggered
    public Boolean fired = false;
    // Activation service started
    public Boolean activated = false;
    // Activation service running
    public Boolean running = false;
    // User dismissed, snoozed, or clicked next on alarm
    public Boolean interaction = false;
    // Activation activity visible
    public Boolean seen = false;
    // Audio successfully started
    public Boolean heard = false;
    // Audio default received
    public Boolean def = false;
    // Audio fail-safe received
    public Boolean failsafe = false;
    // Audio content received
    public Boolean content = false;
    // Alarm defined as failure
    public Boolean failure = false;

    // A failure can be defined by the following failure modes, and is implemented in a
    // RealmManager_AlarmFailureLog query.
    // Failure = FailureMode0 && (FailureMode1 || FailureMode2)
    // FailureMode0 = (currentTime > scheduledTime) && !(activated & !running)
    // FailureMode1 = (!seen || !heard)
    // FailureMode2 = content && (default || !heard)
    // FailureMode3 = failsafe

    public Integer getPendingIntentID() {
        return pendingIntentID;
    }

    public void setPendingIntentID(Integer pendingIntentID) {
        this.pendingIntentID = pendingIntentID;
    }

    public String getAlarmUid() {
        return alarmUid;
    }

    public void setAlarmUid(String alarmUid) {
        this.alarmUid = alarmUid;
    }

    public Long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Boolean isFired() {
        return fired;
    }

    public void setFired(Boolean fired) {
        this.fired = fired;
    }

    public Boolean isActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }

    public Boolean isRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }

    public Boolean isInteraction() {
        return interaction;
    }

    public void setInteraction(Boolean interaction) {
        this.interaction = interaction;
    }

    public Boolean isSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public Boolean isHeard() {
        return heard;
    }

    public void setHeard(Boolean heard) {
        this.heard = heard;
    }

    public Boolean isDef() {
        return def;
    }

    public void setDef(Boolean def) {
        this.def = def;
    }

    public Boolean isFailsafe() {
        return failsafe;
    }

    public void setFailsafe(Boolean failsafe) {
        this.failsafe = failsafe;
    }

    public Boolean isContent() {
        return content;
    }

    public void setContent(Boolean content) {
        this.content = content;
    }

    public Boolean isFailure() {
        return failure;
    }

    public void setFailure(Boolean failure) {
        this.failure = failure;
    }
}
