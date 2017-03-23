/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bscholtz on 2/13/17.
 */

public class DeviceAlarm {

    //##################################################################
    private String setId;
    private int piId;

    private int hour;
    private int minute;
    private int day;
    private boolean recurring;
    private long alarmMillis;

    private String channel;
    private String label;
    private int ringtone;
    private boolean vibrate;
    private boolean enabled;
    private boolean changed;

    private String dateCreated;
//##################################################################

    private Calendar calendar = Calendar.getInstance();

    private List<Integer> alarmDays = new ArrayList<>();

    private List<DeviceAlarm> deviceAlarmSet = new ArrayList<>();

    public DeviceAlarm() {
        super();
    }

//Usage:
/*   Alarm alarmSet = new Alarm()
    .initAlarmSet(int hour, int minute, List alarmDays, boolean repeatWeekly);
    alarmList = alarmSet.getAlarmList();*/


    public DeviceAlarm initAlarmSet(int hour, int minute, List<Integer> alarmDays, boolean repeatWeekly, boolean vibrate, String channel) {
        this.hour = hour;
        this.minute = minute;
        this.alarmDays = alarmDays;
        this.recurring = repeatWeekly;
        this.vibrate = vibrate;
        this.channel = channel;
        return this;
    }

    public List<DeviceAlarm> getAlarmList() {
        Iterator dayIterator = alarmDays.iterator();

        while (dayIterator.hasNext()) {
            day = Integer.valueOf(dayIterator.next().toString());
            deviceAlarmSet.add(new DeviceAlarm().setAlarm(this.hour, this.minute, day, this.recurring, this.vibrate, this.channel));
        }
        return deviceAlarmSet;
    }

