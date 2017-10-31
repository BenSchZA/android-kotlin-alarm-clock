/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.accounts.Account;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.DeviceAlarmFullScreenActivity;
import com.roostermornings.android.activity.MessageStatusFragmentActivity;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.adapter_data.ChannelManager;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.receiver.DeviceAlarmReceiver;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.InternetHelper;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.StrUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

//Service to manage playing and pausing audio during Rooster alarm
public class AudioService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private BroadcastReceiver endAudioServiceBroadcastReceiver;
    private BroadcastReceiver snoozeAudioServiceBroadcastReceiver;

    private Handler alarmFinishTimerHandler = new Handler();

    private final MediaPlayer mediaPlayerDefault = new MediaPlayer();
    private final MediaPlayer mediaPlayerRooster = new MediaPlayer();
    private final MediaPlayer streamMediaPlayer = new MediaPlayer();
    private Ringtone failsafeRingtone;

    private DeviceAudioQueueItem audioItem = new DeviceAudioQueueItem();

    private final AudioService mThis = this;

    Intent wakefulIntent = new Intent();
    Intent intent = new Intent();

    private ArrayList<DeviceAudioQueueItem> channelAudioItems = new ArrayList<>();
    private ArrayList<DeviceAudioQueueItem> socialAudioItems = new ArrayList<>();

    private static ArrayList<DeviceAudioQueueItem> audioItems = new ArrayList<>();
    private final AudioTableManager audioTableManager = new AudioTableManager(this);
    private final DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(this);
    private final DeviceAlarmController deviceAlarmController = new DeviceAlarmController(this);

    private static DeviceAlarm alarm = new DeviceAlarm();
    private int alarmCycle = 1;
    private String alarmChannelUid = "";
    private String alarmUid = "";
    private int alarmCount = 1;
    private int alarmPosition = 1;

    private int currentPositionRooster = 0;

    @Inject
    Account mAccount;
    @Inject SharedPreferences sharedPreferences;
    @Inject @Named("default") SharedPreferences defaultSharedPreferences;
    @Inject JSONPersistence jsonPersistence;
    @Inject
    ChannelManager channelManager;

    public static boolean mRunning = false;

    public AudioService() {
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public AudioService getService() {
            // Return this instance of AudioService so clients can call public methods
            return AudioService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method + ": From destroy.");

        finishTransaction();

        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRunning = false;

        BaseApplication.getRoosterApplicationComponent().inject(this);

        //Catch all uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        logError(e);
                    }
                }
        );

        failsafeRingtone = RingtoneManager.getRingtone(getBaseContext(),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

        //Register broadcast receivers for external access
        registerEndAudioServiceBroadcastReceiver();
        registerSnoozeAudioServiceBroadcastReceiver();

        //Start fullscreen alarm activation activity
        Intent intentAlarmFullscreen = new Intent(mThis, DeviceAlarmFullScreenActivity.class);
        intentAlarmFullscreen.addFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mThis.startActivity(intentAlarmFullscreen);
    }

    private void logError(Throwable e) {
        e.printStackTrace();
        FirebaseCrash.log(e.toString());
        Crashlytics.log(e.toString());
    }

    private void registerEndAudioServiceBroadcastReceiver() {
        endAudioServiceBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String method = Thread.currentThread().getStackTrace()[2].getMethodName();
                if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method + ": From receiver.");

                endService();
                //Start Rooster to show on wakeup
                Intent homeIntent = new Intent(mThis, MessageStatusFragmentActivity.class);
                homeIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_END_AUDIO_SERVICE);
        registerReceiver(endAudioServiceBroadcastReceiver, intentFilter);
    }

    private void registerSnoozeAudioServiceBroadcastReceiver() {
        snoozeAudioServiceBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                alarmCycle = 1;
                startAlarmContent(intent.getStringExtra(Constants.EXTRA_ALARMID));
                //Start fullscreen alarm activation activity
                Intent intentAlarmFullscreen = new Intent(mThis, DeviceAlarmFullScreenActivity.class);
                intentAlarmFullscreen.addFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mThis.startActivity(intentAlarmFullscreen);
            }
        };
        IntentFilter snoozeIntentFilter = new IntentFilter();
        snoozeIntentFilter.addAction(Constants.ACTION_SNOOZE_ACTIVATION);
        registerReceiver(snoozeAudioServiceBroadcastReceiver, snoozeIntentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Only attempt to start alarm once, in case of multiple conflicting alarms
        if (!mRunning) {
            mRunning = true;
            //Get wakeful intent of DeviceAlarmReceiver to enable finishing it in endService()
            if(intent != null) wakefulIntent = intent.getParcelableExtra(Constants.DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT);
            mThis.intent = intent;

            //If no audio playing already, start audio content, or default alarm tone
            try {
                if (!isAudioPlaying()) {
                    if(intent != null && StrUtils.notNullOrEmpty(intent.getStringExtra(Constants.EXTRA_ALARMID))) {
                        startAlarmContent(intent.getStringExtra(Constants.EXTRA_ALARMID));
                    } else {
                        startDefaultAlarmTone();
                    }
                }
            } catch(IllegalStateException e) {
                logError(e);
                startDefaultAlarmTone();
            }
        }

        return START_STICKY;
    }

    public void updateAlarmUI(Boolean defaultAlarm) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Send broadcast message to notify all receivers of new data, in this case UI data
        Intent intent = new Intent(Constants.ACTION_ALARMDISPLAY);
        if(!defaultAlarm) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("audioItem", audioItem);
            bundle.putInt("alarmPosition", alarmPosition);
            bundle.putInt("alarmCount", alarmCount);
            bundle.putBoolean("multipleAudioFiles", audioItems.size() > 1);
            intent.putExtras(bundle);
        }
        sendBroadcast(intent);
    }

    private Boolean isAudioPlaying() {
        try {
            if(mediaPlayerRooster.isPlaying()) return true;
        } catch (IllegalStateException e) {
            logError(e);
        }
        try {
            if(mediaPlayerDefault.isPlaying()) return true;
        } catch (IllegalStateException e) {
            logError(e);
        }
        try {
            if(streamMediaPlayer.isPlaying()) return true;
        } catch (IllegalStateException e) {
            logError(e);
        }
        return false;
    }

    private void startAlarmContent(String alarmUid) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Check if audio already playing
        if(isAudioPlaying()) return;

        //Check if old content exists
        if(audioItems.size() > 0) {
            playAlarmRoosters();
            return;
        }

        //Check if relevant alarm data exists
        if(StrUtils.notNullOrEmpty(alarmUid) && deviceAlarmTableManager.getAlarmSet(alarmUid) != null) {
            alarm = deviceAlarmTableManager.getAlarmSet(alarmUid).get(0);
            this.alarmChannelUid = alarm.getChannel();
            this.alarmUid = alarmUid;
        } else {
            startDefaultAlarmTone();
            return;
        }

        //Check if Social and Channel alarm content exists, else startDefaultAlarmTone
        channelAudioItems = audioTableManager.extractAlarmChannelAudioFiles(mThis.alarmChannelUid);
        socialAudioItems = audioTableManager.extractUnheardSocialAudioFiles();

        this.alarmCycle = 1;

        try {
            //If alarm channel is not empty and channel audioitems is empty, try download
            if (StrUtils.notNullOrEmpty(mThis.alarmChannelUid) && channelAudioItems.isEmpty()) {

                logAlarmActivation(false);

                if (!InternetHelper.noInternetConnection(this)) {
                    //Download any social or channel audio files
                    attemptContentUriRetrieval(alarm);
                } else {
                    compileAudioItemContent();
                }
            } else {
                logAlarmActivation(true);
                compileAudioItemContent();
            }
        } catch(NullPointerException e) {
            logError(e);
            compileAudioItemContent();
        }
    }

    private void logAlarmActivation(boolean dataLoaded) {
        if(channelAudioItems != null && socialAudioItems != null) {
            FA.LogMany(FA.Event.alarm_activated.class, new String[]{FA.Event.alarm_activated.Param.channel_content_received, FA.Event.alarm_activated.Param.social_content_received, FA.Event.alarm_activated.Param.data_loaded}, new Object[]{channelAudioItems.size(), socialAudioItems.size(), dataLoaded});
        } else if(channelAudioItems != null) {
            FA.LogMany(FA.Event.alarm_activated.class, new String[]{FA.Event.alarm_activated.Param.channel_content_received, FA.Event.alarm_activated.Param.data_loaded}, new Object[]{channelAudioItems.size(), dataLoaded});
        } else if(socialAudioItems != null) {
            FA.LogMany(FA.Event.alarm_activated.class, new String[]{FA.Event.alarm_activated.Param.social_content_received, FA.Event.alarm_activated.Param.data_loaded}, new Object[]{socialAudioItems.size(), dataLoaded});
        }
    }

    private void compileAudioItemContent() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //If no content exists to add to audioItems, then return now
        if(mThis.socialAudioItems.isEmpty() && mThis.channelAudioItems.isEmpty()) {
            startDefaultAlarmTone();
            return;
        }

        //Try append new content to end of existing content, if it fails - fail safe and play default alarm tone
        try {
            //Reorder channel and social queue according to user preferences
            if(defaultSharedPreferences.getBoolean(Constants.USER_SETTINGS_ROOSTER_ORDER, false)) {
                if (!mThis.channelAudioItems.isEmpty()) {
                    audioItems.addAll(channelAudioItems);
                }
                //If this alarm does not allow social roosters, move on to channel content
                if (!mThis.socialAudioItems.isEmpty() && alarm.isSocial()) {
                    audioItems.addAll(socialAudioItems);
                }
            } else {
                //If this alarm does not allow social roosters, move on to channel content
                if (!mThis.socialAudioItems.isEmpty() && alarm.isSocial()) {
                    audioItems.addAll(socialAudioItems);
                }
                if (!mThis.channelAudioItems.isEmpty()) {
                    audioItems.addAll(channelAudioItems);
                }
            }
            if (audioItems.isEmpty()) {
                startDefaultAlarmTone();
                return;
            }
        } catch (NullPointerException e){
            logError(e);
            startDefaultAlarmTone();
            return;
        }
        playAlarmRoosters();
    }

    private void attemptContentUriRetrieval(final DeviceAlarm deviceAlarm) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        try {
            //Check if channel has a valid ID, else next pending alarm has no channel
            final String channelId = deviceAlarm.getChannel();
            if (!StrUtils.notNullOrEmpty(channelId)) {
                startDefaultAlarmTone();
                return;
            }

            final ArrayList<ChannelRooster> channelRoosters = jsonPersistence.getNewAlarmChannelRoosters();

            ChannelManager.Companion.setOnFlagChannelManagerDataListener(new ChannelManager.Companion.OnFlagChannelManagerDataListener() {
                @Override
                public void onChannelRoosterDataChanged(@NotNull ArrayList<ChannelRooster> freshChannelRoosters) {
                    channelRoosters.clear();
                    channelRoosters.addAll(freshChannelRoosters);
                }

                @Override
                public void onSyncFinished() {
                    for (ChannelRooster channelRooster:
                            channelRoosters) {
                        if(channelId.equals(channelRooster.channel_uid)) {
                            streamChannelContent(channelRooster.getAudio_file_url());
                            return;
                        }
                    }
                    startDefaultAlarmTone();
                }
            });
            channelManager.refreshChannelData(channelRoosters);

        } catch(Exception e) {
            logError(e);
            startDefaultAlarmTone();
        }
    }

    private void streamChannelContent(final String url) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        foregroundNotification("Alarm content streaming");

        //Check that URL is notNullOrEmpty
        if(!StrUtils.notNullOrEmpty(url)) {
            startDefaultAlarmTone();
            return;
        }

        //Check if audio already playing
        if(isAudioPlaying()) return;

        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
        final StorageReference audioFileRef = mStorageRef.getStorage().getReferenceFromUrl(url);

        audioFileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
        {
            @Override
            public void onSuccess(Uri downloadUrl)
            {
                try {
                    streamMediaPlayer.reset();
                    streamMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
//                        AudioAttributesCompat.Builder builder = new AudioAttributesCompat.Builder()
//                                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
//                                .setFlags(FLAG_AUDIBILITY_ENFORCED)
//                                .setUsage(USAGE_ALARM);
//                    builder.build();
//                    if()
//                    streamMediaPlayer.setAudioAttributes();
                    streamMediaPlayer.setLooping(true);
                    streamMediaPlayer.setDataSource(downloadUrl.toString());
                } catch (Exception e) {
                    logError(e);
                    startDefaultAlarmTone();
                    return;
                }
                streamMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if(streamMediaPlayer.isPlaying()) return;
                        streamMediaPlayer.start();
                        checkStreamVolume(AudioManager.STREAM_ALARM);
                        softStartAudio();

                        streamMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mThis.alarmCycle++;
                                //Increment alarmCycle, only loop rooster content 5 times
                                //after which control handed to activity to endService and turn off screen
                                if(alarmCycle > 5) {
                                    notifyActivityTimesUp();
                                    streamMediaPlayer.stop();
                                }
                            }
                        });
                    }
                });

                streamMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                        streamMediaPlayer.reset();
                        startDefaultAlarmTone();
                        return true;
                    }
                });

                streamMediaPlayer.prepareAsync();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                streamMediaPlayer.reset();
                startDefaultAlarmTone();
            }
        });
    }

    private void playAlarmRoosters() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Check if audio already playing
        if(isAudioPlaying()) return;

        //Show the number of social and channel roosters combined
        this.alarmCount = audioItems.size();

        //Check conditions for playing default tone: people must wake up!
        if (audioItems == null || audioItems.isEmpty()) {
            startDefaultAlarmTone();
            return;
        }

        if(currentPositionRooster < 1){
            alarmPosition = 0;
            playRooster(audioItems.get(0));
        } else{
            try {
                mediaPlayerRooster.seekTo(currentPositionRooster);
                mediaPlayerRooster.start();
                //Slowly increase volume from low to current volume
                softStartAudio();
                //Send broadcast to DeviceAlarmFullScreenActivity with UI data
                updateAlarmUI(false);
            } catch(NullPointerException e){
                logError(e);
                playRooster(audioItems.get(0));
            }
        }
    }

    private void playRooster(final DeviceAudioQueueItem audioItem) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        foregroundNotification("Alarm content playing");

        mThis.audioItem = audioItem;

        if(audioItem == null) {
            startDefaultAlarmTone();
            return;
        }

        //Increment alarmCycle, only loop rooster content 5 times
        //after which control handed to activity to endService and turn off screen
        if(alarmCycle > 5) {
            notifyActivityTimesUp();
            return;
        }

        //Set media player to alarm volume
        mediaPlayerRooster.setAudioStreamType(AudioManager.STREAM_ALARM);
        //Check stream volume above minimum
        checkStreamVolume(AudioManager.STREAM_ALARM);

        //Set alarm count display
        alarmPosition = audioItems.indexOf(mThis.audioItem) + 1;
        //Send broadcast to DeviceAlarmFullScreenActivity with UI data
        updateAlarmUI(false);

        File file = new File(getFilesDir() + "/" + audioItem.getFilename());

        try {
            //AudioService report logging
            Crashlytics.log("Rooster file path: " + file.getPath());
            Crashlytics.log("Rooster is file?: " + String.valueOf(file.isFile()));
            Crashlytics.log("Rooster type: " + String.valueOf(audioItem.getType()));
            if(audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL && StrUtils.notNullOrEmpty(alarm.getChannel())) Crashlytics.log("Rooster channel source: " + alarm.getChannel());
            Crashlytics.log("Rooster file size (kb): " + String.valueOf(file.length()/1024));

            mediaPlayerRooster.reset();
            mediaPlayerRooster.setDataSource(file.getPath());

            mediaPlayerRooster.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayerRooster.start();

                    if(alarmPosition == 1 && alarmCycle == 1) {
                        //Slowly increase volume from low to current volume
                        softStartAudio();
                        if(audioItem.getType() == Constants.AUDIO_TYPE_SOCIAL) FA.Log(FA.Event.social_rooster_unique_play.class, null, null);
                        if(audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) FA.Log(FA.Event.channel_unique_play.class, FA.Event.channel_unique_play.Param.channel_title, audioItem.getQueue_id());
                        if(audioItem.getType() == Constants.AUDIO_TYPE_SOCIAL) FA.Log(FA.Event.social_rooster_play.class, null, null);
                        if(audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) FA.Log(FA.Event.channel_play.class, FA.Event.channel_unique_play.Param.channel_title, audioItem.getQueue_id());
                    } else {
                        if(audioItem.getType() == Constants.AUDIO_TYPE_SOCIAL) FA.Log(FA.Event.social_rooster_play.class, null, null);
                        if(audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) FA.Log(FA.Event.channel_play.class, FA.Event.channel_unique_play.Param.channel_title, audioItem.getQueue_id());
                    }

                    mediaPlayerRooster.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            //set audio file entry in SQL db as listened; to be removed when AudioService ends
                            audioTableManager.setListened(mThis.audioItem);
                            //Check if at end of queue, else play next file
                            if(audioItems.isEmpty()) {
                                startDefaultAlarmTone();
                                return;
                            }
                            if (audioItems.size() == audioItems.indexOf(audioItem) + 1) {
                                playRooster(audioItems.get(0));
                                mThis.alarmCycle++;
                            } else{
                                playRooster(getNextAudioItem());
                            }
                        }
                    });
                }
            });

            mediaPlayerRooster.setOnErrorListener(new MediaPlayer.OnErrorListener(){
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                    //set audio file entry in SQL db as listened; to be removed when AudioService ends
                    audioTableManager.setListened(mThis.audioItem);
                    //Check if at end of queue, else play next file
                    if(audioItems.isEmpty()) {
                        startDefaultAlarmTone();
                        return true;
                    }
                    if (audioItems.size() == audioItems.indexOf(audioItem) + 1) {
                        //If an error occurs on the last queue item, assume error on all and start default alarm tone as fail safe
                        startDefaultAlarmTone();
                    } else{
                        playRooster(getNextAudioItem());
                    }
                    return true;
                }
            });

            //Prepare mediaplayer on new thread: onCompletion or onError listener called
            mediaPlayerRooster.prepareAsync();

        } catch (IOException e) {
            logError(e);
            //Social rooster will never play... let's not go here
            //delete file
            //delete record from AudioTable SQL DB
            audioTableManager.setListened(audioItem);

            //delete record from arraylist
            audioItems.remove(audioItem);

            //If an error occurs on the last queue item, assume error on all and start default alarm tone as fail safe
            startDefaultAlarmTone();
        }
    }

    private DeviceAudioQueueItem getNextAudioItem(){
        //Return next valid audio item in array
        if (audioItems.size() == audioItems.indexOf(audioItem) + 1) {
            return audioItems.get(0);
        } else {
            return audioItems.get(audioItems.indexOf(audioItem) + 1);
        }
    }

    private DeviceAudioQueueItem getPreviousAudioItem() {
        //Return previous valid audio item in array
        if (audioItems.indexOf(audioItem) - 1 < 0) {
            return audioItems.get(audioItems.size() - 1);
        } else{
            return audioItems.get(audioItems.indexOf(audioItem) - 1);
        }
    }

    public void skipNext() {
        if(audioItems.isEmpty()) {
            startDefaultAlarmTone();
            return;
        }
        try {
            if(mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) mediaPlayerRooster.stop();
            currentPositionRooster = 0;
            // set audio file entry in SQL db as listened; to be removed when AudioService ends
            audioTableManager.setListened(mThis.audioItem);
            // play next rooster
            playRooster(getNextAudioItem());
        } catch (NullPointerException e) {
            logError(e);
        }
    }

    public void skipPrevious() {
        if(audioItems.isEmpty()) {
            startDefaultAlarmTone();
            return;
        }
        try {
            if(mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) mediaPlayerRooster.stop();
            currentPositionRooster = 0;
            // set audio file entry in SQL db as listened; to be removed when AudioService ends
            audioTableManager.setListened(mThis.audioItem);
            // play previous rooster
            playRooster(getPreviousAudioItem());
        } catch (NullPointerException e) {
            logError(e);
        }
    }

    public void processAudioEntries() {
        //Ensure partially listened channels and roosters are set as listened
        try {
            if (mThis.audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) {
                audioTableManager.setListened(mThis.audioItem);
            } else if (mThis.audioItem.getType() == Constants.AUDIO_TYPE_SOCIAL) {
                audioTableManager.setListened(mThis.audioItem);
            }
        } catch (NullPointerException e) {
            logError(e);
        }
        //For all listened channels
        for (DeviceAudioQueueItem audioItem :
                audioTableManager.selectListenedByChannel(alarm.getChannel())) {
            try {
                if (audioItem.getType() == Constants.AUDIO_TYPE_CHANNEL) {
                    channelManager.incrementChannelStoryIteration(audioItem.getQueue_id());
                }
            } catch (NullPointerException e) {
                logError(e);
            }
        }
    }

    private void purgeAudioFiles() {
        //Delete all files not contained in SQL db
        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(Constants.FILENAME_PREFIX_ROOSTER_CONTENT);
            }
        });
        ArrayList<String> audioFileNames = audioTableManager.extractAllAudioFileNames();
        if (audioFileNames != null) {
            for (File file :
                    files) {
                if (!audioFileNames.contains(file.getName()))
                    file.delete();
            }
        }
    }

    public void dismissAlarm() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Log status of alarm activation to Firebase event
        try {
            FA.Log(FA.Event.alarm_dismissed.class,
                    FA.Event.alarm_dismissed.Param.alarm_activation_cycle_count,
                    alarmCycle);
            FA.Log(FA.Event.alarm_dismissed.class,
                    FA.Event.alarm_dismissed.Param.alarm_activation_index,
                    audioItems.indexOf(audioItem) + 1);
            FA.Log(FA.Event.alarm_dismissed.class,
                    FA.Event.alarm_dismissed.Param.alarm_activation_total_roosters,
                    channelAudioItems.size() + socialAudioItems.size());
        } catch (NullPointerException e) {
            logError(e);
        }

        endService();
    }

    private void finishTransaction() {
        //AudioService report logging
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);
        Crashlytics.logException(new Throwable("AudioService Report"));

        //Process audio files - set channel persisted iterations,
        // delete all channel audio files/SQL entries,
        // remove files not contained in SQL db
        processAudioEntries();
        purgeAudioFiles();

        //Unregister all broadcastreceivers
        try {
            if (endAudioServiceBroadcastReceiver != null)
                unregisterReceiver(endAudioServiceBroadcastReceiver);
        } catch(IllegalArgumentException e) {
            logError(e);
        }
        try {
            if (snoozeAudioServiceBroadcastReceiver != null)
                unregisterReceiver(snoozeAudioServiceBroadcastReceiver);
        } catch(IllegalArgumentException e) {
            logError(e);
        }

        //Delete audio records from arraylist
        audioItems.clear();
        //Clear variables
        alarmCount = 0;
        alarmPosition = 0;
        currentPositionRooster = 0;

        //Release media player instances
        releaseMediaPlayers();
    }

    public void endService() {
        //https://stackoverflow.com/questions/17146822/when-is-a-started-and-bound-service-destroyed

        //AudioService report logging
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Ensure no alarms still playing...
        stopVibrate();
        stopAlarmAudio();

        //Clear timer callbacks - this timer puts a limit on default or streamed audio duration, whereas normal operation is limited to 5 cycles
        alarmFinishTimerHandler.removeCallbacks(alarmFinishTimerRunnable);

        //Clear forground notifications that attempt to keep service alive
        stopForeground(true);

        //Complete DeviceAlarmReceiver wakeful intent
        if (wakefulIntent != null) {
            DeviceAlarmReceiver.completeWakefulIntent(wakefulIntent);
        }

        //Stop service
        this.stopSelf();
    }

    //Set timer to kill alarm after 5 minutes
    private void startTimer() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        alarmFinishTimerHandler.postDelayed(alarmFinishTimerRunnable, Constants.ALARM_DEFAULTTIME);
    }

    //Set timer to kill alarm after 5 minutes
    private Runnable alarmFinishTimerRunnable = new Runnable() {
        @Override
        public void run() {
            notifyActivityTimesUp();
        }
    };

    private void notifyActivityTimesUp() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Send broadcast message to notify receiver of end of alarm to clear window hold
        Intent intent = new Intent(Constants.ACTION_ALARMTIMESUP);
        sendBroadcast(intent);
        dismissAlarm();
    }

    private void startVibrate() {
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        long[] vibratePattern = Constants.VIBRATE_PATTERN;
        int vibrateRepeat = 2;
        vibrator.vibrate(vibratePattern, vibrateRepeat);
    }

    private void stopVibrate() {
        //If vibrating then cancel
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
        }
    }

    public void snoozeAudioState(){
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        alarmFinishTimerHandler.removeCallbacks(alarmFinishTimerRunnable);

        deviceAlarmController.snoozeAlarm(alarmUid, false);

        String strSnoozeTime = defaultSharedPreferences.getString(Constants.USER_SETTINGS_SNOOZE_TIME, "10");

        snoozeNotification("Alarm snoozed " + strSnoozeTime + " minutes - touch to dismiss");
        try {
            if (mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) {
                mediaPlayerRooster.pause();
                currentPositionRooster = mediaPlayerRooster.getCurrentPosition();
            }
        } catch(Exception e){
            logError(e);
            stopAlarmAudio();
        }

        try {
            if (streamMediaPlayer != null && streamMediaPlayer.isPlaying()) {
                streamMediaPlayer.stop();
            }
        } catch(Exception e){
            logError(e);
            stopAlarmAudio();
        }

        try {
            if (mediaPlayerDefault != null && mediaPlayerDefault.isPlaying()) {
                mediaPlayerDefault.stop();
            }
        } catch(Exception e){
            logError(e);
            stopAlarmAudio();
        }

        //Stop default audio fail-safe
        stopFailsafe();

        //If vibrating then cancel
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.cancel();
        }

        try {
            if(!audioItems.isEmpty()) {
                FA.LogMany(FA.Event.alarm_snoozed.class,
                        new String[]{FA.Event.alarm_snoozed.Param.alarm_activation_cycle_count, FA.Event.alarm_snoozed.Param.alarm_activation_index, FA.Event.alarm_snoozed.Param.alarm_activation_total_roosters},
                        new Object[]{alarmCycle, audioItems.indexOf(audioItem) + 1, channelAudioItems.size() + socialAudioItems.size()});
            }
        } catch (NullPointerException e) {
            logError(e);
        }
    }

    private void startDefaultAlarmTone() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //If channel UID attached to alarm, content should have played
        final Boolean failure = StrUtils.notNullOrEmpty(alarm.getChannel()) && !InternetHelper.noInternetConnection(this);

        try {
            //Check if audio already playing
            if(isAudioPlaying()) return;

            foregroundNotification("Alarm ringtone playing");

            updateAlarmUI(true);

            String strRingtonePreference = defaultSharedPreferences.getString(Constants.USER_SETTINGS_DEFAULT_TONE, "android.intent.extra.ringtone.DEFAULT_URI");

            RingtoneManager ringtoneManager = new RingtoneManager(this);
            ringtoneManager.setType(RingtoneManager.TYPE_ALL);

            Uri notification = Uri.parse(strRingtonePreference);
            //In case alarm tone URI does not exist
            //Ringtonemanager used to check if exists: from Android docs:
            //If the given URI cannot be opened for any reason, this method will attempt to fallback on another sound.
            //If it cannot find any, it will return null.
            if (RingtoneManager.getRingtone(this, notification) == null || ringtoneManager.getRingtonePosition(notification) < 0) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (notification == null) {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    if (notification == null) {
                        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    }
                }
            }

            //Check stream volume above minimum
            checkStreamVolume(AudioManager.STREAM_ALARM);

            //Start audio stream
            try {
                mediaPlayerDefault.reset();
                mediaPlayerDefault.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayerDefault.setDataSource(this, notification);
                mediaPlayerDefault.setLooping(true);

                mediaPlayerDefault.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
                        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method + " prepared listener");

                        if (mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) return;
                        mediaPlayerDefault.start();
                        //Start timer to kill after 5 minutes
                        startTimer();
                        //Log whether a failure or just default alarm tone selected
                        logDefaultRingtoneState(failure);
                    }
                });
                mediaPlayerDefault.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
                        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method + " error listener");

                        startFailsafe();
                        return true;
                    }
                });

                //Must be called after listeners created
                mediaPlayerDefault.prepareAsync();

            } catch (Exception e) {
                method = Thread.currentThread().getStackTrace()[2].getMethodName();
                if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method + " catch 1");
                logError(e);

                startFailsafe();
            }
        } catch (Exception e) {
            method = Thread.currentThread().getStackTrace()[2].getMethodName();
            if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method + " catch 2");
            logError(e);

            startFailsafe();
        }
    }

    private void startFailsafe() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //If this method is called, we should log alarm as a failure
        logDefaultRingtoneState(true);

        checkStreamVolume(AudioManager.STREAM_RING);
        //Do everything possible to wake user up at this stage
        startVibrate();

        //Start failsafe ringtone
        if(failsafeRingtone == null) {
            Crashlytics.log("Is null");
            failsafeRingtone = RingtoneManager.getRingtone(getBaseContext(),
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            Crashlytics.log(failsafeRingtone.getTitle(getBaseContext()));
            failsafeRingtone.play();
        }
        //Check if playing, else try again
        if(failsafeRingtone != null && !failsafeRingtone.isPlaying()) {
            Crashlytics.log("Not null and not playing " + failsafeRingtone.getTitle(getBaseContext()));
            failsafeRingtone.play();
        }
    }

    private void stopFailsafe() {
        if(failsafeRingtone != null && failsafeRingtone.isPlaying()) failsafeRingtone.stop();
    }

    private void logDefaultRingtoneState(boolean failure) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method + " Failure:" + String.valueOf(failure));

        //Log a firebase analytics event indicating whether an attempt was/should have been made to play audio content
        FA.LogMany(FA.Event.default_alarm_play.class,
                new String[]{FA.Event.default_alarm_play.Param.attempt_to_play, FA.Event.default_alarm_play.Param.fatal_failure},
                new Object[]{!socialAudioItems.isEmpty() || !channelAudioItems.isEmpty(), failure});

        if (failure) {
            //Show dialog explainer again by clearing shared pref
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false);
            editor.apply();
        }
    }

    private void checkStreamVolume(int streamType) {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Ensure audio volume is acceptable - as set in user settings
        //Max volume seems to be integer 7
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(streamType);
        int maxVolume = audio.getStreamMaxVolume(streamType);
        int minVolume = (int) (maxVolume*returnUserSettingAlarmMinimumVolume());
        if(currentVolume < minVolume) {
            audio.setStreamVolume(streamType, minVolume, 0);
        }
    }

    private float returnUserSettingAlarmMinimumVolume() {
        //Ensure alarm volume at least equal to user's minimum alarm volume setting
        try {
            String[] alarmVolumeArrayEntries = getResources().getStringArray(R.array.user_settings_alarm_volume_entry_values);

            float alarmMinimumVolume = 0.4f;

            if (alarmVolumeArrayEntries[0].equals(defaultSharedPreferences.getString(Constants.USER_SETTINGS_ALARM_VOLUME, ""))) {
                alarmMinimumVolume = 0.0f;
            } else if (alarmVolumeArrayEntries[1].equals(defaultSharedPreferences.getString(Constants.USER_SETTINGS_ALARM_VOLUME, ""))) {
                alarmMinimumVolume = 0.2f;
            } else if (alarmVolumeArrayEntries[2].equals(defaultSharedPreferences.getString(Constants.USER_SETTINGS_ALARM_VOLUME, ""))) {
                alarmMinimumVolume = 0.4f;
            } else if (alarmVolumeArrayEntries[3].equals(defaultSharedPreferences.getString(Constants.USER_SETTINGS_ALARM_VOLUME, ""))) {
                alarmMinimumVolume = 0.6f;
            } else if (alarmVolumeArrayEntries[4].equals(defaultSharedPreferences.getString(Constants.USER_SETTINGS_ALARM_VOLUME, ""))) {
                alarmMinimumVolume = 0.8f;
            } else if (alarmVolumeArrayEntries[5].equals(defaultSharedPreferences.getString(Constants.USER_SETTINGS_ALARM_VOLUME, ""))) {
                alarmMinimumVolume = 1.0f;
            } else {
                alarmMinimumVolume = 0.4f;
            }

            return alarmMinimumVolume;
        } catch (NullPointerException e) {
            logError(e);
            return 0.4f;
        }
    }

    private void softStartAudio() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        //Ramp up the audio linearly
        // 3    /-------------------
        // 2   /
        // 1  /
        // 0 /

        final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        final int currentVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);
        float timePeriod = (float) Integer.valueOf(defaultSharedPreferences.getString(Constants.USER_SETTINGS_ALARM_VOLUME_INCREMENT_DURATION, "5"));
        float tempTimeInterval = (timePeriod/(float)currentVolume)*1000;
        final float timeInterval = tempTimeInterval > 100 ? tempTimeInterval : 100;

        final ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    int volume = 1;
                    public void run() {
                        if(volume > currentVolume) {
                            volume = currentVolume;
                            scheduler.shutdown();
                        }
                        audio.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
                        volume++;
                    }
                }, 0, (long)timeInterval, TimeUnit.MILLISECONDS);
    }

    private void stopAlarmAudio() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        stopForeground(true);
        //If default tone or media playing then stop

        //Stop default audio fail-safe
        stopFailsafe();

        try {
            if (mediaPlayerDefault != null && mediaPlayerDefault.isPlaying()) {
                mediaPlayerDefault.stop();
            }
        } catch(IllegalStateException e) {
            logError(e);
        }
        try {
            if (mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) {
                mediaPlayerRooster.stop();
                currentPositionRooster = 0;
            }
        } catch(IllegalStateException e) {
            logError(e);
        }
        try {
            if (streamMediaPlayer != null && streamMediaPlayer.isPlaying()) {
                streamMediaPlayer.stop();
            }
        } catch(IllegalStateException e) {
            logError(e);
        }
    }

    private void releaseMediaPlayers() {
        String method = Thread.currentThread().getStackTrace()[2].getMethodName();
        if(StrUtils.notNullOrEmpty(method)) Crashlytics.log(method);

        mediaPlayerDefault.release();
        mediaPlayerRooster.release();
        streamMediaPlayer.release();
    }

    private void foregroundNotification(String state) {
        //Notification used to attempt to stop Android OS from killing service, and for user feedback

        Intent launchIntent = new Intent(this, MyAlarmsFragmentActivity.class);
        launchIntent.putExtra(Constants.EXTRA_ALARMID, alarmUid);
        launchIntent.setAction(Constants.ACTION_CANCEL_SNOOZE);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent broadcastIntent = new Intent(Constants.ACTION_END_AUDIO_SERVICE);
        PendingIntent broadcastPendingIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(state)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(broadcastPendingIntent).build();

        startForeground(Constants.AUDIOSERVICE_NOTIFICATION_ID, notification);
    }

    private void snoozeNotification(String state) {
        //Notification used to display snooze state and clear snooze state

        Intent launchIntent = new Intent(this, MyAlarmsFragmentActivity.class);
        launchIntent.putExtra(Constants.EXTRA_ALARMID, alarmUid);
        launchIntent.setAction(Constants.ACTION_CANCEL_SNOOZE);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(state)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pendingIntent).build();
        //Above not working
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        startForeground(Constants.AUDIOSERVICE_NOTIFICATION_ID, notification);
    }
}
