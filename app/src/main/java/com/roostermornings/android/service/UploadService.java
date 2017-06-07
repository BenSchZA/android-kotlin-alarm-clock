/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.analytics.FA;
import com.roostermornings.android.domain.FCMPayloadSocialRooster;
import com.roostermornings.android.domain.NodeAPIResult;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.node_api.IHTTPClient;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.Toaster;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Timestamp;
import java.util.ArrayList;

import javax.inject.Inject;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

//Service to offload the task of uploading a new Social Rooster to Firebase cloud storage, receive a link -> Firebase entries & Node post
public class UploadService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new UploadService.LocalBinder();
    private HandlerThread handlerThread;

    protected static final String TAG = UploadService.class.getSimpleName();

    private final UploadService mThis = this;

    private Handler mHandler;
    private StorageReference mStorageRef;
    String mAudioSavePathInDevice = "";
    ArrayList<User> friendsList = new ArrayList<>();
    String firebaseIdToken = "";

    @Inject FirebaseUser firebaseUser;
    @Inject
    SharedPreferences sharedPreferences;

    public UploadService() {
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public UploadService getService() {
            // Return this instance of AudioService so clients can call public methods
            return UploadService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BaseApplication baseApplication = (BaseApplication) getApplication();
        baseApplication.getRoosterApplicationComponent().inject(this);

        foregroundNotification("Audio upload in progress");

        // Create a new background thread for processing messages or runnables sequentially
        mThis.handlerThread = new HandlerThread("AudioUploadHandler");
        // Starts the background thread
        handlerThread.start();
        // Create a handler attached to the HandlerThread's Looper
        mHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // Process received messages here!
                Bundle uploadData = msg.getData();
                mAudioSavePathInDevice = uploadData.getString(Constants.EXTRA_LOCAL_FILE_STRING);
                try {
                    friendsList = (ArrayList<User>) uploadData.getSerializable(Constants.EXTRA_FRIENDS_LIST);
                    if (friendsList != null && !friendsList.isEmpty()) {
                        uploadAudioFile(mAudioSavePathInDevice, friendsList);
                    }
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    Toaster.makeToast(getBaseContext(), "Rooster upload failed.", Toast.LENGTH_SHORT).checkTastyToast();
                    endService();
                }
            }
        };

        return START_STICKY;
    }

    /** methods for clients */

    private void uploadAudioFile(String mAudioSavePathInDevice, final ArrayList<User> friendsList) {

        mStorageRef = FirebaseStorage.getInstance().getReference();

        final File localFile = new File(mAudioSavePathInDevice);
        final Uri file = Uri.fromFile(localFile);
        StorageReference audioFileRef = mStorageRef.child("social_rooster_uploads/" + file.getLastPathSegment());

        //Log Firebase user prop: social rooster sender
        if(!friendsList.isEmpty()) {
            int averageSentRoosters = 0;
            if (sharedPreferences.contains(FA.UserProp.social_rooster_sender.shared_pref_average_sent_roosters)) {
                averageSentRoosters = (friendsList.size() +
                        sharedPreferences.getInt(FA.UserProp.social_rooster_sender.shared_pref_average_sent_roosters, 0)) / 2;
            } else {
                averageSentRoosters = friendsList.size();
            }
            sharedPreferences.edit()
                    .putInt(FA.UserProp.social_rooster_sender.shared_pref_average_sent_roosters, averageSentRoosters)
                    .apply();
            FA.SetUserProp(FA.UserProp.social_rooster_sender.class, FA.UserProp.social_rooster_sender.shared_pref_average_sent_roosters);
        }



        audioFileRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        String firebaseStorageURL = file.getLastPathSegment();

                        for (User friend : friendsList) {
                            if (friend.getSelected()) {
                                sendRoosterToUser(friend, firebaseStorageURL);
                            }
                        }

                        FA.Log(FA.Event.social_rooster_sent.class, FA.Event.social_rooster_sent.Param.social_rooster_receivers, friendsList.size());

                        endService();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful upload
                        Toaster.makeToast(getApplicationContext(), "Error uploading!", Toast.LENGTH_LONG).checkTastyToast();
                        endService();
                    }
                });
    }

    private void endService(){
        //Delete all temporary recording files
        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(Constants.FILENAME_PREFIX_ROOSTER_TEMP_RECORDING);
            }
        });
        for (File file:
             files) {
            file.delete();
        }

        stopForeground(true);
        handlerThread.quitSafely();
        this.stopSelf();
    }

    private void sendRoosterToUser(User friend, String firebaseStorageURL) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        FirebaseUser currentUser = firebaseUser;
        //get reference to Firebase database
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        String uploadUrl = String.format("social_rooster_uploads/%s", currentUser.getUid());
        String queueUrl = String.format("social_rooster_queue/%s", friend.getUid());

        String uploadKey = mDatabase.child(uploadUrl).push().getKey();

        SocialRooster socialRoosterUploaded = new SocialRooster(firebaseStorageURL,
                friend.getUser_name(),
                false,
                friend.getProfile_pic(),
                timestamp.getTime(),
                friend.getUid(), uploadKey, currentUser.getUid());

        SocialRooster socialRoosterQueue = new SocialRooster(firebaseStorageURL,
                BaseActivity.mCurrentUser.getUser_name(),
                false,
                BaseActivity.mCurrentUser.getProfile_pic(),
                timestamp.getTime(),
                friend.getUid(), uploadKey, BaseActivity.mCurrentUser.getUid());

        //Note the matching keys
        mDatabase.getDatabase().getReference(uploadUrl + "/" + uploadKey).setValue(socialRoosterUploaded);
        mDatabase.getDatabase().getReference(queueUrl + "/" + uploadKey).setValue(socialRoosterQueue);
        socialRoosterNotifyUserFCMMessage(friend.getUid());
        Toaster.makeToast(getApplicationContext(), "Social rooster sent to " + friend.getUser_name() + "!", Toast.LENGTH_LONG).checkTastyToast();
    }

    private void socialRoosterNotifyUserFCMMessage(String recipientUserId) {
        Call<NodeAPIResult> call = apiService().notifySocialUploadRecipient(
                new FCMPayloadSocialRooster(mThis.firebaseIdToken, recipientUserId));

        call.enqueue(new Callback<NodeAPIResult>() {
            @Override
            public void onResponse(Response<NodeAPIResult> response,
                                   Retrofit retrofit) {

                int statusCode = response.code();
                NodeAPIResult apiResponse = response.body();

                if (statusCode == 200) {

                    Log.d("apiResponse", apiResponse.toString());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.i(TAG, t.getLocalizedMessage());
            }
        });
    }

    private IHTTPClient apiService() {
        BaseApplication baseApplication = (BaseApplication) getApplication();
        return baseApplication.getAPIService();
    }

    public void processAudioFile(String firebaseIdToken, String localFileString, ArrayList<User> friendsList) {
        mThis.firebaseIdToken = firebaseIdToken;
        // Secure a new message to send
        Message message = mHandler.obtainMessage();
        // Create a bundle
        Bundle uploadData = new Bundle();
        uploadData.putString(Constants.EXTRA_LOCAL_FILE_STRING, localFileString);
        uploadData.putSerializable(Constants.EXTRA_FRIENDS_LIST, friendsList);
        // Attach bundle to the message
        message.setData(uploadData);
        // Send message through the handler
        mHandler.sendMessage(message);
    }

    private void foregroundNotification(String state) {

        Intent notificationIntent = new Intent(this, AudioService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification=new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentText("Rooster Mornings: " + state)
                .setContentIntent(pendingIntent).build();

        startForeground(Constants.UPLOADSERVICE_NOTIFICATION_ID, notification);
    }
}
