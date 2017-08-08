/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.database.DatabaseReference;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.util.Constants;

public class BackgroundTaskIntentService extends IntentService {
    public static final String TAG = BackgroundTaskIntentService.class.getSimpleName();
    private static final String EXTRA_PARAM1 = "com.roostermornings.android.background.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.roostermornings.android.background.extra.PARAM2";
    //Firebase libraries
    protected DatabaseReference mDatabase;

    private final AudioTableManager audioTableManager = new AudioTableManager(this);

    public BackgroundTaskIntentService() {
        super("BackgroundTaskIntentService");
    }

    public static void startActionDailyTask(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BackgroundTaskIntentService.class);
        intent.setAction(Constants.ACTION_DAILYTASK);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Constants.ACTION_DAILYTASK.equals(action)) {
                handleActionDailyTask();
            }
        }
    }

    private void handleActionDailyTask() {
        //Purge social audio files from SQL db that are older than two weeks
        audioTableManager.purgeSocialAudioFiles();
        //Purge channel audio files that are stagnant: 1 week or older and not present in alarm set
        audioTableManager.purgeStagnantChannelAudio();
        //Purge social audio files that are stagnant: 1 day or older and not favourite
        audioTableManager.purgeStagnantSocialAudio();
    }
}
