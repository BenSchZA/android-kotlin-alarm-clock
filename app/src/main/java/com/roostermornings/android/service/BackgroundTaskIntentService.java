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
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.RoosterUtils;

import java.io.FileOutputStream;
import java.util.ArrayList;

public class BackgroundTaskIntentService extends IntentService {
    public static final String TAG = BackgroundTaskIntentService.class.getSimpleName();
    private static final String EXTRA_PARAM1 = "com.roostermornings.android.background.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.roostermornings.android.background.extra.PARAM2";
    //Firebase libraries
    protected DatabaseReference mDatabase;
    protected FirebaseAuth mAuth;
    protected StorageReference mStorageRef;

    private AudioTableManager mAudioTableManager;

    public BackgroundTaskIntentService() {
        super("BackgroundTaskIntentService");
    }

    public static void startActionBackgroundDownload(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BackgroundTaskIntentService.class);
        intent.setAction(Constants.ACTION_BACKGROUNDDOWNLOAD);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
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
            if (Constants.ACTION_BACKGROUNDDOWNLOAD.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBackgroundDownload(param1, param2);
            } else if (Constants.ACTION_DAILYTASK.equals(action)) {
                handleActionDailyTask();
            }
        }
    }

    private void handleActionBackgroundDownload(String param1, String param2) {
        retrieveChannelContentData(getApplicationContext());
        retrieveSocialRoosterData(getApplicationContext());
    }

    private void handleActionDailyTask() {
        //Purge audio files from SQL db that are older than two weeks
        mAudioTableManager = new AudioTableManager(getApplicationContext());
        mAudioTableManager.purgeSocialAudioFiles();
    }

    public void retrieveChannelContentData(final Context context) {
        //Retrieve firebase audio files and cache to be played for next alarm
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAudioTableManager = new AudioTableManager(context);

        //Get next pending alarm
        final DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(this);
        final DeviceAlarm deviceAlarm = deviceAlarmTableManager.getNextPendingAlarm();

        //If there is no pending alarm, don't retrieve channel content
        if (deviceAlarm == null) return;
        //Check if channel has a valid ID, else next pending alarm has no channel
        final String channelId = deviceAlarm.getChannel();
        if (channelId == null || channelId.equals("")) return;

        //If the current ChannelRooster is in SQL db, then don't download
        ArrayList<String> existingChannelQueueIDs = new ArrayList<>();
        for (DeviceAudioQueueItem audioItem :
                mAudioTableManager.extractAllChannelAudioFiles()) {
            existingChannelQueueIDs.add(audioItem.getQueue_id());
        }
        if (existingChannelQueueIDs.contains(channelId)) return;

        if (mAuth.getCurrentUser() == null || mAuth.getCurrentUser() == null) {
            Log.d(TAG, "User not authenticated on FB!");
            return;
        }

        final DatabaseReference channelReference = FirebaseDatabase.getInstance().getReference()
                .child("channels").child(channelId);

        ValueEventListener channelListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Channel channel = dataSnapshot.getValue(Channel.class);

                //Check if channel exists
                if(channel == null) return;
                //Check if channel is active
                if(!channel.isActive()) return;

                //Check if channel has content and whether a story or not
                Integer iteration;
                if(channel.isNew_alarms_start_at_first_iteration()) iteration = deviceAlarmTableManager.getChannelStoryIteration(channelId);
                else if (channel.getCurrent_rooster_cycle_iteration() < 1) return;
                else iteration = channel.getCurrent_rooster_cycle_iteration();

                //TODO: Ping Node for next valid entry - Check that new story iteration is not larger than the number of audio files

                final DatabaseReference channelRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                        .child("channel_rooster_uploads").child(channelId).child(String.valueOf(iteration));

                ValueEventListener channelRoosterUploadsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ChannelRooster channelRooster = dataSnapshot.getValue(ChannelRooster.class);

                        if(channelRooster == null) return;
                        retrieveChannelContentAudio(channelRooster, context);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };
                channelRoosterUploadsReference.addListenerForSingleValueEvent(channelRoosterUploadsListener);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        channelReference.addListenerForSingleValueEvent(channelListener);
    }

    public void retrieveSocialRoosterData(final Context context) {
        //Retrieve firebase audio files and cache to be played for next alarm
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAudioTableManager = new AudioTableManager(context);

        if (mAuth.getCurrentUser() == null || mAuth.getCurrentUser() == null) {
            Log.d(TAG, "User not authenticated on FB!");
            return;
        }

        final DatabaseReference queueReference = FirebaseDatabase.getInstance().getReference()
                .child("social_rooster_queue").child(mAuth.getCurrentUser().getUid());

        ValueEventListener socialQueueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    SocialRooster socialRooster = postSnapshot.getValue(SocialRooster.class);
                    //If firebase queue entry is older than two weeks then delete from db and don't process
                    if (System.currentTimeMillis() - socialRooster.getDate_uploaded() >= 1209600000) {
                        queueReference.child(postSnapshot.getKey()).removeValue();
                    } else {
                        retrieveSocialRoosterAudio(socialRooster, context);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        queueReference.addListenerForSingleValueEvent(socialQueueListener);
    }

    private void retrieveChannelContentAudio(final ChannelRooster channelRooster, final Context context) {

        try {
            //TODO: replace with audiofileURL
            StorageReference audioFileRef = mStorageRef.child("channel_roosters/" + channelRooster.getAudio_file_name());
            final String audioFileUniqueName = "audio" + RoosterUtils.createRandomFileName(5) + ".3gp";

            final long FIVE_MEGABYTE = 5 * 1024 * 1024;
            audioFileRef.getBytes(FIVE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {

                    try {
                        FileOutputStream outputStream;
                        outputStream = context.openFileOutput(audioFileUniqueName, Context.MODE_PRIVATE);
                        outputStream.write(bytes);
                        outputStream.close();

                        Toast.makeText(context, "successfully downloaded", Toast.LENGTH_SHORT).show();

                        //Create new object for storing in SQL db
                        DeviceAudioQueueItem deviceAudioQueueItem = new DeviceAudioQueueItem();
                        deviceAudioQueueItem.fromChannelRooster(channelRooster, audioFileUniqueName);
                        //Could use channelRooster.getUpload_date(), but then can't use for purging files
                        deviceAudioQueueItem.setDate_created(System.currentTimeMillis());

                        //store in local SQLLite database
                        mAudioTableManager.insertChannelAudioFile(deviceAudioQueueItem);

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
            Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void retrieveSocialRoosterAudio(final SocialRooster socialRooster, final Context context) {

        try {

            StorageReference audioFileRef = mStorageRef.child("social_rooster_uploads/" + socialRooster.getAudio_file_url());
            final String audioFileUniqueName = "audio" + RoosterUtils.createRandomFileName(5) + ".3gp";
            final DatabaseReference queueRecordReference = FirebaseDatabase.getInstance().getReference()
                    .child("social_rooster_queue").child(mAuth.getCurrentUser().getUid()).child(socialRooster.getQueue_id());

            final long FIVE_MEGABYTE = 5 * 1024 * 1024;
            audioFileRef.getBytes(FIVE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {

                    try {
                        FileOutputStream outputStream;
                        outputStream = context.openFileOutput(audioFileUniqueName, Context.MODE_PRIVATE);
                        outputStream.write(bytes);
                        outputStream.close();

                        Toast.makeText(context, "successfully downloaded", Toast.LENGTH_SHORT).show();

                        //Create new object for storing in SQL db
                        DeviceAudioQueueItem deviceAudioQueueItem = new DeviceAudioQueueItem();
                        deviceAudioQueueItem.fromSocialRooster(socialRooster, audioFileUniqueName);

                        //store in local SQLLite database
                        mAudioTableManager.insertSocialAudioFile(deviceAudioQueueItem);

                        //remove record of queue from FB database
                        queueRecordReference.removeValue();

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
            Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
