/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roostermornings.android.activity.DeviceAlarmFullScreenActivity;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.receiver.DeviceAlarmReceiver;
import com.roostermornings.android.util.Constants;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.roostermornings.android.activity.base.BaseActivity.mCurrentUser;
import static com.roostermornings.android.util.RoosterUtils.hasLollipop;

/**
 * Created by bscholtz on 2/16/17.
 */


//Synchronises alarm manager's pending intents and SQL database
public final class DeviceAlarmController {

    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgr;

    private DeviceAlarmTableManager deviceAlarmTableManager;
    private Context context;

    public DeviceAlarmController(Context context) {
        this.context = context;
        deviceAlarmTableManager = new DeviceAlarmTableManager(context);
    }

    //Take a set of alarms and recreate intents, overwriting if already existing, and clearing changed flag if set
    public void refreshAlarms(List<DeviceAlarm> deviceAlarmList) {
        for (DeviceAlarm deviceAlarm : deviceAlarmList) {
            setAlarm(deviceAlarm);
        }
        deviceAlarmTableManager.clearChanged(deviceAlarmList);
    }

    //Create pending intent for individual alarm (done once for each alarm in set)
    private void setAlarm(DeviceAlarm deviceAlarm) {
        if (deviceAlarm.getEnabled()) {
            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar alarmCalendar = Calendar.getInstance();
            Calendar systemCalendar = Calendar.getInstance();

            Intent alarmIntent = new Intent(context, DeviceAlarmReceiver.class);
            alarmIntent.setAction(Constants.ACTTION_ALARMRECEIVER);
            alarmIntent.putExtra(Constants.EXTRA_REQUESTCODE, deviceAlarm.getPiId());
            alarmIntent.putExtra(Constants.EXTRA_UID, deviceAlarm.getSetId());

            if (deviceAlarm.getRecurring()) {
                alarmIntent.putExtra(Constants.EXTRA_RECURRING, true);
            }

            if (deviceAlarm.getVibrate()){
                alarmIntent.putExtra(Constants.EXTRA_VIBRATE, true);
            }

            PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context,
                    deviceAlarm.getPiId(), alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            alarmCalendar.set(alarmCalendar.get(Calendar.YEAR), alarmCalendar.get(Calendar.MONTH), alarmCalendar.get(Calendar.DATE), deviceAlarm.getHour(), deviceAlarm.getMinute());
            alarmCalendar.set(Calendar.WEEK_OF_MONTH, systemCalendar.get(Calendar.WEEK_OF_MONTH));
            alarmCalendar.set(Calendar.DAY_OF_WEEK, deviceAlarm.getDay());
            alarmCalendar.set(Calendar.SECOND, 0);
            alarmCalendar.set(Calendar.MILLISECOND, 0);

            long alarmTime = alarmCalendar.getTimeInMillis();
            long systemTime = systemCalendar.getTimeInMillis();

            //set alarm time to one week in future If specified day and time of alarm is already in the past
            int WEEK_OF_MONTH = alarmCalendar.get(Calendar.WEEK_OF_MONTH);
            while (alarmTime < systemTime) {
                WEEK_OF_MONTH += 1;
                alarmCalendar.set(Calendar.WEEK_OF_MONTH, WEEK_OF_MONTH);
                alarmTime = alarmCalendar.getTimeInMillis();
            }
            //store the alarm table with the alarm's new date and time
            deviceAlarmTableManager.updateAlarmMillis(deviceAlarm.getPiId(), alarmTime);

            //if newer version of Android, create info pending intent
            if (hasLollipop()) {
                Intent alarmInfoIntent = new Intent(context, DeviceAlarmFullScreenActivity.class);
                PendingIntent alarmInfoPendingIntent = PendingIntent.getActivity(context, 0, alarmInfoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                //required for setting alarm clock
                AlarmManager.AlarmClockInfo alarmInfo = new AlarmManager.AlarmClockInfo(alarmTime, alarmInfoPendingIntent);
                alarmMgr.setAlarmClock(alarmInfo, alarmPendingIntent);

            } else {
                //if older version of android, don't require info pending intent
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, alarmTime, alarmPendingIntent);
                // Show alarm in the status bar
                Intent alarmChanged = new Intent(Constants.ACTION_ALARMCHANGED);
                alarmChanged.putExtra(Constants.EXTRA_ALARMSET, true);
                context.sendBroadcast(alarmChanged);
            }
        }

    }

