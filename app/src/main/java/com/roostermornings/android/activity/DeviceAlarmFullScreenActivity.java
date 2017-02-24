package com.roostermornings.android.activity;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.DeviceAudioQueueItem;
import com.roostermornings.android.sqldata.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnTouch;

public class DeviceAlarmFullScreenActivity extends BaseActivity {

    MediaPlayer mediaPlayer;
    DeviceAlarmController deviceAlarmController;

    List<DeviceAudioQueueItem> audioItems = new ArrayList<>();
    AudioTableManager audioTableManager = new AudioTableManager(this);

    private int playDuration;

    @BindView(R.id.alarm_sender_pic)
    ImageView imgSenderPic;

    @BindView(R.id.alarm_sender_name)
    TextView txtSenderName;

    @BindView(R.id.alarm_snooze_button)
    Button mButtonAlarmSnooze;

    @BindView(R.id.alarm_dismiss)
    TextView mButtonAlarmDismiss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_device_alarm_full_screen);
        //Used to ensure alarm shows over lock-screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                + WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        deviceAlarmController = new DeviceAlarmController(this);

        playDuration = 0;

        if(getIntent().getBooleanExtra(DeviceAlarm.EXTRA_TONE, false)){
            playAlarmTone();
        }
        else {
            retrieveMyAlarms();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
            //If vibrating then cancel
            Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
            if (vibrator.hasVibrator()) {
                vibrator.cancel();
            }

            //If default tone or media playing then stop
            if (mediaPlayer!=null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
    }

    @OnClick(R.id.alarm_snooze_button)
    protected void onAlarmSnoozeButtonClicked() {
        deviceAlarmController.snoozeAlarm();
        finish();
    }

    @OnClick(R.id.alarm_dismiss)
    protected void onAlarmDismissButtonClicked() {
        finish();
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
        mediaPlayer = new MediaPlayer();
        //Set media player to alarm volume
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        final File file = new File(getFilesDir() + "/" + audioItem.getFilename());
        //TODO:
        //setProfilePic(audioItem.getSender_pic());
        txtSenderName.setText(audioItem.getSender_name());

        try {
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    //TODO: should users rather just be forced to record longer file?
                    //playDuration used to check that audio has played and for specified period, otherwise set default alarm
                    playDuration += mediaPlayer.getDuration();
                    //delete file
                    file.delete();
                    //delete record from AudioTable SQL DB
                    audioTableManager.removeAudioFile(audioItem.getId());
                    //delete record from arraylist
                    audioItems.remove(audioItem);
                    //Play next file if list not empty
                    if (!audioItems.isEmpty()) {
                        playNewAudioFile(audioItems.get(0));
                    }
                    //Check conditions for playing default tone: people must wake up!
                    else if(playDuration < 5000){
                        playAlarmTone();
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

        //Check conditions for playing default tone: people must wake up!
        if (playDuration < 5000 && audioItems.isEmpty()){
            playAlarmTone();
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
