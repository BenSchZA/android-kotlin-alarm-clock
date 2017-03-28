/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.sqlutil.DeviceAlarmController;


/**
 * This BroadcastReceiver automatically (re)starts the alarm when the device is
 * rebooted.
 */
public class BootReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        //TODO: set up correctly
        if(BuildConfig.DEBUG) Toast.makeText(context, "BootReceiver!", Toast.LENGTH_LONG).show();

        this.context = context;

        DeviceAlarmController alarmController = new DeviceAlarmController(context);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        BackgroundTaskReceiver backgroundTaskReceiver = new BackgroundTaskReceiver();

        alarmController.rebootAlarms();
        if (auth != null && auth.getCurrentUser().getUid() != null) {
            if(BuildConfig.DEBUG) Toast.makeText(context, "Tasks started, valid user!", Toast.LENGTH_LONG).show();
            //TODO: check for authentication
//            alarmController.rebootAlarms();
//            backgroundTaskReceiver.scheduleBackgroundCacheFirebaseData(context);
//            backgroundTaskReceiver.scheduleBackgroundDailyTask(context);
        } else {
            if(BuildConfig.DEBUG) Toast.makeText(context, "Tasks not started, invalid user!", Toast.LENGTH_LONG).show();
        }
    }
}
