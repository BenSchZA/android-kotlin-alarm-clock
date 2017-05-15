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
import com.roostermornings.android.util.Toaster;


/**
 * This BroadcastReceiver automatically (re)starts the alarm when the device is
 * rebooted.
 */
public class BootReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(BuildConfig.DEBUG) Toaster.makeToast(context, "BootReceiver!", Toast.LENGTH_LONG);

        this.context = context;

        DeviceAlarmController alarmController = new DeviceAlarmController(context);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        BackgroundTaskReceiver backgroundTaskReceiver = new BackgroundTaskReceiver();

        if (auth != null && auth.getCurrentUser().getUid() != null) {
            if(BuildConfig.DEBUG) Toaster.makeToast(context, "Tasks started, valid user!", Toast.LENGTH_LONG);
            alarmController.rebootAlarms();
            backgroundTaskReceiver.scheduleBackgroundDailyTask(context, true);
        } else {
            if(BuildConfig.DEBUG) Toaster.makeToast(context, "Tasks not started, invalid user!", Toast.LENGTH_LONG);
        }
    }
}
