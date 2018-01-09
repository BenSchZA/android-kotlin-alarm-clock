/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.usage.NetworkStatsManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter_data.ChannelManager;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.geolocation.GeoHashUtils;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.StrUtils;
import com.roostermornings.android.util.Toaster;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static android.content.ContentValues.TAG;
import static android.content.Context.ACCOUNT_SERVICE;
import static com.facebook.FacebookSdk.CALLBACK_OFFSET_PROPERTY;
import static com.facebook.FacebookSdk.getApplicationContext;
import static com.roostermornings.android.util.Constants.ACCOUNT;
import static com.roostermornings.android.util.Constants.ACCOUNT_TYPE;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class DownloadSyncAdapter extends AbstractThreadedSyncAdapter {

    private boolean channelSyncActive;
    private boolean socialSyncActive;

    //Firebase SDK
    @Inject @Nullable FirebaseUser firebaseUser;
    @Inject StorageReference mStorageRef;
    @Inject DatabaseReference mDatabaseRef;
    @Inject AudioTableManager audioTableManager;
    @Inject DeviceAlarmTableManager deviceAlarmTableManager;
    @Inject GeoHashUtils geoHashUtils;
    @Inject JSONPersistence jsonPersistence;
    @Inject ChannelManager channelManager;
    @Inject SharedPreferences sharedPreferences;

    private static OnChannelDownloadListener onChannelDownloadListener;

    public interface OnChannelDownloadListener {
        void onChannelDownloadStarted(String channelId);
        void onChannelDownloadComplete(boolean valid, String channelId);
    }

    public static void setOnChannelDownloadListener(OnChannelDownloadListener listener) {
        onChannelDownloadListener = listener;
    }

    public static Bundle getForceBundle() {
        Bundle forceBundle = new Bundle();
        forceBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        forceBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        return forceBundle;
    }

    // Global variables
    // Define a variable to contain a content resolver instance
    private ContentResolver mContentResolver;

    /**
     * Set up the sync adapter
     */
    public DownloadSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        BaseApplication.getRoosterApplicationComponent().inject(this);

        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }


    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public DownloadSyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
    }

    /*
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {

        BaseApplication.getRoosterApplicationComponent().inject(this);

        /*
         * Put the data transfer code here.
         */
        Log.d("SyncAdapter: ", "onPerformSync()");

        if(firebaseUser != null) {
            //Get channel and social audio content
            retrieveSocialRoosterData(getApplicationContext());
            retrieveChannelContentData(getApplicationContext());

            //Update badge count for roosters received
            BaseActivity.setBadge(getApplicationContext(), BaseApplication.getNotificationFlag(Constants.FLAG_ROOSTERCOUNT));
            audioTableManager.updateRoosterCount();

            //Check if the user's geohash location entry is still valid
            geoHashUtils.checkUserGeoHash();
        }
    }

    /**
     * Create a new dummy account for the sync adapter
     *
     * @param context The application context
     */
    public static Account CreateSyncAccount(Context context) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
        } else {
            /*
             * The account exists or some other error occurred. LogMany this, report it,
             * or handle it internally.
             */
        }
        return newAccount;
    }

    private void retrieveChannelContentData(final Context context) {
        //The oneInstanceTask creates a new thread, only if one doesn't exist already - implements 2 minute time limit
        ExecutorService executor = new ThreadPoolExecutor(1, 1, 2, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());

        class oneInstanceTask implements Runnable {
            public void run() {
                //Retrieve firebase audio files and cache to be played for next alarm

                //Check Firebase auth
                if (firebaseUser == null) {
                    Log.d(TAG, "User not authenticated on FB!");
                    return;
                }
                //Check Firebase storage
                if (mStorageRef == null) {
                    Log.d(TAG, "FB storage reference invalid!");
                    return;
                }
                //Check Firebase database
                if (mDatabaseRef == null) {
                    Log.d(TAG, "FB database reference invalid!");
                    return;
                }

                //Get next pending alarm
                final DeviceAlarm deviceAlarm = deviceAlarmTableManager.getNextPendingAlarm();

                //If there is no pending alarm, don't retrieve channel content
                if (deviceAlarm == null) return;
                //Check if channel has a valid ID, else next pending alarm has no channel
                final String channelId = deviceAlarm.getChannel();
                if (!StrUtils.notNullOrEmpty(channelId)) return;

                final DatabaseReference channelReference = mDatabaseRef
                        .child("channels").child(channelId);
                channelReference.keepSynced(true);

                ValueEventListener channelListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final Channel channel = dataSnapshot.getValue(Channel.class);

                        //Check if channel exists
                        if(channel == null) return;
                        //Check if channel is active
                        if(!channel.isActive()) return;

                        //Check if channel has content and whether a story or not
                        //For time dependant channels, iteration should be the iteration for the day of alarm
                        final Integer iteration;
                        Integer tempIteration;
                        if(channel.isNew_alarms_start_at_first_iteration()) {
                            tempIteration = jsonPersistence.getStoryIteration(channelId);
                            if (tempIteration <= 0) tempIteration = 1;
                            iteration = tempIteration;
                        } else {
                            Calendar currentCalendar = Calendar.getInstance();
                            Calendar nextPendingCalendar = Calendar.getInstance();
                            nextPendingCalendar.setTimeInMillis(deviceAlarm.getMillis());

                            tempIteration = channel.getCurrent_rooster_cycle_iteration() > 0 ? channel.getCurrent_rooster_cycle_iteration() : 1;
                            Integer daysUntilAlarm = nextPendingCalendar.get(Calendar.DAY_OF_MONTH) - currentCalendar.get(Calendar.DAY_OF_MONTH);
                            iteration = daysUntilAlarm > 0 ? tempIteration + daysUntilAlarm : tempIteration;
                        }

                        final DatabaseReference channelRoosterUploadsReference = mDatabaseRef
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
                                    if(channelRooster.isActive()) {
                                        channelIterationMap.put(channelRooster.getRooster_cycle_iteration(), channelRooster);
                                    }
                                }

                                if(channelIterationMap.isEmpty()) return;

                                //Check head and tail of naturally sorted TreeMap for next valid channel content
                                Integer actualIterationKey;
                                //Check if iteration is valid in the context of channelIterationMap keys
                                if(iteration > channelIterationMap.lastKey() || iteration < channelIterationMap.firstKey()) {
                                    actualIterationKey = channelIterationMap.firstKey();
                                } else {
                                    actualIterationKey = iteration;
                                }
                                ChannelRooster validChannelRooster = channelManager.findNextValidChannelRooster(
                                        channelIterationMap, channel, actualIterationKey);
                                retrieveChannelContentAudio(validChannelRooster, context);
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

    private void retrieveSocialRoosterData(final Context context) {
        //The oneInstanceTask creates a new thread, only if one doesn't exist already - implements 1 minute time limit
        ExecutorService executor = new ThreadPoolExecutor(1, 1, 2, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());

        class oneInstanceTask implements Runnable {
            public void run() {
                //Retrieve firebase audio files and cache to be played for next alarm

                //Check Firebase auth
                if (firebaseUser == null) {
                    Log.d(TAG, "User not authenticated on FB!");
                    return;
                }
                //Check Firebase storage
                if (mStorageRef == null) {
                    Log.d(TAG, "FB storage reference invalid!");
                    return;
                }
                //Check Firebase database
                if (mDatabaseRef == null) {
                    Log.d(TAG, "FB database reference invalid!");
                    return;
                }

                final DatabaseReference queueReference = mDatabaseRef
                        .child("social_rooster_queue").child(firebaseUser.getUid());

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

    public void retrieveChannelContentAudio(final ChannelRooster channelRooster, final Context context) {

        if(channelRooster == null) return;
        if(channelRooster.getChannel_uid() == null) return;

        //Check if audio in db, if so return and don't download
        //An issue here is that it's asynchronous AF, so you get lots of download tasks queued,
        //and then the db entry is relied upon to check for existing audio files,
        //but it is only entered on completion...
        //If download fails...? Remove db entry? but then could loop
        //New solution: ensure only one download task
        //Previous solution: insert into db and then download, updating filename on completion using:
        //mAudioTableManager.setChannelAudioFileName(channelRooster.getChannel_uid(), audioFileUniqueName);

        if(channelSyncActive) return;
        if(audioTableManager.isChannelAudioURLFresh(channelRooster.getChannel_uid(), channelRooster.getAudio_file_url())) {
            //Notify any listeners
            if(onChannelDownloadListener != null)
                onChannelDownloadListener.onChannelDownloadComplete(true, channelRooster.getChannel_uid());

            return;
        }

        try {
            //https://firebase.google.com/docs/storage/android/download-files
            final StorageReference audioFileRef = mStorageRef.getStorage().getReferenceFromUrl(channelRooster.getAudio_file_url());
            final String audioFileUniqueName = Constants.FILENAME_PREFIX_ROOSTER_CONTENT + RoosterUtils.createRandomUID(5) + ".3gp";

            Crashlytics.log("Channel pre-download UID: " + channelRooster.getChannel_uid());

            //Pre-cache image to display on alarm screen, in case no internet connection
            if(StrUtils.notNullOrEmpty(channelRooster.getPhoto())) Picasso.with(getApplicationContext()).load(channelRooster.getPhoto()).fetch();

            channelSyncActive = true;

            //Notify any listeners
            if(onChannelDownloadListener != null)
                onChannelDownloadListener.onChannelDownloadStarted(channelRooster.getChannel_uid());

            //Check for oversize files and report to Crashlytics
            if(isOversize(channelRooster.audio_file_url)) return;

            audioFileRef.getBytes(Constants.ABSOLUTE_MAX_FILE_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {

                    try {
                        FileOutputStream outputStream;
                        outputStream = context.openFileOutput(audioFileUniqueName, Context.MODE_PRIVATE);
                        outputStream.write(bytes);
                        outputStream.close();

                        //AudioService report logging
                        File file = new File(context.getFilesDir() + "/" + audioFileUniqueName);
                        Crashlytics.log("Rooster file path: " + file.getPath());
                        Crashlytics.log("Rooster is file?: " + String.valueOf(file.isFile()));
                        Crashlytics.log("Rooster channel source: " + channelRooster.getName());
                        Crashlytics.log("Rooster file size (kb): " + String.valueOf(file.length()/1024));

                        Crashlytics.log("insertChannelAudioFile() started");
                        //Create new object for storing in SQL db
                        DeviceAudioQueueItem deviceAudioQueueItem = new DeviceAudioQueueItem();
                        deviceAudioQueueItem.fromChannelRooster(channelRooster, audioFileUniqueName);
                        //Could use channelRooster.getUpload_date(), but then can't use for purging files
                        deviceAudioQueueItem.setDate_uploaded(System.currentTimeMillis());
                        audioTableManager.insertChannelAudioFile(deviceAudioQueueItem);
                        Crashlytics.log("insertChannelAudioFile() completed");

                        Crashlytics.logException(new Throwable("Sync Adapter Channel Download Report"));

                        //Send broadcast message to notify all receivers of download finished
                        Intent intent = new Intent(Constants.ACTION_CHANNEL_DOWNLOAD_FINISHED);
                        getApplicationContext().sendBroadcast(intent);

                        //Notify any listeners
                        if(onChannelDownloadListener != null)
                            onChannelDownloadListener.onChannelDownloadComplete(true, channelRooster.getChannel_uid());

                        channelSyncActive = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
                        deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
                        channelSyncActive = false;
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
                    deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
                    channelSyncActive = false;
                }
            });

            if(BuildConfig.DEBUG) Toaster.makeToast(context, "I'm running", Toast.LENGTH_SHORT);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            //Show that an attempted download has occurred - this is used when "streaming" alarm content in AudioService
            deviceAlarmTableManager.updateAlarmLabel(Constants.ALARM_CHANNEL_DOWNLOAD_FAILED);
            channelSyncActive = false;
        }
    }

    private boolean isOversize(String fileUrl) {
        //Check for oversize files and report to Crashlytics
        final URL uri;
        try {
            uri = new URL(fileUrl);
        } catch(MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        URLConnection ucon;
        try {
            ucon = uri.openConnection();
            ucon.connect();
            final String contentLengthStr = ucon.getHeaderField("content-length");
            long contentLength = Long.valueOf(contentLengthStr);

            if(contentLength > Constants.MAX_ROOSTER_FILE_SIZE) {
                Crashlytics.log("File URL: " + fileUrl);
                Crashlytics.log("File size: " + String.valueOf(contentLength/1024));
                Crashlytics.logException(new Throwable("Sync Adapter Oversize File Report"));
            }

            // If file size is greater than absolute max, abort download
            if(contentLength > Constants.ABSOLUTE_MAX_FILE_SIZE) return true;
        } catch(final Exception e) {
            // File content length not readable
            Crashlytics.log("File URL: " + fileUrl);
            Crashlytics.log("Content length not readable!");
            Crashlytics.logException(new Throwable("Sync Adapter Oversize File Report"));
        } finally {
            try {
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Check and log device data usage
                int appUid = android.os.Process.myUid();
                long rxBytes = TrafficStats.getUidRxBytes(appUid);
                long txBytes = TrafficStats.getUidTxBytes(appUid);
                long previousRxBytes = sharedPreferences.getLong(Constants.APP_CUMULATIVE_RX_BYTES, 0);
                long previousTxBytes = sharedPreferences.getLong(Constants.APP_CUMULATIVE_TX_BYTES, 0);
                long appCumulativeRxBytes;
                long appCumulativeTxBytes;

                if (rxBytes > previousRxBytes)
                    appCumulativeRxBytes = previousRxBytes + (rxBytes - previousRxBytes);
                else appCumulativeRxBytes = previousRxBytes + rxBytes;

                if (txBytes > previousTxBytes)
                    appCumulativeTxBytes = previousTxBytes + (txBytes - previousTxBytes);
                else appCumulativeTxBytes = previousTxBytes + txBytes;

                // Clear cumulative data on first day of month
                if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1) {
                    appCumulativeRxBytes = 0;
                    appCumulativeTxBytes = 0;
                }

                editor.putLong(Constants.APP_CUMULATIVE_RX_BYTES, appCumulativeRxBytes);
                editor.putLong(Constants.APP_CUMULATIVE_TX_BYTES, appCumulativeTxBytes);
                editor.apply();

                Crashlytics.log("App cumulative RX MiB: " + appCumulativeRxBytes / 1000000);
                Crashlytics.log("App cumulative TX MiB: " + appCumulativeTxBytes / 1000000);
                Crashlytics.logException(new Throwable("App Cumulative Monthly Data Usage"));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void retrieveSocialRoosterAudio(final SocialRooster socialRooster, final Context context) {

        if(socialRooster == null) return;

        if(socialSyncActive) return;

        if(audioTableManager.isSocialAudioInDatabase(socialRooster.getQueue_id())) return;

        try {

            final StorageReference audioFileRef = mStorageRef.child("social_rooster_uploads/" + socialRooster.getAudio_file_url());
            final String audioFileUniqueName = Constants.FILENAME_PREFIX_ROOSTER_CONTENT + RoosterUtils.createRandomUID(5) + ".3gp";
            final DatabaseReference queueRecordReference = mDatabaseRef
                    .child("social_rooster_queue").child(firebaseUser.getUid()).child(socialRooster.getQueue_id());

            socialSyncActive = true;

            audioFileRef.getBytes(Constants.MAX_ROOSTER_FILE_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {

                    try {
                        FileOutputStream outputStream;
                        outputStream = context.openFileOutput(audioFileUniqueName, Context.MODE_PRIVATE);
                        outputStream.write(bytes);
                        outputStream.close();

                        //Create new object for storing in SQL db
                        DeviceAudioQueueItem deviceAudioQueueItem = new DeviceAudioQueueItem();
                        deviceAudioQueueItem.fromSocialRooster(socialRooster, audioFileUniqueName);

                        //store in local SQLLite database
                        if(audioTableManager.insertSocialAudioFile(deviceAudioQueueItem)) {
                            //remove record of queue from FB database
                            queueRecordReference.removeValue();
                        }

                        //Pre-cache image to display on alarm screen, in case no internet connection
                        if(!socialRooster.getProfile_pic().isEmpty()) Picasso.with(getApplicationContext()).load(socialRooster.getProfile_pic()).fetch();
                        socialSyncActive = false;
                    } catch (Exception e) {
                        e.printStackTrace();
                        socialSyncActive = false;
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                    Toaster.makeToast(context, "Error downloading rooster.", Toast.LENGTH_SHORT);
                    //remove record of queue from FB database on error
                    queueRecordReference.removeValue();
                    socialSyncActive = false;
                }
            });


            // For our recurring task, we'll just display a message
            if(BuildConfig.DEBUG) Toaster.makeToast(context, "I'm running", Toast.LENGTH_SHORT);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
