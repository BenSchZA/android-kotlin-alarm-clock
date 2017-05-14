/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.sqlutil.AudioTableController;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.RoosterUtils;
import com.squareup.picasso.Picasso;

import java.io.FileOutputStream;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        //Purge audio files from SQL db that are older than two weeks
        audioTableManager.purgeSocialAudioFiles();
        //TODO: update list adaptor
    }
}
