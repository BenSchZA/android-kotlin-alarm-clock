/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.roostermornings.android.sqlutil.DeviceAlarm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by steven on 2017/02/15.
 */

@IgnoreExtraProperties
public class Alarm {

    private boolean enabled;
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

    @Exclude
    private Integer unseen_roosters = -1;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public Alarm() {
    }

    public Alarm(AlarmChannel channel, boolean monday, boolean tuesday, boolean wednesday,
                 boolean thursday, boolean friday, boolean saturday, boolean
                         sunday, int hour, int minute, boolean recurring, boolean allow_friend_audio_files, String uid) {
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

        this.uid = uid;

        this.unseen_roosters = 0;
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Exclude //NB: exclude needs to be used even for methods, so that Firebase doesn't include in db object mapping
    public List<Integer> getDays() {
        List<Integer> alarmDays = new ArrayList<>();
        if (isMonday()) alarmDays.add(Calendar.MONDAY);
        if (isTuesday()) alarmDays.add(Calendar.TUESDAY);
        if (isWednesday()) alarmDays.add(Calendar.WEDNESDAY);
        if (isThursday()) alarmDays.add(Calendar.THURSDAY);
        if (isFriday()) alarmDays.add(Calendar.FRIDAY);
        if (isSaturday()) alarmDays.add(Calendar.SATURDAY);
        if (isSunday()) alarmDays.add(Calendar.SUNDAY);
        return alarmDays;
    }

    @Exclude
    public void fromDeviceAlarm(DeviceAlarm deviceAlarm, boolean alarmSetEnabled) {
        this.enabled = alarmSetEnabled;

        if(deviceAlarm.getChannel() != null) {
            this.channel = new AlarmChannel("", deviceAlarm.getChannel());
        }
        this.hour = deviceAlarm.getHour();
        this.minute = deviceAlarm.getMinute();
        this.recurring = deviceAlarm.getRecurring();
        this.allow_friend_audio_files = deviceAlarm.isSocial();
        this.uid = deviceAlarm.getSetId();
        this.unseen_roosters = 0;
    }

    @Exclude
    public void setAlarmDayFromDeviceAlarm(List<Integer> alarmDays) {
        for (Integer day:
                alarmDays) {
            switch (day) {
                case Calendar.SUNDAY:
                    this.setSunday(true);
                    break;
                case Calendar.MONDAY:
                    this.setMonday(true);
                    break;
                case Calendar.TUESDAY:
                    this.setTuesday(true);
                    break;
                case Calendar.WEDNESDAY:
                    this.setWednesday(true);
                    break;
                case Calendar.THURSDAY:
                    this.setThursday(true);
                    break;
                case Calendar.FRIDAY:
                    this.setFriday(true);
                    break;
                case Calendar.SATURDAY:
                    this.setSaturday(true);
                    break;
            }
        }
    }

    @Exclude
    public void setAlarmDayFromCalendar(Calendar alarmTime) {
        switch (alarmTime.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                this.setSunday(true);
                break;
            case Calendar.MONDAY:
                this.setMonday(true);
                break;
            case Calendar.TUESDAY:
                this.setTuesday(true);
                break;
            case Calendar.WEDNESDAY:
                this.setWednesday(true);
                break;
            case Calendar.THURSDAY:
                this.setThursday(true);
                break;
            case Calendar.FRIDAY:
                this.setFriday(true);
                break;
            case Calendar.SATURDAY:
                this.setSaturday(true);
                break;
        }
    }

    public Integer getUnseen_roosters() {
        return unseen_roosters;
    }

    public void setUnseen_roosters(Integer unseen_roosters) {
        this.unseen_roosters = unseen_roosters;
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
}



