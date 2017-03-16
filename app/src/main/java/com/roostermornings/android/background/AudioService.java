/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.background;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roostermornings.android.R;
import com.roostermornings.android.domain.DeviceAudioQueueItem;
import com.roostermornings.android.sqlutil.AudioTableManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private final static int NOTIFICATION_ID = 1000;

    private MediaPlayer mediaPlayerDefault;
    private MediaPlayer mediaPlayerRooster;

    private DeviceAudioQueueItem audioItem;

    List<DeviceAudioQueueItem> audioItems = new ArrayList<>();
    AudioTableManager audioTableManager = new AudioTableManager(this);

    private int playDuration;
    private int alarmCount;
    private int alarmPosition;

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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /** methods for clients */

    public void updateAlarmUI() {
        //Send broadcast message to notify all receivers of new data, in this case UI data
        Intent intent = new Intent("rooster.update.ALARMDISPLAY");
        Bundle bundle = new Bundle();
        bundle.putSerializable("audioItem", audioItem);
        bundle.putInt("alarmPosition", alarmPosition);
        bundle.putInt("alarmCount", alarmCount);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    public void startAlarmSocialRoosters() {

        audioItems = audioTableManager.extractAudioFiles();
        alarmCount = audioItems.size();

        if(currentPositionRooster < 1){
            if (audioItems == null || audioItems.size() == 0) {
                //Check conditions for playing default tone: people must wake up!
                if (playDuration < 5000 && (audioItems == null || audioItems.size() == 0)) {
                    startDefaultAlarmTone();
                }
                return;
            }
            alarmPosition = 0;
            playSocialRooster(audioItems.get(0));
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

    public void playSocialRooster(final DeviceAudioQueueItem audioItem) {
        audioServiceForegroundNotification("Social Roosters playing");

        this.audioItem = audioItem;

        mediaPlayerRooster = new MediaPlayer();
        //Set media player to alarm volume
        mediaPlayerRooster.setAudioStreamType(AudioManager.STREAM_ALARM);

        final File file = new File(getFilesDir() + "/" + audioItem.getFilename());

        try {
            mediaPlayerRooster.setDataSource(file.getPath());

            mediaPlayerRooster.prepareAsync();
            mediaPlayerRooster.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayerRooster.start();

                    //Set alarm count display
                    alarmPosition++;
                    //Send broadcast to DeviceAlarmFullScreenActivity with UI data
                    updateAlarmUI();

                    mediaPlayerRooster.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            //TODO: should users rather just be forced to record longer file?
                            //playDuration used to check that audio has played and for specified period, otherwise set default alarm
                            playDuration += mediaPlayerRooster.getDuration();
                            //delete file
                            file.delete();
                            //delete record from AudioTable SQL DB
                            audioTableManager.removeAudioFile(audioItem.getId());
                            //delete record from arraylist
                            audioItems.remove(audioItem);
                            //Play next file if list not empty
                            if (!audioItems.isEmpty()) {
                                playSocialRooster(audioItems.get(0));
                            }
                            //Check conditions for playing default tone: people must wake up!
                            else if (playDuration < 5000) {
                                startDefaultAlarmTone();
                            }
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();

            //delete file
            file.delete();
            //delete record from AudioTable SQL DB
            audioTableManager.removeAudioFile(audioItem.getId());
            //delete record from arraylist
            audioItems.remove(audioItem);
        }

        //Check conditions for playing default tone: people must wake up!
        if (playDuration < 5000 && (audioItems == null || audioItems.size() == 0)) {
            startDefaultAlarmTone();
        }
    }

    public void pauseSocialRooster() {
        audioServiceForegroundNotification("Social Rooster paused");

        mediaPlayerRooster.pause();
        currentPositionRooster = mediaPlayerRooster.getCurrentPosition();
    }

    public void startVibrate() {
        audioServiceForegroundNotification("Alarm vibrate active");
    }

    public void stopVibrate() {
        stopForeground(true);
        //If vibrating then cancel
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
        }
    }

    public void snoozeAudioState(){
        pauseSocialRooster();
        //If vibrating then cancel
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.cancel();
        }
    }

    public void startDefaultAlarmTone() {
        audioServiceForegroundNotification("Alarm ringtone playing");

        mediaPlayerDefault = new MediaPlayer();

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        //In case no alarm tone previously set
        if (notification == null) {
            notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        try {
            mediaPlayerDefault = new MediaPlayer();
            mediaPlayerDefault.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayerDefault.setDataSource(this, notification);
            mediaPlayerDefault.setLooping(true);
            mediaPlayerDefault.prepare();
            mediaPlayerDefault.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopAlarmAudio() {
        stopForeground(true);
        //If default tone or media playing then stop
        if (mediaPlayerDefault != null && mediaPlayerDefault.isPlaying()) {
            mediaPlayerDefault.stop();
            mediaPlayerDefault.release();
        }
        if (mediaPlayerRooster != null && mediaPlayerRooster.isPlaying()) {
            mediaPlayerRooster.stop();
            mediaPlayerRooster.release();
            currentPositionRooster = 0;
        }
    }

    private void audioServiceForegroundNotification(String state) {

        Intent notificationIntent = new Intent(this, AudioService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification=new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setContentText("Rooster Mornings: " + state)
                .setContentIntent(pendingIntent).build();

        startForeground(NOTIFICATION_ID, notification);
    }
}
