/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.widget.Toast

import com.roostermornings.android.BaseApplication
import com.roostermornings.android.BuildConfig
import com.roostermornings.android.keys.Action
import com.roostermornings.android.sqlutil.AudioTableManager
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.Toaster

import javax.inject.Inject

class BackgroundTaskReceiver : BroadcastReceiver() {

    @Inject lateinit var audioTableManager: AudioTableManager

    // The app's AlarmManager, which provides access to the system alarm services.
    private var alarmMgrBackgroundTask: AlarmManager? = null

    init {
        BaseApplication.roosterApplicationComponent.inject(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Log.d("Background Message:", "BackgroundTaskReceiver")
        if (BuildConfig.DEBUG) Toaster.makeToast(context, "BackgroundTaskReceiver!", Toast.LENGTH_LONG)

        when (intent.action) {
            Action.DAILY_BACKGROUND_TASK.name -> {
                //Purge channel audio files that are stagnant: 1 week or older and not present in alarm set
                audioTableManager.purgeStagnantChannelAudio()
                //Purge social audio files that are stagnant: 1 day or older and not favourite
                audioTableManager.purgeStagnantSocialAudio()
            }
            else -> {
            }
        }
    }

    fun scheduleBackgroundDailyTask(context: Context, start: Boolean) {
        alarmMgrBackgroundTask = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BackgroundTaskReceiver::class.java)
        intent.action = Action.DAILY_BACKGROUND_TASK.name
        //starts a inexact repeating background task that runs every day
        //the task runs the 'dailyTasks' method in BackgroundTaskIntentService
        val backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        if (start)
            alarmMgrBackgroundTask?.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10 * 1000,
                    Constants.TIME_MILLIS_1_DAY, backgroundIntent)
        else
            alarmMgrBackgroundTask?.cancel(backgroundIntent)
    }
}
