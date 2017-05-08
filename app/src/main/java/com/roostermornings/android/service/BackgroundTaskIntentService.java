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

import static com.roostermornings.android.BaseApplication.AppContext;

public class BackgroundTaskIntentService extends IntentService {
    public static final String TAG = BackgroundTaskIntentService.class.getSimpleName();
    private static final String EXTRA_PARAM1 = "com.roostermornings.android.background.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.roostermornings.android.background.extra.PARAM2";
    //Firebase libraries
    protected DatabaseReference mDatabase;
    protected FirebaseAuth mAuth;
    protected StorageReference mStorageRef;

    private final AudioTableManager audioTableManager = new AudioTableManager(this);
    private final AudioTableController audioTableController = new AudioTableController(this);
    private final DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(this);

    public BackgroundTaskIntentService() {
        super("BackgroundTaskIntentService");
    }

    public static void startActionBackgroundDownload(Context context) {
        Intent intent = new Intent(context, BackgroundTaskIntentService.class);
        intent.setAction(Constants.ACTION_BACKGROUNDDOWNLOAD);
        context.startService(intent);
    }

    public static void startActionDailyTask(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BackgroundTaskIntentService.class);
        intent.setAction(Constants.ACTION_DAILYTASK);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public static void startActionMinuteTask(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BackgroundTaskIntentService.class);
        intent.setAction(Constants.ACTION_MINUTETASK);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Constants.ACTION_BACKGROUNDDOWNLOAD.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBackgroundDownload(param1, param2);
            } else if (Constants.ACTION_DAILYTASK.equals(action)) {
                handleActionDailyTask();
            } else if (Constants.ACTION_MINUTETASK.equals(action)) {
                handleActionMinuteTask();
            }
        }
    }

    private void handleActionBackgroundDownload(String param1, String param2) {
        //if(InternetHelper.mobileDataConnection(this) && !sharedPreferences.getBoolean(Constants.USER_SETTINGS_DOWNLOAD_ON_DATA, true)) return;
        retrieveChannelContentData(getApplicationContext());
        retrieveSocialRoosterData(getApplicationContext());
    }

    private void handleActionDailyTask() {
        //Purge audio files from SQL db that are older than two weeks
        audioTableManager.purgeSocialAudioFiles();
        //TODO: update list adaptor
    }

    private void handleActionMinuteTask() {
        BaseActivity.setBadge(getApplicationContext(), ((BaseApplication)getApplication()).getNotificationFlag(Constants.FLAG_ROOSTERCOUNT));
        audioTableManager.updateRoosterCount();
        //Check if an instance of service exists, if not then start
        if(!FirebaseListenerService.running) {
            Intent intent = new Intent(this, FirebaseListenerService.class);
            startService(intent);
        }
    }

    public void retrieveChannelContentData(final Context context) {
        //The oneInstanceTask creates a new thread, only if one doesn't exist already - implements 1 minute time limit
        ExecutorService executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());

        class oneInstanceTask implements Runnable {
            public void run() {
                //Retrieve firebase audio files and cache to be played for next alarm
                mAuth = FirebaseAuth.getInstance();
                mStorageRef = FirebaseStorage.getInstance().getReference();
                mDatabase = FirebaseDatabase.getInstance().getReference();

                if (mAuth.getCurrentUser() == null || mAuth.getCurrentUser() == null) {
                    Log.d(TAG, "User not authenticated on FB!");
                    return;
                }

                //Get next pending alarm
                final DeviceAlarm deviceAlarm = deviceAlarmTableManager.getNextPendingAlarm();

                //If there is no pending alarm, don't retrieve channel content
                if (deviceAlarm == null) return;
                //Check if channel has a valid ID, else next pending alarm has no channel
                final String channelId = deviceAlarm.getChannel();
                if ("".equals(channelId)) return;

                //If the current ChannelRooster is in SQL db, then don't download
                if(audioTableManager.isChannelAudioInDatabase(channelId)) return;
                //Check firebase auth
                if (mAuth.getCurrentUser() == null || mAuth.getCurrentUser() == null) {
                    Log.d(TAG, "User not authenticated on FB!");
                    return;
                }

                final DatabaseReference channelReference = FirebaseDatabase.getInstance().getReference()
                        .child("channels").child(channelId);

                channelReference.keepSynced(true);

                ValueEventListener channelListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Channel channel = dataSnapshot.getValue(Channel.class);

                        //Check if channel exists
                        if(channel == null) return;
                        //Check if channel is active
                        if(!channel.isActive()) return;

                        //Check if channel has content and whether a story or not
                        final Integer iteration;
                        if(channel.isNew_alarms_start_at_first_iteration() && deviceAlarmTableManager.getChannelStoryIteration(channelId) != null) iteration = deviceAlarmTableManager.getChannelStoryIteration(channelId);
                            //If iteration is null, this means no entry in db - set to 1 and process from there
                        else if (channel.isNew_alarms_start_at_first_iteration()) iteration = 1;
                        else if (channel.getCurrent_rooster_cycle_iteration() < 1) return;
                        else iteration = channel.getCurrent_rooster_cycle_iteration();

                        final DatabaseReference channelRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                                .child("channel_rooster_uploads").child(channelId);

                        //Ensure latest data is pulled
                        channelRoosterUploadsReference.keepSynced(true);

                        ValueEventListener channelRoosterUploadsListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                TreeMap<Integer,ChannelRooster> channelIterationMap = new TreeMap<>();
                                //Check if node has children i.e. channelId content exists
                                if(dataSnapshot.getChildrenCount() == 0) return;
                                //Iterate over all content children
                                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                    ChannelRooster channelRooster = postSnapshot.getValue(ChannelRooster.class);
                                    if(channelRooster.isActive() && (channelRooster.getRooster_cycle_iteration() != iteration)) {
                                        channelIterationMap.put(channelRooster.getRooster_cycle_iteration(), channelRooster);
                                    } else if(channelRooster.isActive()) {
                                        retrieveChannelContentAudio(channelRooster, context);
                                        return;
                                    }
                                }

                                //Check head and tail of naturally sorted TreeMap for next valid channel content
                                SortedMap<Integer,ChannelRooster> tailMap = channelIterationMap.tailMap(iteration);
                                SortedMap<Integer,ChannelRooster> headMap = channelIterationMap.headMap(iteration);
                                if(!tailMap.isEmpty()) {
                                    //User is starting story at next valid entry
                                    //Set SQL entry for iteration to current valid story iteration, to be incremented on play
                                    deviceAlarmTableManager.setChannelStoryIteration(channelId, tailMap.firstKey());
                                    //Retrieve channel audio
                                    retrieveChannelContentAudio(channelIterationMap.get(tailMap.firstKey()), context);
                                } else if(!headMap.isEmpty()) {
                                    //User is starting story from beginning again, at valid entry
                                    //Set SQL entry for iteration to current valid story iteration, to be incremented on play
                                    deviceAlarmTableManager.setChannelStoryIteration(channelId, headMap.firstKey());
                                    //Retrieve channel audio
                                    retrieveChannelContentAudio(channelIterationMap.get(headMap.firstKey()), context);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
                                deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
                            }
                        };
                        channelRoosterUploadsReference.addListenerForSingleValueEvent(channelRoosterUploadsListener);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                        //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
                        deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
                    }
                };
                channelReference.addListenerForSingleValueEvent(channelListener);
            }
        }
        executor.execute(new oneInstanceTask());
    }

    public void retrieveSocialRoosterData(final Context context) {
        //The oneInstanceTask creates a new thread, only if one doesn't exist already - implements 1 minute time limit
        ExecutorService executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());

        class oneInstanceTask implements Runnable {
            public void run() {
                //Retrieve firebase audio files and cache to be played for next alarm
                mAuth = FirebaseAuth.getInstance();
                mStorageRef = FirebaseStorage.getInstance().getReference();
                mDatabase = FirebaseDatabase.getInstance().getReference();

                if (mAuth.getCurrentUser() == null || mAuth.getCurrentUser() == null) {
                    Log.d(TAG, "User not authenticated on FB!");
                    return;
                }

                final DatabaseReference queueReference = FirebaseDatabase.getInstance().getReference()
                        .child("social_rooster_queue").child(mAuth.getCurrentUser().getUid());

                //Ensure latest data is pulled
                queueReference.keepSynced(true);

                ValueEventListener socialQueueListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            SocialRooster socialRooster = postSnapshot.getValue(SocialRooster.class);
                            retrieveSocialRoosterAudio(socialRooster, context);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                    }
                };
                queueReference.addListenerForSingleValueEvent(socialQueueListener);
            }
        }
        executor.execute(new oneInstanceTask());
    }

    private void retrieveChannelContentAudio(final ChannelRooster channelRooster, final Context context) {

        if(channelRooster == null) return;
        if(channelRooster.getChannel_uid() == null) return;

        //Check if audio in db, if so return and don't download
        //An issue here is that it's asynchronous AF, so you get lots of download tasks queued,
        //and then the db entry is relied upon to check for existing audio files,
        //but it is only entered on completion...
        //If download fails...? Remove db entry? but then could loop
        //New solution: ensure only one download task
        //Previous solution: inser into db and then download, updating filename on completion using:
        //mAudioTableManager.setChannelAudioFileName(channelRooster.getChannel_uid(), audioFileUniqueName);

        if(audioTableManager.isChannelAudioInDatabase(channelRooster.getChannel_uid())) return;

        try {
            //https://firebase.google.com/docs/storage/android/download-files
            final StorageReference audioFileRef = mStorageRef.getStorage().getReferenceFromUrl(channelRooster.getAudio_file_url());
            final String audioFileUniqueName = Constants.FILENAME_PREFIX_ROOSTER_CONTENT + RoosterUtils.createRandomFileName(5) + ".3gp";

            //Create new object for storing in SQL db
            DeviceAudioQueueItem deviceAudioQueueItem = new DeviceAudioQueueItem();
            deviceAudioQueueItem.fromChannelRooster(channelRooster, audioFileUniqueName);
            //Could use channelRooster.getUpload_date(), but then can't use for purging files
            deviceAudioQueueItem.setDate_created(System.currentTimeMillis());

            //Pre-cache image to display on alarm screen, in case no internet connection
            if(!channelRooster.getPhoto().isEmpty()) Picasso.with(AppContext).load(channelRooster.getPhoto()).fetch();

            //store in local SQLLite database and check if successful
            if(audioTableManager.insertChannelAudioFile(deviceAudioQueueItem)) {

                audioFileRef.getBytes(Constants.MAX_ROOSTER_FILE_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {

                        try {
                            FileOutputStream outputStream;
                            outputStream = context.openFileOutput(audioFileUniqueName, Context.MODE_PRIVATE);
                            outputStream.write(bytes);
                            outputStream.close();

                            if (BuildConfig.DEBUG)
                                Toast.makeText(context, "successfully downloaded", Toast.LENGTH_SHORT).show();

                            //Send broadcast message to notify all receivers of download finished
                            Intent intent = new Intent(Constants.ACTION_CHANNEL_DOWNLOAD_FINISHED);
                            sendBroadcast(intent);

                        } catch (Exception e) {
                            e.printStackTrace();
                            //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
                            deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
                            audioTableManager.removeChannelAudioEntries(channelRooster.getChannel_uid());
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
                        deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
                        audioTableManager.removeChannelAudioEntries(channelRooster.getChannel_uid());
                    }
                });
            }


            if(BuildConfig.DEBUG) Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
            deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
        }
    }

    private void retrieveSocialRoosterAudio(final SocialRooster socialRooster, final Context context) {

        if(socialRooster == null) return;

        if(audioTableManager.isChannelAudioInDatabase(socialRooster.getQueue_id())) return;

        try {

            StorageReference audioFileRef = mStorageRef.child("social_rooster_uploads/" + socialRooster.getAudio_file_url());
            final String audioFileUniqueName = Constants.FILENAME_PREFIX_ROOSTER_CONTENT + RoosterUtils.createRandomFileName(5) + ".3gp";
            final DatabaseReference queueRecordReference = FirebaseDatabase.getInstance().getReference()
                    .child("social_rooster_queue").child(mAuth.getCurrentUser().getUid()).child(socialRooster.getQueue_id());

            audioFileRef.getBytes(Constants.MAX_ROOSTER_FILE_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {

                    try {
                        FileOutputStream outputStream;
                        outputStream = context.openFileOutput(audioFileUniqueName, Context.MODE_PRIVATE);
                        outputStream.write(bytes);
                        outputStream.close();

                        if(BuildConfig.DEBUG) Toast.makeText(context, "successfully downloaded", Toast.LENGTH_SHORT).show();

                        //Create new object for storing in SQL db
                        DeviceAudioQueueItem deviceAudioQueueItem = new DeviceAudioQueueItem();
                        deviceAudioQueueItem.fromSocialRooster(socialRooster, audioFileUniqueName);

                        //store in local SQLLite database
                        if(audioTableManager.insertSocialAudioFile(deviceAudioQueueItem)) {
                            //remove record of queue from FB database
                            queueRecordReference.removeValue();
                        }

                        //Pre-cache image to display on alarm screen, in case no internet connection
                        if(!socialRooster.getProfile_pic().isEmpty()) Picasso.with(AppContext).load(socialRooster.getProfile_pic()).fetch();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });


            // For our recurring task, we'll just display a message
            if(BuildConfig.DEBUG) Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
