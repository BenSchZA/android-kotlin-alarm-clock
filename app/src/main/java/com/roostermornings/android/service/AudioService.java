/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import com.roostermornings.android.R;
import com.roostermornings.android.sqlutil.AudioTableController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.util.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

//Service to manage playing and pausing audio during Rooster alarm
public class AudioService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private static Timer timer = new Timer();

    MediaPlayer mediaPlayerDefault = new MediaPlayer();
    MediaPlayer mediaPlayerRooster = new MediaPlayer();

    private DeviceAudioQueueItem audioItem;

    protected AudioService mThis = this;

    ArrayList<DeviceAudioQueueItem> audioItems = new ArrayList<>();
    AudioTableManager audioTableManager = new AudioTableManager(this);
    AudioTableController audioTableController = new AudioTableController(this);
    DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(this);

    private int alarmCycle;
    private String alarmChannelUid;
    private String alarmUid;
    private int playDuration;
    private int alarmCount;
    private int alarmPosition;
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
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    public void startAlarmContent(String alarmUid) {
        this.alarmChannelUid = deviceAlarmTableManager.getAlarmSet(alarmUid).get(0).getChannel();
        this.alarmUid = alarmUid;

        //Check if Social and Channel alarm content exists, else startDefaultAlarmTone
        ArrayList<DeviceAudioQueueItem> channelAudioItems;
        channelAudioItems = audioTableManager.extractAlarmChannelAudioFiles(mThis.alarmChannelUid);
        ArrayList<DeviceAudioQueueItem> socialAudioItems;
        socialAudioItems = audioTableManager.extractSocialAudioFiles();

        this.alarmCycle = 0;

        //Check if old content exists
        if(audioItems.size() > 0) {
            playAlarmRoosters();
            return;
        }
        //Try append new content to end of existing content, if it fails - fail safe and play default alarm tone
        try {
            //If this alarm does not allow social roosters, move on to channel content
            if (!socialAudioItems.isEmpty() && deviceAlarmTableManager.getAlarmSet(this.alarmUid).get(0).isSocial()) {
                this.audioItems.addAll(socialAudioItems);
            }
            if (!channelAudioItems.isEmpty()) {
                this.audioItems.addAll(channelAudioItems);
            }
            if (this.audioItems.isEmpty()) {
                startDefaultAlarmTone();
                return;
            }
        } catch (NullPointerException e){
            e.printStackTrace();
            startDefaultAlarmTone();
            return;
        }
        playAlarmRoosters();
    }

    private void playAlarmRoosters() {

        //Show the number of social and channel roosters combined
        this.alarmCount = this.audioItems.size();

        //Check conditions for playing default tone: people must wake up!
        if (this.audioItems == null || this.audioItems.isEmpty()) {
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
        if(alarmCycle == 5) {
            notifyActivityTimesUp();
            return;
        }

        //Set media player to alarm volume
        mediaPlayerRooster.setAudioStreamType(AudioManager.STREAM_ALARM);
        //Check stream volume above minimum
        checkStreamVolume();

        file = new File(getFilesDir() + "/" + audioItem.getFilename());

        try {
            mediaPlayerRooster.reset();
            mediaPlayerRooster.setDataSource(file.getPath());

            mediaPlayerRooster.prepareAsync();
            mediaPlayerRooster.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayerRooster.start();
                    //Set alarm count display
                    alarmPosition = audioItems.indexOf(mThis.audioItem) + 1;
                    //Send broadcast to DeviceAlarmFullScreenActivity with UI data
                    updateAlarmUI();

                    mediaPlayerRooster.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            //playDuration used to check that audio has played and for specified period, otherwise set default alarm
                            mThis.playDuration += mediaPlayerRooster.getDuration();
                            //set audio file entry in SQL db as listened; to be removed when AudioService ends
                            audioTableManager.setListened(mThis.audioItem.getId());
                            //Check if at end of queue, else play next file
                            if (mThis.audioItems.size() == mThis.audioItems.indexOf(audioItem) + 1) {
                                mThis.alarmCycle++;
                                playRooster(mThis.audioItems.get(0));
                            } else{
                                playRooster(getNextAudioItem());
                            }
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            //Social rooster will never play... let's not go here
            //delete file
            //delete record from AudioTable SQL DB
            audioTableManager.setListened(audioItem.getId());

            //delete record from arraylist
            mThis.audioItems.remove(audioItem);
        }
    }

    private DeviceAudioQueueItem getNextAudioItem(){
        if (mThis.audioItems.size() == mThis.audioItems.indexOf(audioItem) + 1) {
            return mThis.audioItems.get(0);
        } else {
            return mThis.audioItems.get(mThis.audioItems.indexOf(audioItem) + 1);
        }
    }

    private DeviceAudioQueueItem getPreviousAudioItem() {
        if (mThis.audioItems.indexOf(audioItem) - 1 < 0) {
            return mThis.audioItems.get(mThis.audioItems.size() - 1);
        } else{
            return mThis.audioItems.get(mThis.audioItems.indexOf(audioItem) - 1);
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

    public void endService(ServiceConnection conn) {
        processListenedChannels();
        //delete record and file of all listened audio files
        for (DeviceAudioQueueItem audioItem :
             audioTableManager.selectListened()) {
            //Set the listened flag in firebase
            if(audioItem.getType() != 1) audioTableController.setListened(audioItem.getSender_id(), audioItem.getQueue_id());
            //Remove entry from SQL db
            audioTableManager.removeAudioEntry(audioItem);
            //Specifically delete file here to avoid loop - what happens if file note deleted and entry only removed on success?
            file = new File(getFilesDir() + "/" + audioItem.getFilename());
            file.delete();
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

    public void notifyActivityTimesUp() {
        //Send broadcast message to notify receiver to stop alarm and release wakelock
        Intent intent = new Intent(Constants.ACTION_ALARMTIMESUP);
        sendBroadcast(intent);
    }

    private void pauseRooster() {
        foregroundNotification("Alarm content paused");

        mediaPlayerRooster.pause();
        currentPositionRooster = mediaPlayerRooster.getCurrentPosition();
    }

    private void pauseDefaultAlarmTone() {
        foregroundNotification("Alarm tone paused");

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
        try {
            if (mediaPlayerRooster.isPlaying()) pauseRooster();
            if (mediaPlayerDefault.isPlaying()) pauseDefaultAlarmTone();
        } catch(NullPointerException e){
            e.printStackTrace();
            stopAlarmAudio();
        }
        //If vibrating then cancel
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
        }
    }

    public void startDefaultAlarmTone() {
        foregroundNotification("Alarm ringtone playing");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String strRingtonePreference = sharedPreferences.getString(Constants.USER_SETTINGS_DEFAULT_TONE, "DEFAULT_SOUND");

        Uri notification = Uri.parse(strRingtonePreference);
        //In case alarm tone URI does not exist
        if(RingtoneManager.getRingtone(this, notification) == null) {
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
                    startTimer();
                }});

        } catch (IOException e) {
            e.printStackTrace();
            //If audio fails, vibrate instead - if that fails... well enjoy your sleep.
            startVibrate();
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
                .setContentText("Rooster Mornings: " + state)
                .setContentIntent(pendingIntent).build();

        startForeground(Constants.AUDIOSERVICE_NOTIFICATION_ID, notification);
    }
}
