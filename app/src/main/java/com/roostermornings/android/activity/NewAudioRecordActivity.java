/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.util.RoosterUtils;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class NewAudioRecordActivity extends BaseActivity {

    private long startTime;
    private long elapsedTime;
    private long runningTime;
    private final int REFRESH_RATE = 100;
    private final int MAX_RECORDING_TIME = 60000;
    private final String maxRecordingTime = "60";
    private String hours, minutes, seconds, milliseconds;
    private long secs, mins, hrs;
    private Handler mHandler = new Handler();
    private boolean mRecording = false;
    private boolean mListening = false;
    private boolean mPaused = false;
    String mAudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    public static final int RequestPermissionCode = 1;
    private String randomAudioFileName = "";

    @BindView(R.id.new_audio_time)
    TextView txtAudioTime;

    @BindView(R.id.new_audio_start_stop)
    ImageView imgAudioStartStop;

    @BindView(R.id.new_audio_listen_parent)
    LinearLayout layoutListenParent;

    @BindView(R.id.new_audio_record_parent)
    RelativeLayout layoutRecordParent;


    @BindView(R.id.new_audio_listen)
    ImageView imgNewAudioListen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_new_audio);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_audio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.new_audio_start_stop)
    public void startStopAudioRecording() {


        if (!mRecording) {


            randomAudioFileName = RoosterUtils.createRandomFileName(5) + "RoosterRecording.3gp";
            startTime = System.currentTimeMillis() + MAX_RECORDING_TIME;

            if (checkPermission()) {

                mAudioSavePathInDevice =
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                                randomAudioFileName;

                MediaRecorderReady();

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                mHandler.removeCallbacks(startTimer);
                mHandler.postDelayed(startTimer, 0);

                imgAudioStartStop.setBackgroundResource(R.drawable.rooster_record_audio_circle_inner_recording);
                imgAudioStartStop.requestLayout();
                mRecording = true;


            } else {
                requestPermission();
            }

        } else {
            stopRecording();
        }
    }

    public void stopRecording() {
        try {
            mHandler.removeCallbacks(startTimer);
            mRecording = false;
            mediaRecorder.stop();
            imgAudioStartStop.setBackgroundResource(R.drawable.rooster_record_audio_circle_inner_selectable);
            layoutListenParent.setVisibility(View.VISIBLE);
            layoutRecordParent.setVisibility(View.INVISIBLE);
        } catch(IllegalStateException e){
            e.printStackTrace();
        }
    }

    @OnClick(R.id.new_audio_delete)
    public void onDeleteAudioClick() {

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            MediaRecorderReady();
        }
        File file = new File(mAudioSavePathInDevice);
        file.delete();
        layoutListenParent.setVisibility(View.INVISIBLE);
        layoutRecordParent.setVisibility(View.VISIBLE);
        txtAudioTime.setText(maxRecordingTime);
        mHandler.removeCallbacks(startTimer);

    }

    @OnClick(R.id.new_audio_listen)
    public void onAudioListenClick() {

        try {

            if (!mListening && !mPaused) { //playback initiated

                startTime = System.currentTimeMillis() + MAX_RECORDING_TIME;
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(mAudioSavePathInDevice);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mListening = true;
                mediaPlayer.start();

                mHandler.removeCallbacks(startTimer);
                mHandler.postDelayed(startTimer, 0);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {

                        try {

                            mPaused = false;
                            mListening = false;
                            mHandler.removeCallbacks(startTimer);
                            txtAudioTime.setText(maxRecordingTime);
                            mediaPlayer.stop();
                            imgNewAudioListen.setBackgroundResource(R.drawable.rooster_new_audio_play_button);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                });
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_new_audio_pause_button);


            } else if (!mPaused) { //user paused playback

                mPaused = true;
                mediaPlayer.pause();
                runningTime = System.currentTimeMillis();
                mHandler.removeCallbacks(startTimer);
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_new_audio_play_button);

            } else { //user resumed playback

                mPaused = false;
                mediaPlayer.start();
                mHandler.postDelayed(startTimer, 0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @OnClick(R.id.new_audio_save)
    public void onSaveAudioFileClick() {

        if (!checkInternetConnection()) return;

        Intent intent = new Intent(NewAudioRecordActivity.this, NewAudioFriendsActivity.class);
        Bundle bun = new Bundle();
        bun.putString("localFileString", mAudioSavePathInDevice);
        intent.putExtras(bun);
        startActivity(intent);
    }

    private Runnable startTimer = new Runnable() {
        public void run() {
            if (mPaused) {
                elapsedTime = startTime - runningTime;
                mPaused = false;
            } else {
                elapsedTime = startTime - System.currentTimeMillis();
            }
            if(elapsedTime < 0){
                stopRecording();
            } else{
                updateTimer(elapsedTime);
                mHandler.postDelayed(this, REFRESH_RATE);
            }
        }
    };

    private void updateTimer(float time) {
        secs = (long) (time / 1000);
        mins = (long) ((time / 1000) / 60);
        hrs = (long) (((time / 1000) / 60) / 60);

		/* Convert the seconds to String
         * and format to ensure it has
		 * a leading zero when required
		 */
        secs = secs % 60;
        seconds = String.valueOf(secs);
        if (secs == 0) {
            seconds = "0";
        }

		/* Convert the minutes to String and format the String */

        mins = mins % 60;
        minutes = String.valueOf(mins);
        if (mins == 0) {
            minutes = "00";
        }
        if (mins < 10 && mins > 0) {
            minutes = "0" + minutes;
        }

    	/* Convert the hours to String and format the String */

        hours = String.valueOf(hrs);
        if (hrs == 0) {
            hours = "00";
        }
        if (hrs < 10 && hrs > 0) {
            hours = "0" + hours;
        }

    	/* Although we are not using milliseconds on the timer in this example
         * I included the code in the event that you wanted to include it on your own
    	 */
        milliseconds = String.valueOf((long) time);
        if (milliseconds.length() == 2) {
            milliseconds = "0" + milliseconds;
        }
        if (milliseconds.length() <= 1) {
            milliseconds = "00";
        }
        milliseconds = milliseconds.substring(milliseconds.length() - 3, milliseconds.length() - 2);

		/* Setting the timer text to the elapsed time */
        txtAudioTime.setText(seconds);
    }

    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(mAudioSavePathInDevice);
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(NewAudioRecordActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                    } else {
                        Toast.makeText(NewAudioRecordActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }


}
