package com.roostermornings.android.realm;

import com.google.firebase.database.Exclude;
import com.google.gson.annotations.Expose;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import kotlin.jvm.JvmField;

/**
 * Created by bscholtz on 2017/11/04.
 */

public class AlarmFailureLog extends RealmObject {

    // Pending intent ID
    @Expose
    public Integer pendingIntentID = -1;
    // Firebase UID of alarm
    @Expose
    public String alarmUid = "";
    // Time of scheduled alarm
    @Expose
    @PrimaryKey
    public Long scheduledTime = -1L;
    // Pending intent triggered
    @Expose
    public Long firedTime = -1L;
    @Expose
    public boolean fired = false;
    // Activation service started
    @Expose
    public boolean activated = false;
    // Activation service running
    @Expose
    public boolean running = false;
    // User dismissed, snoozed, or clicked next on alarm
    @Expose
    public boolean interaction = false;
    // Activation activity visible
    @Expose
    public boolean seen = false;
    // Audio successfully started
    @Expose
    public boolean heard = false;
    // Audio default received
    @Expose
    public boolean def = false;
    // Audio fail-safe received
    @Expose
    public boolean failsafe = false;
    // Audio content received
    @Expose
    public boolean content = false;
    // Audio content selected
    @Expose
    public boolean channel = false;
    // Audio content stream attempted
    @Expose
    public boolean stream = false;
    // Internet connection available
    @Expose
    public boolean internet = false;
    // Alarm defined as failure
    @Expose
    public boolean failure = false;

    // A failure can be defined by the following failure modes, and is implemented in a
    // RealmManager_AlarmFailureLog query.
    // Failure = FailureMode0 && (FailureMode1 || FailureMode2)
    // FailureMode0 = (currentTime > scheduledTime) && !(fired & activated & !running)
    // FailureMode1 = (!fired  || !activated || !running || !seen || !heard || failsafe || stream)
    // FailureMode2 = default && (content || channel) || channel && !content
    // What we want to feed back to the user:
    // * Alarm content didn't download because of internet connection
    // as a subset of above: not to worry, alarm default occurred, or streamed if internet access present at time of activation
    // * Alarm intent didn't fire, possibly because of not being whitelisted (can cross reference with phone model)

    public Long getFiredTime() {
        return firedTime;
    }

    public void setFiredTime(Long firedTime) {
        this.firedTime = firedTime;
    }

    public boolean isChannel() {
        return channel;
    }

    public void setChannel(boolean channel) {
        this.channel = channel;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean isInternet() {
        return internet;
    }

    public void setInternet(boolean internet) {
        this.internet = internet;
    }

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
