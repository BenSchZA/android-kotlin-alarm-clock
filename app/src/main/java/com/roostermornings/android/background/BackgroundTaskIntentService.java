package com.roostermornings.android.background;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.SystemClock;
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
import com.roostermornings.android.domain.AlarmQueue;
import com.roostermornings.android.sqldata.AudioTableManager;
import com.roostermornings.android.util.RoosterUtils;

import java.io.FileOutputStream;

public class BackgroundTaskIntentService extends IntentService {
    private static final String ACTION_BACKGROUND_DOWNLOAD = "com.roostermornings.android.background.action.BACKGROUND_DOWNLOAD";
    private static final String ACTION_DAILY_TASK = "com.roostermornings.android.background.action.DAILY_TASK";

    private static final String EXTRA_PARAM1 = "com.roostermornings.android.background.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.roostermornings.android.background.extra.PARAM2";

    public static final String TAG = BackgroundTaskIntentService.class.getSimpleName();

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
        intent.setAction(ACTION_BACKGROUND_DOWNLOAD);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public static void startActionDailyTask(Context context, String param1, String param2) {
        Intent intent = new Intent(context, BackgroundTaskIntentService.class);
        intent.setAction(ACTION_DAILY_TASK);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_BACKGROUND_DOWNLOAD.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBackgroundDownload(param1, param2);
            } else if(ACTION_DAILY_TASK.equals(action)){
                handleActionDailyTask();
            }
        }
    }

    private void handleActionBackgroundDownload(String param1, String param2) {
        retrieveFirebaseData(getApplicationContext());
    }

    private void handleActionDailyTask() {
        //Purge audio files from SQL db that are older than two weeks
        mAudioTableManager.purgeAudioFiles();
    }

    public void retrieveFirebaseData(final Context context) {
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

        ValueEventListener alarmQueueListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    AlarmQueue alarmQueue = postSnapshot.getValue(AlarmQueue.class);
                    //If firebase queue entry is older than two weeks then delete from db and don't process
                    if(System.currentTimeMillis() - alarmQueue.getDate_uploaded() >= 1209600000){
                        queueReference.child(postSnapshot.getKey()).removeValue();
                    } else{
                        retrieveAudioFileFromFB(alarmQueue, context);
                    }
                }
                //queueReference.removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };

        queueReference.addListenerForSingleValueEvent(alarmQueueListener);

    }


    private void retrieveAudioFileFromFB(final AlarmQueue alarmQueue, final Context context) {

        try {
            StorageReference audioFileRef = mStorageRef.child(alarmQueue.getAudio_file_url());
            final String audioFileUniqueName = "audio" + RoosterUtils.createRandomFileName(5) + ".3gp";
            final DatabaseReference queueRecordReference = FirebaseDatabase.getInstance().getReference()
                    .child("social_rooster_queue").child(mAuth.getCurrentUser().getUid()).child(alarmQueue.getQueue_id());

            final long ONE_MEGABYTE = 1024 * 1024;
            audioFileRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {

                    try {
                        FileOutputStream outputStream;
                        outputStream = context.openFileOutput(audioFileUniqueName, Context.MODE_PRIVATE);
                        outputStream.write(bytes);
                        outputStream.close();

                        Toast.makeText(context, "successfully downloaded", Toast.LENGTH_SHORT).show();

                        alarmQueue.setAudio_file_url(audioFileUniqueName);
                        //store in local SQLLite database
                        mAudioTableManager.insertAudioFile(alarmQueue);
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
