package com.roostermornings.android.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.fragment.NewAlarmFragment1;
import com.roostermornings.android.fragment.NewAlarmFragment2;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

import butterknife.BindView;
import butterknife.OnClick;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class NewAudioActivity extends BaseActivity {

    private long startTime;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;
    private String hours, minutes, seconds, milliseconds;
    private long secs, mins, hrs;
    private Handler mHandler = new Handler();
    private boolean mRecording = false;
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    Random random;
    String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer;
    private StorageReference mStorageRef;
    private String randomAudioFileName = "";
    final int sdk = android.os.Build.VERSION.SDK_INT;

    @BindView(R.id.new_audio_time)
    TextView txtAudioTime;

    @BindView(R.id.new_audio_start_stop)
    TextView txtAudioStartStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_audio);
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

    private Runnable startTimer = new Runnable() {
        public void run() {
            elapsedTime = System.currentTimeMillis() - startTime;
            updateTimer(elapsedTime);
            mHandler.postDelayed(this, REFRESH_RATE);
        }
    };

    @OnClick(R.id.new_audio_start_stop)
    public void startStopAudioRecording() {

        if (!mRecording) {

            random = new Random();
            randomAudioFileName = CreateRandomAudioFileName(5) + "RoosterRecording.3gp";

            if (checkPermission()) {

                AudioSavePathInDevice =
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

                startTime = System.currentTimeMillis();
                mHandler.removeCallbacks(startTimer);
                mHandler.postDelayed(startTimer, 0);

                txtAudioStartStop.setBackgroundResource(R.drawable.rooster_record_audio_square_inner);
                mRecording = true;


            } else {
                requestPermission();
            }


        } else {
            mediaRecorder.stop();
            mHandler.removeCallbacks(startTimer);
            txtAudioStartStop.setBackgroundResource(R.drawable.rooster_record_audio_circle_inner_selectable);
        }


    }

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
            seconds = "00";
        }
        if (secs < 10 && secs > 0) {
            seconds = "0" + seconds;
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
        txtAudioTime.setText(hours + ":" + minutes + ":" + seconds);
    }

    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    public String CreateRandomAudioFileName(int string) {
        StringBuilder stringBuilder = new StringBuilder(string);
        int i = 0;
        while (i < string) {
            stringBuilder.append(RandomAudioFileName.
                    charAt(random.nextInt(RandomAudioFileName.length())));

            i++;
        }
        return stringBuilder.toString();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(NewAudioActivity.this, new
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
                        Toast.makeText(NewAudioActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
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
