/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.service.notification.NotificationListenerService;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.sqldata.DeviceAlarmTableHelper;

import javax.inject.Inject;

public class SplashActivity extends BaseActivity {

    //TODO: implement and check auth
//    @Override
//    public void onStart() {
//        super.onStart();
//
//        // Monitor launch times and interval from installation
//        RateThisApp.onStart(this);
//        // If the condition is satisfied, "Rate this app" dialog will be shown
//        RateThisApp.showRateDialogIfNeeded(this);
//    }

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //All users go through intro activity upon sign out -
        // this ensures cell number is entered and if old user they are on-boarded, no harm done
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            navigateToActivity(IntroFragmentActivity.class);
        } else {
            //TODO: go to alarm creation for new user?
            navigateToActivity(MyAlarmsFragmentActivity.class);
        }
    }

    private void navigateToActivity(Class<? extends Activity> activityClass) {
        Intent i = new Intent(SplashActivity.this, activityClass);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        startActivity(i);
        finish();
    }
}