    private DeviceAlarm setAlarm(int hour, int minute, int day, boolean repeatWeekly, boolean vibrate, String channel) {
        this.hour = hour;
        this.minute = minute;
        this.day = day;
        this.recurring = repeatWeekly;
        this.vibrate = vibrate;
        this.alarmMillis = -1;
        this.channel = channel;

        return this;
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

    public int getRingtone() {
        return this.ringtone;
    }

    public void setRingtone(int ringtone) {
        this.ringtone = ringtone;
    }

    public boolean getVibrate() {
        return this.vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
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

//    private Context c;
//
//    private AlarmManager alarmMgr;
//
//    private Intent alarmIntent = new Intent(c, AlarmFullscreenActivity.class);
//
//    //##################################################
//    //Alarm JSON Object
//    JSONObject alarmJSON = new JSONObject();
//    //Alarm Set ID (i.e. set of alarms via repeat weekly etc.)
//    private int alarmSetID = -1;
//    //Alarm JSON Elements
//    private long alarmID;
//
//    private long alarmTimePicker;
//
//    private boolean repeatWeekly = false;
//    private boolean alarmEnable = false;
//    //##################################################
//
//    private List<Integer> alarmDays;
//    private Iterator dayIterator;
//
//    private int alarmDay;
//
//    public void registerAlarm(Long alarmTimePicker, List alarmDays, boolean repeatWeekly, boolean alarmEnable){
//
//        SharedPreferences sharedPref = c.getSharedPreferences(String.valueOf(alarmSetID), Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//
//        dayIterator = alarmDays.iterator();
//
//        while(dayIterator.hasNext()) {
//            alarmDay = Integer.parseInt(dayIterator.next().toString());
//            alarmTime = getAlarmMillis(alarmDay, alarmTimePicker);
//
//            try {
//                //Each unique alarm in set receives an ID
//                alarmID = alarmTime;
//                editor.putString(String.valueOf(alarmID), alarmJSON.toString());
//                //Set JSON element values
//                alarmJSON.put("alarmTime", alarmTime);
//                alarmJSON.put("alarmTimePicker", alarmTimePicker);
//                alarmJSON.put("repeatWeekly", repeatWeekly);
//                alarmJSON.put("alarmEnable", alarmEnable);
//
//                editor.apply();
//
//                //Create pending intents, one for each unique alarm in set
//                setAlarmPendingIntent(alarmID, alarmTime);
//                addAlarmToList(alarmSetID);
//
//            } catch (JSONException e) {
//                Log.e("JSONException Error:", e.toString());
//            }
//        }
//    }
//
//    private void setAlarmPendingIntent(long alarmID, long alarmTime){
//        alarmMgr = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
//        int ALARM_TYPE = AlarmManager.RTC_WAKEUP;
//
//        PendingIntent alarmPendingIntent = PendingIntent.getActivity(c, (int)alarmID, alarmIntent, 0);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                Intent alarmInfoIntent = new Intent(c, LoginActivity.class);
//                PendingIntent alarmInfoPendingIntent = PendingIntent.getActivity(c, 0, alarmInfoIntent, FLAG_CANCEL_CURRENT);
//
//                AlarmManager.AlarmClockInfo alarmInfo = new AlarmManager.AlarmClockInfo(alarmTime, alarmInfoPendingIntent);
//                alarmMgr.setAlarmClock(alarmInfo, alarmPendingIntent);
//
//        } else {
//                alarmMgr.set(ALARM_TYPE, alarmTime, alarmPendingIntent);
//        }
//    }
//
//    private Long getAlarmMillis(int alarmDay, Long alarmTimePicker){
//        Calendar calendar = Calendar.getInstance();
//
//        calendar.setTimeInMillis(alarmTimePicker);
//        calendar.set(Calendar.SECOND, 0);
//        calendar.set(Calendar.MILLISECOND, 0);
//        calendar.set(Calendar.DAY_OF_WEEK, alarmDay);
//
//        return calendar.getTimeInMillis();
//    }
//
//    public void rebootAlarms(){
//        SharedPreferences sharedPref = c.getSharedPreferences(String.valueOf(0), Context.MODE_PRIVATE);
//
//    }
//
//    public void rescheduleAlarm(){
//
//    }
//
//    public void registerRepeatWeekly(){
//
//    }
//
//    public void enableAlarm(int alarmSetID){
//
//        SharedPreferences sharedPref = c.getSharedPreferences(String.valueOf(alarmSetID), Context.MODE_PRIVATE);
//
//        Map<String, ?> allEntries = sharedPref.getAll();
//        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//
//            alarmID = Long.parseLong(entry.getKey());
//            try {
//                JSONObject alarmJSON = new JSONObject(entry.getValue().toString());
//
//                alarmEnable = alarmJSON.getBoolean("alarmEnable");
//            }
//            //TODO: configure JSON exceptions correctly
//            catch (JSONException e){
//                Log.e("JSONException Error:", e.toString());
//            }
//        }
//    }
//
//    public void disableAlarm(int alarmSetID){
//        SharedPreferences sharedPref = c.getSharedPreferences(String.valueOf(alarmSetID), Context.MODE_PRIVATE);
//
//        Map<String, ?> allEntries = sharedPref.getAll();
//        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//
//            alarmID = Long.parseLong(entry.getKey());
//            if (alarmMgr!= null) {
//                alarmMgr.cancel(PendingIntent.getActivity(c, (int)alarmID, alarmIntent, 0));
//            }
//
//        }
//
//    }
//
//    public void changeAlarm(int alarmSetID){
//        SharedPreferences sharedPref = c.getSharedPreferences(String.valueOf(alarmSetID), Context.MODE_PRIVATE);
//    }
//
//    //Remove alarm from SharedPreferences and cancel PendingIntent
//    public void deleteAlarm(int alarmSetID) {
//        SharedPreferences sharedPref = c.getSharedPreferences(String.valueOf(alarmSetID), Context.MODE_PRIVATE);
//
//        Map<String, ?> allEntries = sharedPref.getAll();
//        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//
//                alarmID = Long.parseLong(entry.getKey());
//                if (alarmMgr!= null) {
//                        alarmMgr.cancel(PendingIntent.getActivity(c, (int)alarmID, alarmIntent, 0));
//                }
//        }
//
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.remove(String.valueOf(alarmSetID));
//        editor.apply();
//    }
//
//    public void retreiveAlarmJSON(){
//    }
//
//    private void addAlarmToList(int alarmSetID){
//        SharedPreferences sharedPref = c.getSharedPreferences("alarmList", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//
//        editor.putInt(String.valueOf(alarmSetID), alarmSetID);
//        editor.apply();
//    }
//
//    private void getNextAlarmSetID() {
//        SharedPreferences sharedPref = c.getSharedPreferences("alarmList", Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();
//
//        Map<String, ?> allEntries = sharedPref.getAll();
//        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
//            Integer.parseInt(entry.getValue().toString());
//        }
//    }
//
//    public void validateAlarm(){
//    }
}
