package com.roostermornings.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.roostermornings.android.background.BackgroundTaskReceiver;
import com.roostermornings.android.sqlutil.DeviceAlarmController;


/**
 * This BroadcastReceiver automatically (re)starts the alarm when the device is
 * rebooted. This receiver is set to be disabled (android:enabled="false") in the
 * application's manifest file. When the user sets the alarm, the receiver is enabled.
 * When the user cancels the alarm, the receiver is disabled, so that rebooting the
 * device will not trigger this receiver.
 */
// BEGIN_INCLUDE(autostart)
public class BootReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        //TODO: set up correctly
        Toast.makeText(context, "BootReceiver!", Toast.LENGTH_LONG).show();

        this.context = context;

        DeviceAlarmController alarmController = new DeviceAlarmController(context);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        BackgroundTaskReceiver backgroundTaskReceiver = new BackgroundTaskReceiver();

        alarmController.rebootAlarms();
        if (auth != null && auth.getCurrentUser().getUid() != null) {
            Toast.makeText(context, "Tasks started, valid user!", Toast.LENGTH_LONG).show();
            alarmController.rebootAlarms();
            backgroundTaskReceiver.startBackgroundTask(context);
        } else {
            Toast.makeText(context, "Tasks not started, invalid user!", Toast.LENGTH_LONG).show();
        }
    }
}
