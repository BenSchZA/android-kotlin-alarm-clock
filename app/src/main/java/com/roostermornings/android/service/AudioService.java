/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.analytics.FA;
import com.roostermornings.android.sqlutil.AudioTableController;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.InternetHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.roostermornings.android.service.BackgroundTaskIntentService.startActionBackgroundDownload;

//Service to manage playing and pausing audio during Rooster alarm
public class AudioService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private static BroadcastReceiver receiver;
    private static final Timer timer = new Timer();
    private static final Timer downloadTimer = new Timer();
    private static final Timer volumeSoftStartTimer = new Timer();

    private final MediaPlayer mediaPlayerDefault = new MediaPlayer();
    private final MediaPlayer mediaPlayerRooster = new MediaPlayer();

    private DeviceAudioQueueItem audioItem;

    private final AudioService mThis = this;

    private ArrayList<DeviceAudioQueueItem> channelAudioItems;
    private ArrayList<DeviceAudioQueueItem> socialAudioItems;

    private static ArrayList<DeviceAudioQueueItem> audioItems = new ArrayList<>();
    private final AudioTableManager audioTableManager = new AudioTableManager(this);
    private final AudioTableController audioTableController = new AudioTableController(this);
    private final DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(this);

    private static DeviceAlarm alarm;
    private int alarmCycle = 1;
    private String alarmChannelUid;
    private String alarmUid;
    private int playDuration;
    private int alarmCount = 1;
    private int alarmPosition = 1;
    private File file;

    private int currentPositionRooster;

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
        BaseApplication baseApplication = new BaseApplication();
        baseApplication.getRoosterApplicationComponent().inject(this);
        return mBinder;
    }

    @Override
    public void onDestroy() {
        endService(null);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /** methods for clients */

    private void updateAlarmUI() {
        //Send broadcast message to notify all receivers of new data, in this case UI data
        Intent intent = new Intent(Constants.ACTION_ALARMDISPLAY);
        Bundle bundle = new Bundle();
        bundle.putSerializable("audioItem", audioItem);
        bundle.putInt("alarmPosition", alarmPosition);
        bundle.putInt("alarmCount", alarmCount);
        bundle.putBoolean("multipleAudioFiles", audioItems.size()>1);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    public void startAlarmContent(String alarmUid) {

        //Check if audio already playing
        try {
            if (mediaPlayerRooster.isPlaying() || mediaPlayerDefault.isPlaying()) {
                return;
            }
        } catch(IllegalStateException e) {
            e.printStackTrace();
        }

        alarm = deviceAlarmTableManager.getAlarmSet(alarmUid).get(0);
        this.alarmChannelUid = deviceAlarmTableManager.getAlarmSet(alarmUid).get(0).getChannel();
        this.alarmUid = alarmUid;

        //Check if Social and Channel alarm content exists, else startDefaultAlarmTone
        channelAudioItems = audioTableManager.extractAlarmChannelAudioFiles(mThis.alarmChannelUid);
        socialAudioItems = audioTableManager.extractSocialAudioFiles();

        FA.Log(FA.Event.Alarm_activated.class, FA.Event.Alarm_activated.Param.Channel_content_received, channelAudioItems.size());
        FA.Log(FA.Event.Alarm_activated.class, FA.Event.Alarm_activated.Param.Social_content_received, socialAudioItems.size());

        this.alarmCycle = 1;

        //Check if old content exists
        if(audioItems.size() > 0) {
            playAlarmRoosters();
            return;
        }

        //Broadcast receiver filter to receive download finished updates
        IntentFilter audioServiceChannelDownloadFinishedFilter = new IntentFilter();
        audioServiceChannelDownloadFinishedFilter.addAction(Constants.ACTION_CHANNEL_DOWNLOAD_FINISHED);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //Cancel download attempt timer and ensure BroadcastReceiver not re-triggered
                try {
                    if (downloadTimer != null) downloadTimer.cancel();
                } catch(IllegalArgumentException e) {
                    e.printStackTrace();
                } try {
                    if (receiver != null) unregisterReceiver(receiver);
                } catch(IllegalArgumentException e) {
                    e.printStackTrace();
                }

                //Check if old content exists
                if(audioItems.size() > 0) {
                    playAlarmRoosters();
                    return;
                }

                //Try append new content to end of existing content, if it fails - fail safe and play default alarm tone
                try {
                    //Reorder channel and social queue according to user preferences
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    if(sharedPreferences.getBoolean(Constants.USER_SETTINGS_ROOSTER_ORDER, false)) {
                        if (!mThis.channelAudioItems.isEmpty()) {
                            audioItems.addAll(channelAudioItems);
                        }
                        //If this alarm does not allow social roosters, move on to channel content
                        if (!mThis.socialAudioItems.isEmpty() && deviceAlarmTableManager.getAlarmSet(mThis.alarmUid).get(0).isSocial()) {
                            audioItems.addAll(socialAudioItems);
                        }
                    } else {
                        //If this alarm does not allow social roosters, move on to channel content
                        if (!mThis.socialAudioItems.isEmpty() && deviceAlarmTableManager.getAlarmSet(mThis.alarmUid).get(0).isSocial()) {
                            audioItems.addAll(socialAudioItems);
                        }
                        if (!mThis.channelAudioItems.isEmpty()) {
                            audioItems.addAll(channelAudioItems);
                        }
                    }
                    if (audioItems.isEmpty()) {
                        startDefaultAlarmTone(false);
                        return;
                    }
                } catch (NullPointerException e){
                    e.printStackTrace();
                    startDefaultAlarmTone(true);
                    return;
                }
                playAlarmRoosters();
            }
        }; registerReceiver(receiver, audioServiceChannelDownloadFinishedFilter);

        //Try to fetch un-downloaded channel content for 30 seconds if it doesn't already exist
        try {
            if (!"".equals(mThis.alarmChannelUid)
                    && !Constants.ALARM_CHANNEL_DOWNLOAD_FAILED.equals(alarm.getLabel())
                    && channelAudioItems == null) {

                FA.Log(FA.Event.Alarm_activated.class, FA.Event.Alarm_activated.Param.Alarm_content_stream, true);

                if (!InternetHelper.noInternetConnection(this)) {
                    //Download any social or channel audio files
                    startActionBackgroundDownload(mThis);
                    //Start alarm after 30 seconds or after download finished
                    startDownloadTimer();
                } else {
                    //Send broadcast message to notify all receivers of download finished
                    Intent intent = new Intent(Constants.ACTION_CHANNEL_DOWNLOAD_FINISHED);
                    sendBroadcast(intent);
                }
            } else {
                FA.Log(FA.Event.Alarm_activated.class, FA.Event.Alarm_activated.Param.Alarm_content_downloaded, true);

                //Send broadcast message to notify all receivers of download finished
                Intent intent = new Intent(Constants.ACTION_CHANNEL_DOWNLOAD_FINISHED);
                sendBroadcast(intent);
            }
        } catch(NullPointerException e) {
            e.printStackTrace();
            //Send broadcast message to notify all receivers of download finished
            Intent intent = new Intent(Constants.ACTION_CHANNEL_DOWNLOAD_FINISHED);
            sendBroadcast(intent);
        }
    }

    //Set timer to kill alarm after 5 minutes
    private void startDownloadTimer() {
        downloadTimer.schedule(new timerDownloadTask(), Constants.AUDIOSERVICE_DOWNLOAD_TASK_LIMIT);
    }

    private class timerDownloadTask extends TimerTask {
        public void run() {
            //Send broadcast message to notify all receivers of download finished
            Intent intent = new Intent(Constants.ACTION_CHANNEL_DOWNLOAD_FINISHED);
            sendBroadcast(intent);
        }
    }

    private void playAlarmRoosters() {

        //Show the number of social and channel roosters combined
        this.alarmCount = audioItems.size();

        //Check conditions for playing default tone: people must wake up!
        if (audioItems == null || audioItems.isEmpty()) {
            startDefaultAlarmTone(true);
            return;
        }

        if(currentPositionRooster < 1){
            alarmPosition = 0;
            playRooster(audioItems.get(0));
        } else{
            try {
                mediaPlayerRooster.seekTo(currentPositionRooster);
                mediaPlayerRooster.start();
                this.audioItem = audioItems.get(0);
                //Send broadcast to DeviceAlarmFullScreenActivity with UI data
                updateAlarmUI();
            } catch(NullPointerException e){
                e.printStackTrace();
            }
        }
    }

    private void playRooster(final DeviceAudioQueueItem audioItem) {
        foregroundNotification("Alarm content playing");

        mThis.audioItem = audioItem;

        //Increment alarmCycle, only loop rooster content 5 times
        //after which control handed to activity to endService and turn off screen
        if(alarmCycle > 5) {
            notifyActivityTimesUp();
            return;
        }

        //Set media player to alarm volume
        mediaPlayerRooster.setAudioStreamType(AudioManager.STREAM_ALARM);
        //Check stream volume above minimum
        checkStreamVolume();

        //Set alarm count display
        alarmPosition = audioItems.indexOf(mThis.audioItem) + 1;
        //Send broadcast to DeviceAlarmFullScreenActivity with UI data
        updateAlarmUI();

        file = new File(getFilesDir() + "/" + audioItem.getFilename());

        try {
            mediaPlayerRooster.reset();
            mediaPlayerRooster.setDataSource(file.getPath());

            mediaPlayerRooster.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayerRooster.start();
                    //Slowly increase volume from low to current volume
                    if(alarmPosition == 1 && alarmCycle == 1) softStartAudio();

                    mediaPlayerRooster.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            //playDuration used to check that audio has played and for specified period, otherwise set default alarm
                            mThis.playDuration += mediaPlayerRooster.getDuration();
                            //set audio file entry in SQL db as listened; to be removed when AudioService ends
                            audioTableManager.setListened(mThis.audioItem.getId());
                            //Check if at end of queue, else play next file
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
                    audioTableManager.setListened(mThis.audioItem.getId());
                    //Check if at end of queue, else play next file
                    if (audioItems.size() == audioItems.indexOf(audioItem) + 1) {
                        //If an error occurs on the last queue item, assume error on all and start default alarm tone as fail safe
                        startDefaultAlarmTone(true);
                    } else{
                        playRooster(getNextAudioItem());
                    }
                    return true;
                }
            });

            //Prepare mediaplayer on new thread: onCompletion or onError listener called
            mediaPlayerRooster.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
            //Social rooster will never play... let's not go here
            //delete file
            //delete record from AudioTable SQL DB
            audioTableManager.setListened(audioItem.getId());

            //delete record from arraylist
            audioItems.remove(audioItem);

            //If an error occurs on the last queue item, assume error on all and start default alarm tone as fail safe
            startDefaultAlarmTone(true);
        }
    }

    private DeviceAudioQueueItem getNextAudioItem(){
        if (audioItems.size() == audioItems.indexOf(audioItem) + 1) {
            return audioItems.get(0);
        } else {
            return audioItems.get(audioItems.indexOf(audioItem) + 1);
        }
    }

    private DeviceAudioQueueItem getPreviousAudioItem() {
        if (audioItems.indexOf(audioItem) - 1 < 0) {
            return audioItems.get(audioItems.size() - 1);
        } else{
            return audioItems.get(audioItems.indexOf(audioItem) - 1);
        }
    }

    public void skipNext() {
        try {
            if(mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) mediaPlayerRooster.stop();
            currentPositionRooster = 0;
            // set audio file entry in SQL db as listened; to be removed when AudioService ends
            audioTableManager.setListened(mThis.audioItem.getId());
            // play next rooster
            playRooster(getNextAudioItem());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void skipPrevious() {
        try {
            if(mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) mediaPlayerRooster.stop();
            currentPositionRooster = 0;
            // set audio file entry in SQL db as listened; to be removed when AudioService ends
            audioTableManager.setListened(mThis.audioItem.getId());
            // play previous rooster
            playRooster(getPreviousAudioItem());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void processListenedChannels() {
        //Ensure partially listened channels are removed and incremented
        try {
            if (mThis.audioItem.getType() == 1) {
                audioTableManager.setListened(mThis.audioItem.getId());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        //For all listened channels
        for (DeviceAudioQueueItem audioItem :
                audioTableManager.selectListened()) {
            try {
                if (audioItem.getType() == 1) {
                    //increment the current story iteration if it is a story
                    Integer currentStoryIteration = deviceAlarmTableManager.getChannelStoryIteration(audioItem.getQueue_id());
                    if (currentStoryIteration != null && currentStoryIteration > 0)
                        deviceAlarmTableManager.setChannelStoryIteration(audioItem.getQueue_id(), currentStoryIteration + 1);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public void dismissAlarm(ServiceConnection conn) {
        try {
            FA.Log(FA.Event.Alarm_dismissed.class,
                    FA.Event.Alarm_dismissed.Param.Alarm_activation_cycle_count,
                    alarmCycle);
            FA.Log(FA.Event.Alarm_dismissed.class,
                    FA.Event.Alarm_dismissed.Param.Alarm_activation_index,
                    audioItems.indexOf(audioItem) + 1);
            FA.Log(FA.Event.Alarm_dismissed.class,
                    FA.Event.Alarm_dismissed.Param.Alarm_activation_total_roosters,
                    channelAudioItems.size() + socialAudioItems.size());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        endService(conn);
    }

    private void endService(ServiceConnection conn) {
        processListenedChannels();
        //Delete record of all listened audio files
        for (DeviceAudioQueueItem audioItem :
             audioTableManager.selectListened()) {
            //Set the listened flag in firebase
            if(audioItem.getType() != 1) audioTableController.setListened(audioItem.getSender_id(), audioItem.getQueue_id());
            //Remove entry from SQL db
            audioTableManager.removeAudioEntry(audioItem);
        }

        //Delete all files not contained in SQL db
        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(Constants.FILENAME_PREFIX_ROOSTER_CONTENT);
            }
        });
        ArrayList<String> audioFileNames = audioTableManager.extractAllAudioFileNames();
        if(audioFileNames != null) {
            for (File file :
                    files) {
                if (!audioFileNames.contains(file.getName()))
                    file.delete();
            }
        }

        //delete record from arraylist
        audioItems.clear();
        //clear variables
        playDuration = 0;
        alarmCount = 0;
        alarmPosition = 0;
        currentPositionRooster = 0;
        //ensure no alarms still playing...
        stopVibrate();
        stopAlarmAudio();
        //unbind from and kill service
        stopForeground(true);
        try {
            this.unbindService(conn);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
        this.stopSelf();
    }

    //Set timer to kill alarm after 5 minutes
    private void startTimer() {
        timer.schedule(new timerTask(), Constants.ALARM_DEFAULTTIME);
    }

    private class timerTask extends TimerTask {
        public void run() {
            notifyActivityTimesUp();
        }
    }

    private void notifyActivityTimesUp() {
        //Send broadcast message to notify receiver to stop alarm and release wakelock
        Intent intent = new Intent(Constants.ACTION_ALARMTIMESUP);
        sendBroadcast(intent);
    }

    private void pauseRooster() {
        mediaPlayerRooster.pause();
        currentPositionRooster = mediaPlayerRooster.getCurrentPosition();
    }

    private void pauseDefaultAlarmTone() {
        mediaPlayerDefault.stop();
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String strSnoozeTime = sharedPreferences.getString(Constants.USER_SETTINGS_SNOOZE_TIME, "10");

        snoozeNotification("Alarm snoozed " + strSnoozeTime + " minutes - touch to dismiss");
        try {
            if (mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) pauseRooster();
        } catch(Exception e){
            e.printStackTrace();
            stopAlarmAudio();
        }

        try {
            if (mediaPlayerDefault != null && mediaPlayerDefault.isPlaying()) pauseDefaultAlarmTone();
        } catch(Exception e){
            e.printStackTrace();
            stopAlarmAudio();
        }

        //If vibrating then cancel
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.cancel();
        }

        try {
            FA.Log(FA.Event.Alarm_snoozed.class,
                    FA.Event.Alarm_snoozed.Param.Alarm_activation_cycle_count,
                    alarmCycle);
            FA.Log(FA.Event.Alarm_snoozed.class,
                    FA.Event.Alarm_snoozed.Param.Alarm_activation_index,
                    audioItems.indexOf(audioItem) + 1);
            FA.Log(FA.Event.Alarm_snoozed.class,
                    FA.Event.Alarm_snoozed.Param.Alarm_activation_total_roosters,
                    channelAudioItems.size() + socialAudioItems.size());
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void startDefaultAlarmTone(Boolean failure) {
        //Check that another alarm isn't already playing
        if(mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) return;
        if(mediaPlayerDefault != null && mediaPlayerDefault.isPlaying()) return;
        foregroundNotification("Alarm ringtone playing");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String strRingtonePreference = sharedPreferences.getString(Constants.USER_SETTINGS_DEFAULT_TONE, "android.intent.extra.ringtone.DEFAULT_URI");

        RingtoneManager ringtoneManager = new RingtoneManager(this);
        ringtoneManager.setType(RingtoneManager.TYPE_ALL);

        Uri notification = Uri.parse(strRingtonePreference);
        //In case alarm tone URI does not exist
        //Ringtonemanager used to check if exists: from Android docs:
        //If the given URI cannot be opened for any reason, this method will attempt to fallback on another sound.
        //If it cannot find any, it will return null.
        if(RingtoneManager.getRingtone(this, notification) == null || ringtoneManager.getRingtonePosition(notification) < 0) {
            notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (notification == null) {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
            }
        }

        //Check stream volume above minimum
        checkStreamVolume();

        //Start audio stream
        try {
            mediaPlayerDefault.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayerDefault.setDataSource(this, notification);
            mediaPlayerDefault.setLooping(true);
            mediaPlayerDefault.prepareAsync();

            mediaPlayerDefault.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayerDefault.start();
                    //Start timer to kill after 5 minutes
                    startTimer();
                }});

        } catch (Exception e) {
            e.printStackTrace();
            //If audio fails, vibrate instead - if that fails... well enjoy your sleep.
            startVibrate();
        }

        if(failure) {
            FA.Log(FA.Event.Alarm_activated.class, FA.Event.Alarm_activated.Param.Default_alarm_fail_safe, true);
        } else {
            FA.Log(FA.Event.Alarm_activated.class, FA.Event.Alarm_activated.Param.Default_alarm, true);
        }
    }

    private void checkStreamVolume() {
        //Ensure audio volume is acceptable TODO: what is acceptable?
        //Max volume seems to be 7
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        float percent = 0.5f;
        int minVolume = (int) (maxVolume*percent);
        if(currentVolume < minVolume) {
            audio.setStreamVolume(AudioManager.STREAM_ALARM, minVolume, 0);
        }
    }

    private void softStartAudio() {
        final AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        final int currentVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);

        final ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    int volume = 1;
                    public void run() {
                        if(volume > currentVolume) scheduler.shutdown();
                        audio.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
                        Log.d("Audio soft start: ", String.valueOf(volume));
                        volume++;
                    }
                }, 0, 1, TimeUnit.SECONDS);
    }

    private void stopAlarmAudio() {
        stopForeground(true);
        //If default tone or media playing then stop
        try {
            if (mediaPlayerDefault != null && mediaPlayerDefault.isPlaying()) {
                mediaPlayerDefault.stop();
                mediaPlayerDefault.release();
            }
        } catch(IllegalStateException e) {
                e.printStackTrace();
        }
        try {
            if (mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) {
                mediaPlayerRooster.stop();
                mediaPlayerRooster.release();
                currentPositionRooster = 0;
            }
        } catch(IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void foregroundNotification(String state) {

        Intent notificationIntent = new Intent(this, AudioService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification=new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Rooster Mornings")
                .setContentText(state)
                .setContentIntent(pendingIntent).build();

        startForeground(Constants.AUDIOSERVICE_NOTIFICATION_ID, notification);
    }

    private void snoozeNotification(String state) {
        Intent launchIntent = new Intent(this, MyAlarmsFragmentActivity.class);
        launchIntent.putExtra(Constants.EXTRA_ALARMID, alarmUid);
        launchIntent.setAction(Constants.ACTION_CANCEL_SNOOZE);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Rooster Mornings")
                .setContentText(state)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent).build();
        //Above not working
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        startForeground(Constants.AUDIOSERVICE_NOTIFICATION_ID, notification);
    }
}
