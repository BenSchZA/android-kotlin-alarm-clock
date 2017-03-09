package com.roostermornings.android.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

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
        intent.setAction("com.roostermornings.android.background.action.BACKGROUND_DOWNLOAD");
        //starts a inexact repeating background task that runs every 10 seconds
        //the task runs the 'retrieveFirebaseData' method in BackgroundTaskIntentService
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10 * 1000,
                120 * 1000, backgroundIntent);
    }

    public void scheduleBackgroundDailyTask(Context context) {
        alarmMgrBackgroundTask = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundTaskReceiver.class);
        intent.setAction("com.roostermornings.android.background.action.DAILY_TASK");
        //starts a inexact repeating background task that runs every day
        //the task runs the 'dailyTasks' method in BackgroundTaskIntentService
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 86400 * 1000,
                86400 * 1000, backgroundIntent);
    }
}
