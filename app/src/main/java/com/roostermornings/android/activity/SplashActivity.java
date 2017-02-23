package com.roostermornings.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;

import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;

public class SplashActivity extends BaseActivity {

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

                boolean introViewed = sharedPreferences.getBoolean(getString(R.string.preferences_intro_viewed), false);

                if (!introViewed) {

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.preferences_intro_viewed), true);
                    editor.commit();

                    navigateToActivity(IntroFragmentActivity.class);

                } else if (mFBUser == null || mFBUser.getUid() == null) {

                    navigateToActivity(SignInActivity.class);

                } else {

                    navigateToActivity(MyAlarmsFragmentActivity.class);
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
