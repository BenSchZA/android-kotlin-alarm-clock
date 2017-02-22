package com.roostermornings.android.background;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.domain.AlarmQueue;
import com.roostermornings.android.sqldata.AudioTableManager;
import com.roostermornings.android.util.RoosterUtils;

import java.io.FileOutputStream;

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
        startBackgroundTaskIntentService(context);


    }

    public void startBackgroundTask(Context context) {

        //starts a inexact repeating background task that runs every 10 seconds
        //the task runs the 'retrieveFirebaseData' method

        alarmMgrBackgroundTask = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BackgroundTaskReceiver.class);
        PendingIntent backgroundIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgrBackgroundTask.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10 * 1000,
                120 * 1000, backgroundIntent);
    }

    public void startBackgroundTaskIntentService(Context context){
        Intent BackgroundTaskIntent = new Intent(context, BackgroundTaskIntentService.class);
        BackgroundTaskIntent.setAction("com.roostermornings.android.background.action.BACKGROUND_DOWNLOAD");
        context.startService(BackgroundTaskIntent);
    }
}
