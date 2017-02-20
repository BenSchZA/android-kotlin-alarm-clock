package com.roostermornings.android.backgound;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.domain.AlarmQueue;
import com.roostermornings.android.sqldata.AudioTableManager;
import com.roostermornings.android.util.RoosterUtils;

import java.io.File;
import java.io.IOException;

public class BackgroundTaskReceiver extends BroadcastReceiver {

    // The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgrBackgroundTask;
    public static final String TAG = BackgroundTaskReceiver.class.getSimpleName();

    //Firebase libraries
    protected DatabaseReference mDatabase;
    protected FirebaseAuth mAuth;
    protected StorageReference mStorageRef;

    private AudioTableManager mAudioTableManager;

    public BackgroundTaskReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Log.d("Background Message:", "BackgroundTaskReceiver");
        Toast.makeText(context, "BackgroundTaskReceiver!", Toast.LENGTH_LONG).show();
        retrieveFirebaseData(context);

    }

    public void startBackgroundTask(Context context) {

        //starts a inexact repeating background task that runs every 10 seconds
        //the task runs the 'retrieveFirebaseData' method

        alarmMgrBackgroundTask = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundTaskReceiver.class);
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10 * 1000, 10 * 1000, backgroundIntent);
    }


    public void retrieveFirebaseData(final Context context) {

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

                    //TODO: check created_on timestamp on queue record, if older than 2 weeks, DELETE from DB

                    AlarmQueue alarmQueue = postSnapshot.getValue(AlarmQueue.class);
                    retrieveAudioFileFromFB(alarmQueue, context);
                }

                queueReference.removeValue();
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

            File localFile = null;
            StorageReference audioFileRef = mStorageRef.child(alarmQueue.getAudio_file_url());
            String fileName = "audio" + RoosterUtils.createRandomFileName(5);

            try {
                localFile = File.createTempFile(fileName, "3gp");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return;
            }

            audioFileRef.getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            // For our recurring task, we'll just display a message
                            Toast.makeText(context, "successfully downloaded", Toast.LENGTH_SHORT).show();

                            //store in local SQLLite database
                            mAudioTableManager.insertAudioFile(alarmQueue);

                            //remove record of queue from FB database
                            DatabaseReference queueRecordReference = FirebaseDatabase.getInstance().getReference()
                                    .child("social_rooster_queue").child(mAuth.getCurrentUser().getUid()).child(alarmQueue.getQueue_id());
                            queueRecordReference.removeValue();


                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(context, "download failed", Toast.LENGTH_SHORT).show();
                    Log.e("BackgroundTask:", "Failed download!");
                }
            });        // For our recurring task, we'll just display a message
            Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }


    }
}
