package com.roostermornings.android.receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.roostermornings.android.activity.DeviceAlarmFullScreenActivity;
import com.roostermornings.android.sqldata.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class DeviceAlarmReceiver extends WakefulBroadcastReceiver {

    private Context context;
    private DeviceAlarmController alarmController;
    private DeviceAlarmTableManager alarmTableManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        //instatiate new alarm controller
        //alarm controller provides interface for synchronising pending intents with SQLLite database on device
        alarmController = new DeviceAlarmController(context);

        //instantiate new alarm table manager
        //this interfaces directly with database
        alarmTableManager = new DeviceAlarmTableManager(context);

        Intent intentAlarmFullscreen = new Intent(context, DeviceAlarmFullScreenActivity.class);
        intentAlarmFullscreen.addFlags(FLAG_ACTIVITY_NEW_TASK);

        //if this is a recurring alarm, set anoter pending intent for next week same time
        if (intent.getBooleanExtra(DeviceAlarm.EXTRA_RECURRING, false)) {

            //make record that this alarm has been changed, refresh as necessary
            alarmTableManager.setAlarmChanged(intent.getIntExtra("requestCode", 0));
            alarmController.refreshAlarms(alarmTableManager.selectChanged());

        } else {
            //TODO: check the current alarm set; if all alarms have fired, set all alarms to disables
        }

        context.startActivity(intentAlarmFullscreen);
        DeviceAlarmReceiver.completeWakefulIntent(intent);

    }
}
