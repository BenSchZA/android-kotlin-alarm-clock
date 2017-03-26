/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.roostermornings.android.service.BackgroundTaskIntentService;
import com.roostermornings.android.service.FirebaseListenerService;
import com.roostermornings.android.util.Constants;

public class BackgroundTaskReceiver extends BroadcastReceiver {

    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgrBackgroundTask;

    public BackgroundTaskReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Log.d("Background Message:", "BackgroundTaskReceiver");
        Toast.makeText(context, "BackgroundTaskReceiver!", Toast.LENGTH_LONG).show();

        Intent intentService = new Intent(context, BackgroundTaskIntentService.class);
        intentService.setAction(intent.getAction());
        context.startService(intentService);
    }

    public void scheduleBackgroundCacheFirebaseData(Context context) {
        alarmMgrBackgroundTask = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundTaskReceiver.class);
        intent.setAction(Constants.ACTION_BACKGROUNDDOWNLOAD);
        //starts a inexact repeating background task that runs every 10 seconds
        //the task runs the 'retrieveFirebaseData' method in BackgroundTaskIntentService
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10 * 1000,
                1 * 60 * 1000, backgroundIntent);
    }

    public void scheduleBackgroundDailyTask(Context context) {
        alarmMgrBackgroundTask = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundTaskReceiver.class);
        intent.setAction(Constants.ACTION_DAILYTASK);
        //starts a inexact repeating background task that runs every day
        //the task runs the 'dailyTasks' method in BackgroundTaskIntentService
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10 * 1000,
                86400 * 1000, backgroundIntent);
    }

    public void scheduleBackgroundUpdateNotificationsTask(Context context) {
        alarmMgrBackgroundTask = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundTaskReceiver.class);
        intent.setAction(Constants.ACTION_MINUTETASK);
        //starts a inexact repeating background task that runs every day
        //the task runs the 'dailyTasks' method in BackgroundTaskIntentService
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 2 * 1000,
                60*1000, backgroundIntent);
    }
}
