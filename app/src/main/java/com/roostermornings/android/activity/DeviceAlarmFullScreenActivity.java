package com.roostermornings.android.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class DeviceAlarmFullScreenActivity extends BaseActivity {

    MediaPlayer mediaPlayer;
    DeviceAlarmController deviceAlarmController;

    List<DeviceAudioQueueItem> audioItems = new ArrayList<>();
    AudioTableManager audioTableManager = new AudioTableManager(this);

    private int playDuration;
    private int alarmCount;
    private int alarmPosition;

    @BindView(R.id.alarm_sender_pic)
    ImageView imgSenderPic;

    @BindView(R.id.alarm_sender_name)
    TextView txtSenderName;

    @BindView(R.id.alarm_count)
    TextView txtAlarmCount;

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
        //Replace image and name with message if no Roosters etc.
        setDefaultDisplayProfile();
    }

    protected void retrieveMyAlarms() {
        audioItems = audioTableManager.extractAudioFiles();
        alarmCount = audioItems.size();
        if (audioItems == null || audioItems.size() == 0) {
            //Check conditions for playing default tone: people must wake up!
            if (playDuration < 5000 && (audioItems == null || audioItems.size() == 0)){
                playAlarmTone();
            }
            return;
        };
        alarmPosition = 0;
        playNewAudioFile(audioItems.get(0));
    }

    protected void playNewAudioFile(final DeviceAudioQueueItem audioItem) {
        mediaPlayer = new MediaPlayer();
        //Set media player to alarm volume
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        final File file = new File(getFilesDir() + "/" + audioItem.getFilename());

        if (audioItem.getSender_pic() != null && audioItem.getSender_pic().length() != 0) {
            setProfilePic(audioItem.getSender_pic());
        } else{
            imgSenderPic.setBackground(getResources().getDrawable(R.drawable.alarm_profile_pic_circle));
        }
        txtSenderName.setText(audioItem.getSender_name());

        try {
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            //Set alarm count display
            alarmPosition++;
            setAlarmCount(alarmPosition, alarmCount);

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
        if (playDuration < 5000 && (audioItems == null || audioItems.size() == 0)){
            playAlarmTone();
        }
    }

    protected void setAlarmCount(int count, int total) {
        txtAlarmCount.setText(String.format("%s of %s", count, total));
    }

    protected void setProfilePic(String url) {

        Picasso.with(DeviceAlarmFullScreenActivity.this).load(url)
                .resize(400, 400)
                .into(imgSenderPic, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap imageBitmap = ((BitmapDrawable) imgSenderPic.getDrawable()).getBitmap();
                        RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                        imageDrawable.setCircular(true);
                        imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                        imgSenderPic.setImageDrawable(imageDrawable);
                    }

                    @Override
                    public void onError() {
                        //imgSenderPic.setBackground(getResources().getDrawable(R.drawable.alarm_profile_pic_circle));
                        setDefaultDisplayProfile();
                    }
                });
    }

    protected void setDefaultDisplayProfile() {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        Drawable d = new BitmapDrawable(getResources(), bm);
        txtSenderName.setText(R.string.alarm_default_name);
        imgSenderPic.setBackground(d);
    }
}
