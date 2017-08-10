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

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.Toaster;

import javax.inject.Inject;

public class BackgroundTaskReceiver extends BroadcastReceiver {

    @Inject AudioTableManager audioTableManager;

    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgrBackgroundTask;

    public BackgroundTaskReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        BaseApplication.getRoosterApplicationComponent().inject(this);

        Log.d("Background Message:", "BackgroundTaskReceiver");
        if(BuildConfig.DEBUG) Toaster.makeToast(context, "BackgroundTaskReceiver!", Toast.LENGTH_LONG);

        switch (intent.getAction()) {
            case Constants.ACTION_DAILYTASK:
                //Purge channel audio files that are stagnant: 1 week or older and not present in alarm set
                audioTableManager.purgeStagnantChannelAudio();
                //Purge social audio files that are stagnant: 1 day or older and not favourite
                audioTableManager.purgeStagnantSocialAudio();
                break;
            default:
                break;
        }
    }

    public void scheduleBackgroundDailyTask(Context context, Boolean start) {
        alarmMgrBackgroundTask = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundTaskReceiver.class);
        intent.setAction(Constants.ACTION_DAILYTASK);
        //starts a inexact repeating background task that runs every day
        //the task runs the 'dailyTasks' method in BackgroundTaskIntentService
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        if(start) alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10 * 1000,
                Constants.TIME_MILLIS_1_DAY, backgroundIntent);
        else alarmMgrBackgroundTask.cancel(backgroundIntent);
    }
}
