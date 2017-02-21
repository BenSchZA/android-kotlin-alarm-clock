package com.roostermornings.android.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.DeviceAudioQueueItem;
import com.roostermornings.android.sqldata.AudioTableManager;

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
        retrieveMyAlarms();
    }

    protected void retrieveMyAlarms() {
        audioItems = audioTableManager.extractAudioFiles();
        if (audioItems == null || audioItems.size() == 0) return;
        playNewAudioFile(audioItems.get(0));
    }


    protected void playNewAudioFile(final DeviceAudioQueueItem audioItem) {
        //TODO: default alarm tone
        mediaPlayer = new MediaPlayer();
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
                    //delete record from arraylist
                    file.delete();
                    audioItems.remove(audioItem);
                    if (!audioItems.isEmpty()) {
                        playNewAudioFile(audioItems.get(0));
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setProfilePic(String url) {
        URL imageUrl = null;
        try {
            imageUrl = new URL(url);
            Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
            imgSenderPic.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            imgSenderPic.setImageBitmap(null);
        }
    }
}
