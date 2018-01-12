/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bscholtz on 2/13/17.
 */

public class DeviceAlarm {

    //##################################################################
    private String setId = "";
    private int piId = -1;

    private int hour = -1;
    private int minute = -1;
    private int day = -1;
    private boolean recurring = false;
    private boolean social = true;
    private long alarmMillis = -1;

    private String channel = "";
    private String label = "";
    private boolean enabled = true;
    private boolean changed = true;

    private String dateCreated;
//##################################################################

    private List<Integer> alarmDays = new ArrayList<>();

    private List<DeviceAlarm> deviceAlarmSet = new ArrayList<>();

    public DeviceAlarm() {
        super();
    }

//Usage:
/*   Alarm alarmSet = new Alarm()
    .initAlarmSet(int hour, int minute, List alarmDays, boolean repeatWeekly);
    alarmList = alarmSet.getAlarmList();*/


    public DeviceAlarm initAlarmSet(boolean enabled, int hour, int minute, List<Integer> alarmDays, boolean repeatWeekly, String channel, boolean allowSocialRoosters) {
        this.enabled = enabled;
        this.hour = hour;
        this.minute = minute;
        this.alarmDays = alarmDays;
        this.recurring = repeatWeekly;
        this.channel = channel;
        this.social = allowSocialRoosters;
        return this;
    }

    public List<DeviceAlarm> getAlarmList() {

        for (Object alarmDay : alarmDays) {
            day = Integer.valueOf(alarmDay.toString());
            deviceAlarmSet.add(new DeviceAlarm().setAlarm(this.enabled, this.hour, this.minute, day, this.recurring, this.channel, this.social));
        }
        return deviceAlarmSet;
    }

    private DeviceAlarm setAlarm(boolean enabled, int hour, int minute, int day, boolean repeatWeekly, String channel, boolean social) {
        this.enabled = enabled;
        this.hour = hour;
        this.minute = minute;
        this.day = day;
        this.recurring = repeatWeekly;
        this.alarmMillis = -1;
        this.channel = channel;
        this.social = social;

        return this;
    }

    public boolean isSocial() {
        return social;
    }

    public void setSocial(boolean social) {
        this.social = social;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String id) {
        this.setId = id;
    }

    public int getPiId() {
        return piId;
    }

    public void setPiId(int id) {
        this.piId = id;
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

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public long getMillis() {
        return alarmMillis;
    }

    public void setMillis(long alarmMillis) {
        this.alarmMillis = alarmMillis;
    }

    public boolean getRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setChanged(boolean b) {
        this.changed = b;
    }

    public boolean getChanged() {
        return changed;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDateCreated() {
        return dateCreated;
    }
}
