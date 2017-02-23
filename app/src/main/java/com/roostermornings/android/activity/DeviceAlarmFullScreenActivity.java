package com.roostermornings.android.activity;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.DeviceAudioQueueItem;
import com.roostermornings.android.sqldata.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class DeviceAlarmFullScreenActivity extends BaseActivity {

    MediaPlayer mediaPlayer;

    List<DeviceAudioQueueItem> audioItems = new ArrayList<>();
    AudioTableManager audioTableManager = new AudioTableManager(this);

    @BindView(R.id.alarm_sender_pic)
    ImageView imgSenderPic;

    @BindView(R.id.alarm_sender_name)
    TextView txtSenderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_device_alarm_full_screen);
        //Used to ensure alarm shows over lock-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if(getIntent().getBooleanExtra(DeviceAlarm.EXTRA_TONE, false)){
            playAlarmTone();
        }
        else {
            retrieveMyAlarms();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //If vibrating then cancel
        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
        if(vibrator.hasVibrator()) {
            vibrator.cancel();
        }

        //If default tone or media playing then stop
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    protected void playAlarmTone() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        //In case no alarm tone previously set
        if(notification == null){
            notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if(notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setDataSource(this, notification);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected void retrieveMyAlarms() {
        audioItems = audioTableManager.extractAudioFiles();
        if (audioItems == null || audioItems.size() == 0) return;
        playNewAudioFile(audioItems.get(0));
    }


    protected void playNewAudioFile(final DeviceAudioQueueItem audioItem) {
        //TODO: test corrupt audio
        //TODO: default alarm tone
        mediaPlayer = new MediaPlayer();
        //Set media player to alarm volume
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        final File file = new File(getFilesDir() + "/" + audioItem.getFilename());
        setProfilePic(audioItem.getSender_pic());
        txtSenderName.setText(audioItem.getSender_name());

        try {
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //delete file
                    file.delete();
                    //delete record from AudioTable SQL DB
                    audioTableManager.removeAudioFile(audioItem.getId());
                    //delete record from arraylist
                    audioItems.remove(audioItem);
                    if (!audioItems.isEmpty()) {
                        playNewAudioFile(audioItems.get(0));
                    }
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
    }

    protected void setProfilePic(String url) {
        try {
            URL imageUrl = new URL(url);
            Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());

            Resources res = getResources();
            RoundedBitmapDrawable dr =
                    RoundedBitmapDrawableFactory.create(res, bitmap);
            dr.setCornerRadius(Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f);
            imgSenderPic.setImageDrawable(dr);

        } catch (IOException e) {
            e.printStackTrace();
            imgSenderPic.setImageBitmap(null);
        }
    }
}