    public void snoozeAlarm(String setId){
        //Add new pending intent for 10 minutes time
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar alarmCalendar = Calendar.getInstance();
        alarmCalendar.setTimeInMillis(alarmCalendar.getTimeInMillis() + Constants.ALARM_SNOOZETIME);

        Intent alarmIntent = new Intent(context, DeviceAlarmReceiver.class);
        alarmIntent.setAction(Constants.ACTTION_ALARMRECEIVER);
        alarmIntent.putExtra(Constants.EXTRA_REQUESTCODE, 0);

        alarmIntent.putExtra(Constants.EXTRA_VIBRATE, true);
        alarmIntent.putExtra(Constants.EXTRA_TONE, false);
        alarmIntent.putExtra(Constants.EXTRA_UID, setId);

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context,
                0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        long alarmTime = alarmCalendar.getTimeInMillis();

        //if newer version of Android, create info pending intent
        if (hasLollipop()) {
            Intent alarmInfoIntent = new Intent(context, DeviceAlarmFullScreenActivity.class);
            PendingIntent alarmInfoPendingIntent = PendingIntent.getActivity(context, 0, alarmInfoIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            //required for setting alarm clock
            AlarmManager.AlarmClockInfo alarmInfo = new AlarmManager.AlarmClockInfo(alarmTime, alarmInfoPendingIntent);
            alarmMgr.setAlarmClock(alarmInfo, alarmPendingIntent);

        } else {
            //if older of android, don't require info pending intent
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, alarmTime, alarmPendingIntent);
            // Show alarm in the status bar
            Intent alarmChanged = new Intent(Constants.ACTION_ALARMCHANGED);
            alarmChanged.putExtra(Constants.EXTRA_ALARMSET, true);
            context.sendBroadcast(alarmChanged);
        }

    }

    public void registerAlarmSet(String setId, int alarmHour, int alarmMinute, List<Integer> alarmDays, boolean repeatWeekly, boolean vibrate, String channel, boolean social) {
        List<DeviceAlarm> deviceAlarmList;
        DeviceAlarm deviceAlarmSet = new DeviceAlarm()
                .initAlarmSet(alarmHour, alarmMinute, alarmDays, repeatWeekly, vibrate, channel, social);
        deviceAlarmList = deviceAlarmSet.getAlarmList();

        for (DeviceAlarm deviceAlarm :
                deviceAlarmList) {
            deviceAlarmTableManager.insertAlarm(deviceAlarm, setId);
        }

        //Update alarm millis and setAlarm
        refreshAlarms(deviceAlarmTableManager.selectChanged());
        //Notify user of time until next alarm, once alarm millis has been updated in db
        notifyUserAlarmTime(deviceAlarmTableManager.getAlarmSet(setId));
    }

    private void notifyUserAlarmTime(List<DeviceAlarm> deviceAlarmList) {
        Calendar alarmCalendar = Calendar.getInstance();
        Calendar systemCalendar = Calendar.getInstance();

        Long nextAlarmMillis;
        Long timeUntilNextAlarm;
        nextAlarmMillis = Long.MAX_VALUE;
        String nextAlarmTimeString;

        //Iterate over all alarms in SQL db for specific set, find next alarm time
        for (DeviceAlarm deviceAlarm :
                deviceAlarmList) {
            if(deviceAlarm.getMillis() < nextAlarmMillis) nextAlarmMillis = deviceAlarm.getMillis();
        }

        //Time until next alarm is the time of alarm, minus the current time in millis - alarm always set for future date
        alarmCalendar.setTimeInMillis(nextAlarmMillis);
        timeUntilNextAlarm = alarmCalendar.getTimeInMillis() - systemCalendar.getTimeInMillis();

        //Convert millis value to days,hours,mins until next alarm
        int minutes = (int) ((timeUntilNextAlarm / (1000*60)) % 60);
        int hours   = (int) ((timeUntilNextAlarm / (1000*60*60)) % 24);
        int days = (int) (timeUntilNextAlarm / (1000*60*60*24));

        //Notify user of time until next alarm
        if(days == 0 && hours != 0) nextAlarmTimeString = String.format("%s hours, and %s minutes", hours, minutes);
        else if(days == 0 && hours == 0) nextAlarmTimeString = String.format("%s minutes", minutes);
        else nextAlarmTimeString = String.format("%s days, %s hours, and %s minutes", days, hours, minutes);
        Toast.makeText(context, "Alarm set for " + nextAlarmTimeString + " from now.", Toast.LENGTH_LONG).show();
    }

