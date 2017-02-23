package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by steven on 2017/02/15.
 */

@IgnoreExtraProperties
public class Alarm {

    private AlarmChannel channel;
    private boolean monday;
    private boolean tuesday;
    private boolean wednesday;
    private boolean thursday;
    private boolean friday;
    private boolean saturday;
    private boolean sunday;
    private int hour;
    private int minute;
    private boolean recurring;
    private boolean allow_friend_audio_files;
    private String uid;

    private boolean vibrate;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public Alarm() {
    }

    public Alarm(AlarmChannel channel, boolean monday, boolean tuesday, boolean wednesday,
                 boolean thursday, boolean friday, boolean saturday, boolean
                         sunday, int hour, int minute, boolean recurring, boolean allow_friend_audio_files, boolean vibrate, String uid) {
        this.channel = channel;
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
        this.hour = hour;
        this.minute = minute;
        this.recurring = recurring;
        this.allow_friend_audio_files = allow_friend_audio_files;

        this.vibrate = vibrate;
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isAllow_friend_audio_files() {
        return allow_friend_audio_files;
    }

    public void setAllow_friend_audio_files(boolean allow_friend_audio_files) {
        this.allow_friend_audio_files = allow_friend_audio_files;
    }

    public AlarmChannel getChannel() {
        return channel;
    }

    public void setChannel(AlarmChannel channel) {
        this.channel = channel;
    }

    public boolean isMonday() {
        return monday;
    }

    public void setMonday(boolean monday) {
        this.monday = monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public void setTuesday(boolean tuesday) {
        this.tuesday = tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public void setWednesday(boolean wednesday) {
        this.wednesday = wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public void setThursday(boolean thursday) {
        this.thursday = thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public void setFriday(boolean friday) {
        this.friday = friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public void setSaturday(boolean saturday) {
        this.saturday = saturday;
    }

    public boolean isSunday() {
        return sunday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }
}



