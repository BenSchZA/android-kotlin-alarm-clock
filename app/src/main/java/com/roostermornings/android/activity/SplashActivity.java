package com.roostermornings.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;

public class SplashActivity extends BaseActivity {

    static final String TAG = "SplashActivity";
    boolean mIntroViewed = false;//sharedPreferences.getBoolean(getString(R.string.preferences_intro_viewed), false);
    FirebaseUser mFBUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_splash);
        mFBUser = getFirebaseUser();

        CountDownTimer countDownTimer = new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                if (!mIntroViewed) {

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.preferences_intro_viewed), true);
                    editor.commit();

                    navigateToActivity(IntroFragmentActivity.class);

                } else if (mFBUser == null) {

                    navigateToActivity(SignInActivity.class);

                } else {

                    navigateToActivity(MyAlarmsActivity.class);
                }

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