    public void deleteAlarmsLocal() {
        List<DeviceAlarm> deviceAlarmList = deviceAlarmTableManager.getAlarmSets();
        if(deviceAlarmList == null) return;
        for (DeviceAlarm deviceAlarm:
                deviceAlarmList) {
            deleteAlarmSetLocal(deviceAlarm.getSetId());
        }
    }

    private void deleteAlarmSetLocal(String setId) {
        List<DeviceAlarm> deviceAlarmList = deviceAlarmTableManager.getAlarmSet(setId);
        for (DeviceAlarm deviceAlarm :
                deviceAlarmList) {
            cancelAlarm(deviceAlarm);
        }
    }

    //Remove entire set of alarms, first recreate intent EXACTLY as before, then call alarmMgr.cancel(intent)
    public void deleteAlarmSetGlobal(String setId) {
        List<DeviceAlarm> deviceAlarmList = deviceAlarmTableManager.getAlarmSet(setId);
        for (DeviceAlarm deviceAlarm :
                deviceAlarmList) {
            cancelAlarm(deviceAlarm);
        }
        removeSetChannelAudio(deviceAlarmList);
        removeFirebaseAlarm(setId);
        deviceAlarmTableManager.deleteAlarmSet(setId);
    }

    public void setSetEnabled(String setId, boolean enabled) {
        if(enabled) {
            //Set all intents for enabled alarms
            deviceAlarmTableManager.setSetEnabled(setId, enabled);
            deviceAlarmTableManager.setSetChanged(setId, true);
            refreshAlarms(deviceAlarmTableManager.selectChanged());
            updateFirebaseAlarmEnabled(setId, enabled);
        } else{
            List<DeviceAlarm> deviceAlarmList = deviceAlarmTableManager.getAlarmSet(setId);
            for (DeviceAlarm deviceAlarm :
                    deviceAlarmList) {
                cancelAlarm(deviceAlarm);
            }
            removeSetChannelAudio(deviceAlarmList);
            updateFirebaseAlarmEnabled(setId, enabled);
        }
    }

    public void removeFirebaseAlarm(String setId) {
        DatabaseReference alarmReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(mCurrentUser.getUid()).child(setId);
        alarmReference.removeValue();
    }

    public void updateFirebaseAlarmEnabled(String setId, boolean enabled) {
        DatabaseReference alarmReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(mCurrentUser.getUid()).child(setId);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(String.format("enabled"), enabled);

        alarmReference.updateChildren(childUpdates);
    }

    private void removeSetChannelAudio(List<DeviceAlarm> deviceAlarmList) {
        if (!deviceAlarmList.isEmpty()) {
            AudioTableManager audioTableManager = new AudioTableManager(context);
            String channelId = deviceAlarmList.get(0).getChannel();
            if (channelId != null) audioTableManager.removeChannelAudioEntry(channelId);
        }
    }

    private void cancelAlarm(DeviceAlarm deviceAlarm) {
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, DeviceAlarmReceiver.class);
        alarmIntent.setAction(Constants.ACTTION_ALARMRECEIVER);
        alarmIntent.putExtra(Constants.EXTRA_REQUESTCODE, deviceAlarm.getPiId());
        alarmIntent.putExtra(Constants.EXTRA_UID, deviceAlarm.getSetId());

        if (deviceAlarm.getRecurring()) {
            alarmIntent.putExtra(Constants.EXTRA_RECURRING, true);
        }
        if (deviceAlarm.getVibrate()){
            alarmIntent.putExtra(Constants.EXTRA_VIBRATE, true);
        }

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context,
                deviceAlarm.getPiId(), alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // If the alarm has been set, cancel it
        if (alarmMgr != null) {
             alarmMgr.cancel(alarmPendingIntent);
        }
    }

    //Used to recreate alarm intents after reboot. All ENABLED alarms recreated.
    public void rebootAlarms() {
        refreshAlarms(deviceAlarmTableManager.selectEnabled());
    }
}
