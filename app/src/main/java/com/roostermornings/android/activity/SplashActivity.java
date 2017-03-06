package com.roostermornings.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.background.BackgroundTaskReceiver;
import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.sqldata.DeviceAlarmTableHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SplashActivity extends BaseActivity {

    FirebaseUser mFBUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_splash);
        mFBUser = getFirebaseUser();

        //TODO: remove on release, used for Facebook app auth during debug stage
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.roostermornings.android",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        //TODO: remove
        if (BuildConfig.DEBUG) {
            AudioTableHelper dbAudioHelper = new AudioTableHelper(this);
            SQLiteDatabase dbAudio = dbAudioHelper.getWritableDatabase();
            DeviceAlarmTableHelper dbAlarmHelper = new DeviceAlarmTableHelper(this);
            SQLiteDatabase dbAlarm = dbAlarmHelper.getWritableDatabase();

            AudioTableHelper audioTableHelper = new AudioTableHelper(this);
            DeviceAlarmTableHelper deviceAlarmTableHelper = new DeviceAlarmTableHelper(this);
            audioTableHelper.onUpgrade(dbAudio, 1, 2);
            deviceAlarmTableHelper.onUpgrade(dbAlarm, 1, 2);

            dbAudio.close();
            dbAlarm.close();
        }

            BackgroundTaskReceiver backgroundTaskReceiver = new BackgroundTaskReceiver();
            backgroundTaskReceiver.scheduleBackgroundCacheFirebaseData(getApplicationContext());
            backgroundTaskReceiver.scheduleBackgroundDailyTask(getApplicationContext());

        CountDownTimer countDownTimer = new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                //All users go through intro activity upon sign out - this ensures cell number is entered and if old user they are on-boarded, no harm done
               if (mFBUser == null || mFBUser.getUid() == null) {
                   navigateToActivity(IntroFragmentActivity.class);
                } else {
                    navigateToActivity(MyAlarmsFragmentActivity.class);
                }

//                boolean introViewed = sharedPreferences.getBoolean(getString(R.string.preferences_intro_viewed), false);
//
//                if (!introViewed) {
//
//                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                    editor.putBoolean(getString(R.string.preferences_intro_viewed), true);
//                    editor.commit();
//
//                    navigateToActivity(IntroFragmentActivity.class);
//
//                } else if (mFBUser == null || mFBUser.getUid() == null) {
//
//                    navigateToActivity(SignInActivity.class);
//
//                } else {
//
//                    navigateToActivity(MyAlarmsFragmentActivity.class);
//                }

            }
        };
        countDownTimer.start();
    }

    private void navigateToActivity(Class<? extends Activity> activityClass) {
        Intent i = new Intent(SplashActivity.this, activityClass);
        finish();
        startActivity(i);
    }

}
