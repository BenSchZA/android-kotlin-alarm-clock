package com.roostermornings.android.logging;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

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
    public boolean fired = false;
    // Activation service started
    public boolean activated = false;
    // Activation service running
    public boolean running = false;
    // User dismissed, snoozed, or clicked next on alarm
    public boolean interaction = false;
    // Activation activity visible
    public boolean seen = false;
    // Audio successfully started
    public boolean heard = false;
    // Audio default received
    public boolean def = false;
    // Audio fail-safe received
    public boolean failsafe = false;
    // Audio content received
    public boolean content = false;
    // Alarm defined as failure
    public boolean failure = false;

    // A failure can be defined by the following failure modes, and is implemented in a
    // RealmManager query.
    // Failure = FailureMode0 && (FailureMode1 || FailureMode2)
    // FailureMode0 = (currentTime > scheduledTime) && !(activated & !running)
    // FailureMode1 = (!seen || !heard)
    // FailureMode2 = content && (default || failsafe || !heard)


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

    public boolean isFired() {
        return fired;
    }

    public void setFired(boolean fired) {
        this.fired = fired;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isInteraction() {
        return interaction;
    }

    public void setInteraction(boolean interaction) {
        this.interaction = interaction;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public boolean isHeard() {
        return heard;
    }

    public void setHeard(boolean heard) {
        this.heard = heard;
    }

    public boolean isDef() {
        return def;
    }

    public void setDef(boolean def) {
        this.def = def;
    }

    public boolean isFailsafe() {
        return failsafe;
    }

    public void setFailsafe(boolean failsafe) {
        this.failsafe = failsafe;
    }

    public boolean isContent() {
        return content;
    }

    public void setContent(boolean content) {
        this.content = content;
    }

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }
}
