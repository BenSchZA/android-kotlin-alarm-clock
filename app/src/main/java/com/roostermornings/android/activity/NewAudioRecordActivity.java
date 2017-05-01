/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.RoosterUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class NewAudioRecordActivity extends BaseActivity {

    private long startTime;
    private long countDownTime;
    //NB: this refresh rate has a direct influence on performance as well as tuning of record time/amplitude calculations
    private final int REFRESH_RATE = 100;
    private final int MAX_RECORDING_TIME = 60000;
    private final String maxRecordingTime = "60";

    //Silence average: 125
    //Ambient music: 300
    private final int AUDIO_MIN_AMPLITUDE = 250;
    private final int MIN_CUMULATIVE_TIME = 5000;
    private int maxAmplitude;
    private double averageAmplitude;
    private long timeSinceAcceptableAmplitudeStart;
    private long cumulativeAcceptableAmplitudeTime;
    private long timeOfUnsuccessfulAmplitude;
    private long timeOfSuccessfulAmplitude;

    private final Handler mHandler = new Handler();

    private String mAudioSavePathInDevice = null;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private int audioLength = 0;

    private String randomAudioFileName = "";

    private int newAudioStatus = 0;
    private static final int NEW_AUDIO_RECORDING = 1;
    private static final int NEW_AUDIO_LISTENING = 2;
    private static final int NEW_AUDIO_PAUSED = 3;
    private static final int NEW_AUDIO_READY_RECORD = 4;
    private static final int NEW_AUDIO_READY_LISTEN = 5;
    private static final int NEW_AUDIO_RECORD_ERROR = 6;

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

    @BindView(R.id.new_message)
    TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_new_audio);

        setDayNightTheme();
        setButtonBarSelection();

        setNewAudioStatus(NEW_AUDIO_READY_RECORD);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @OnClick(R.id.home_friends)
    public void manageFriends() {
        startActivity(new Intent(NewAudioRecordActivity.this, FriendsFragmentActivity.class));
    }

    @OnClick(R.id.home_my_alarms)
    public void manageAlarms() {
        startHomeActivity();
    }

    @OnClick(R.id.home_my_uploads)
    public void manageUploads() {
        startActivity(new Intent(NewAudioRecordActivity.this, MessageStatusActivity.class));
    }

    @OnClick(R.id.home_record_audio)
    public void recordNewAudio() {
        if (!checkInternetConnection()) return;
        startActivity(new Intent(NewAudioRecordActivity.this, NewAudioRecordActivity.class));
    }

    private void setNewAudioStatus(int status) {
        newAudioStatus = status;
        switch(status){
            case NEW_AUDIO_RECORD_ERROR:
                layoutRecordParent.setAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
                mHandler.removeCallbacks(startTimer);
                layoutListenParent.setVisibility(View.INVISIBLE);
                layoutRecordParent.setVisibility(View.VISIBLE);
                //Clear red recording focus
                imgAudioStartStop.setSelected(false);
                txtMessage.setText(getResources().getText(R.string.new_audio_quiet_instructions));
                txtAudioTime.setText(maxRecordingTime);
                break;
            case NEW_AUDIO_READY_RECORD:
                layoutRecordParent.setAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
                mHandler.removeCallbacks(startTimer);
                layoutListenParent.setVisibility(View.INVISIBLE);
                layoutRecordParent.setVisibility(View.VISIBLE);
                //Clear red recording focus
                imgAudioStartStop.setSelected(false);
                txtMessage.setText(getResources().getText(R.string.new_audio_instructions));
                txtAudioTime.setText(maxRecordingTime);
                break;
            case NEW_AUDIO_READY_LISTEN:
                layoutRecordParent.clearAnimation();
                mHandler.removeCallbacks(startTimer);
                layoutRecordParent.setVisibility(View.INVISIBLE);
                //Clear red recording focus
                imgAudioStartStop.setSelected(false);
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_new_audio_play_button);
                layoutListenParent.setVisibility(View.VISIBLE);
                txtMessage.setText(getResources().getText(R.string.new_audio_continue_instructions));

                try {
                    Uri uri = Uri.parse(mAudioSavePathInDevice);
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(this, uri);
                    String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    audioLength = Integer.parseInt(duration);
                    updateTimer(audioLength);
                } catch(Resources.NotFoundException e) {
                    e.printStackTrace();
                } catch(IllegalArgumentException e) {
                    e.printStackTrace();
                }

                break;
            case NEW_AUDIO_RECORDING:
                layoutRecordParent.setAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
                //Reset message from amplitude instructions to default
                txtMessage.setText(getResources().getText(R.string.new_audio_instructions));
                imgAudioStartStop.setSelected(true);

                startTime = System.currentTimeMillis() + MAX_RECORDING_TIME;
                break;
            case NEW_AUDIO_PAUSED:
                layoutRecordParent.clearAnimation();
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_new_audio_play_button);
                break;
            case NEW_AUDIO_LISTENING:
                layoutRecordParent.clearAnimation();
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_new_audio_pause_button);

                if(mediaPlayer != null && mediaPlayer.getCurrentPosition() > 0) {
                    startTime = System.currentTimeMillis() + audioLength - mediaPlayer.getCurrentPosition();
                } else {
                    startTime = System.currentTimeMillis() + audioLength;
                }
                break;
            default:
                break;
        }
    }

    @OnClick(R.id.new_audio_start_stop)
    public void startStopAudioRecording() {
        if (newAudioStatus != NEW_AUDIO_RECORDING) {
            setNewAudioStatus(NEW_AUDIO_RECORDING);

            //Reset acceptable amplitude timer
            timeSinceAcceptableAmplitudeStart = 0;
            timeOfUnsuccessfulAmplitude = System.currentTimeMillis();
            timeOfSuccessfulAmplitude = System.currentTimeMillis();
            cumulativeAcceptableAmplitudeTime = 0;
            averageAmplitude = 0;

            randomAudioFileName = RoosterUtils.createRandomFileName(5) + Constants.FILENAME_PREFIX_ROOSTER_TEMP_RECORDING + ".3gp";

            if (checkPermission()) {

                mAudioSavePathInDevice =
                        getFilesDir().getAbsolutePath() + "/" +
                                randomAudioFileName;

                mediaRecorderReady();

                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();
                    mHandler.removeCallbacks(startTimer);
                    mHandler.postDelayed(startTimer, 0);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                requestPermission();
            }

        } else {
            stopRecording();
        }
    }

    private void stopRecording() {
        try {
            mHandler.removeCallbacks(startTimer);
            mediaRecorder.stop();
        } catch(IllegalStateException e){
            e.printStackTrace();
            deleteAudio();
            setNewAudioStatus(NEW_AUDIO_READY_RECORD);
        } catch(RuntimeException e){
            e.printStackTrace();
            deleteAudio();
            setNewAudioStatus(NEW_AUDIO_READY_RECORD);
        }

        //Check if cumulative amplitude condition has not been met, else continue
        if(cumulativeAcceptableAmplitudeTime < MIN_CUMULATIVE_TIME) {
            deleteAudio();
            setNewAudioStatus(NEW_AUDIO_RECORD_ERROR);
        } else {
            setNewAudioStatus(NEW_AUDIO_READY_LISTEN);
        }
    }

    @OnClick(R.id.new_audio_delete)
    public void onDeleteAudioClick() {
        setNewAudioStatus(NEW_AUDIO_READY_RECORD);
        deleteAudio();
    }

    private boolean deleteAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaRecorderReady();
        }
        File file = new File(mAudioSavePathInDevice);
        return file.delete();
    }

    @OnClick(R.id.new_audio_listen)
    public void onAudioListenClick() {

        try {
            switch (newAudioStatus) {
                case NEW_AUDIO_READY_LISTEN:

                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(mAudioSavePathInDevice);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mediaPlayer.start();

                    mHandler.removeCallbacks(startTimer);
                    mHandler.postDelayed(startTimer, 0);
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {

                            try {
                                setNewAudioStatus(NEW_AUDIO_READY_LISTEN);
                                mediaPlayer.stop();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    setNewAudioStatus(NEW_AUDIO_LISTENING);
                    break;
                case NEW_AUDIO_LISTENING:

                    setNewAudioStatus(NEW_AUDIO_PAUSED);
                    mediaPlayer.pause();
                    mHandler.removeCallbacks(startTimer);

                    break;
                case NEW_AUDIO_PAUSED:

                    setNewAudioStatus(NEW_AUDIO_LISTENING);
                    mediaPlayer.start();
                    mHandler.postDelayed(startTimer, 0);

                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.new_audio_save)
    public void onSaveAudioFileClick() {
        if (!checkInternetConnection()) return;

        //Manage whether audio file is being sent direct to a user or a list of users needs to be shown
        Intent intent = new Intent(NewAudioRecordActivity.this, NewAudioFriendsActivity.class);
        Bundle bun = new Bundle();
        bun.putString(Constants.EXTRA_LOCAL_FILE_STRING, mAudioSavePathInDevice);

        //If this fails (shouldn't) then user can select from list of friends rather than direct message,
        //no harm done
        try {
            if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.EXTRA_FRIENDS_LIST)) {
                ArrayList<User> mFriends = (ArrayList<User>)getIntent().getSerializableExtra(Constants.EXTRA_FRIENDS_LIST);
                bun.putSerializable(Constants.EXTRA_FRIENDS_LIST, mFriends);
            }
        } catch(ClassCastException e) {
            e.printStackTrace();
        }

        intent.putExtras(bun);
        startActivity(intent);
    }

    private final Runnable startTimer = new Runnable() {
        public void run() {
            switch(newAudioStatus) {
                case NEW_AUDIO_READY_RECORD:
                    mHandler.removeCallbacks(startTimer);
                    txtAudioTime.setText(maxRecordingTime);
                    break;
                case NEW_AUDIO_RECORD_ERROR:
                    mHandler.removeCallbacks(startTimer);
                    txtAudioTime.setText(maxRecordingTime);
                    break;
                case NEW_AUDIO_PAUSED:
                    break;
                case NEW_AUDIO_LISTENING:
                    countDownTime = startTime - System.currentTimeMillis();
                    updateTimer(countDownTime);
                    mHandler.postDelayed(this, REFRESH_RATE);
                    break;
                case NEW_AUDIO_RECORDING:
                    //This logic checks that the average recording amplitude is above a certain threshold for a cumulative amount of time
                    maxAmplitude = mediaRecorder.getMaxAmplitude();
                    averageAmplitude = (maxAmplitude + averageAmplitude)/2;

                    //TODO: error here
                    cumulativeAcceptableAmplitudeTime = cumulativeAcceptableAmplitudeTime + timeSinceAcceptableAmplitudeStart;
                    timeSinceAcceptableAmplitudeStart = 0;

                    if(averageAmplitude < AUDIO_MIN_AMPLITUDE && cumulativeAcceptableAmplitudeTime < MIN_CUMULATIVE_TIME) {
                        txtMessage.setText(getResources().getText(R.string.new_audio_time_amplitude_instructions));
                        timeOfUnsuccessfulAmplitude = System.currentTimeMillis();
                    } else if(cumulativeAcceptableAmplitudeTime > MIN_CUMULATIVE_TIME) {
                        txtMessage.setText(getResources().getText(R.string.new_audio_instructions));
                    } else {
                        txtMessage.setText(getResources().getText(R.string.new_audio_time_instructions));
                        timeSinceAcceptableAmplitudeStart = timeOfSuccessfulAmplitude - timeOfUnsuccessfulAmplitude;
                        timeOfSuccessfulAmplitude = System.currentTimeMillis();
                    }
                    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                    if(countDownTime < 0) stopRecording();

                    countDownTime = startTime - System.currentTimeMillis();
                    updateTimer(countDownTime);
                    mHandler.postDelayed(this, REFRESH_RATE);
                    break;
                default:
                    mHandler.removeCallbacks(startTimer);
                    txtAudioTime.setText(maxRecordingTime);
                    break;
            }
        }
    };

    private void updateTimer(float time) {
        String displaySeconds;
        long seconds;
        seconds = (long) (time / 1000);

		/* Convert the seconds to String
         * and format to ensure it has
		 * a leading zero when required
		 */
        seconds = seconds % 60;
        displaySeconds = String.valueOf(seconds);

		/* Setting the timer text to the elapsed time */
        txtAudioTime.setText(displaySeconds);
    }

    private void mediaRecorderReady() {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(mAudioSavePathInDevice);
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(NewAudioRecordActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, Constants.MY_PERMISSIONS_REQUEST_AUDIO_RECORD);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_AUDIO_RECORD:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        setNewAudioStatus(NEW_AUDIO_READY_LISTEN);
                        startStopAudioRecording();
                    } else {
                        Toast.makeText(NewAudioRecordActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            default:
                break;
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }
}