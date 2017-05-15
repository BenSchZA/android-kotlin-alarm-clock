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
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.domain.MinimumRequirements;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.sqldata.AudioTableHelper;
import com.roostermornings.android.sqldata.DeviceAlarmTableHelper;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.InternetHelper;

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
        if(!InternetHelper.noInternetConnection(this)) {
            checkMinimumRequirements();
        } else {
            chooseActivity(true, null);
        }
    }

    private void navigateToActivity(Class<? extends Activity> activityClass) {
        Intent i = new Intent(SplashActivity.this, activityClass);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        startActivity(i);
        finish();
    }

    private void checkMinimumRequirements() {
        DatabaseReference minReqRef = FirebaseDatabase.getInstance().getReference()
                .child("minimum_requirements");
        minReqRef.keepSynced(true);

        ValueEventListener minReqListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean aboveMinReq;
                MinimumRequirements minimumRequirements = dataSnapshot.getValue(MinimumRequirements.class);
                if(minimumRequirements == null) {
                    chooseActivity(true, null);
                    return;
                }
                aboveMinReq = !minimumRequirements.isInvalidate_user() || (BuildConfig.VERSION_CODE >= minimumRequirements.getApp_version());
                chooseActivity(aboveMinReq, minimumRequirements);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                chooseActivity(true, null);
            }
        }; minReqRef.addListenerForSingleValueEvent(minReqListener);
    }

    private void chooseActivity(boolean aboveMinimumRequirements, MinimumRequirements minimumRequirements) {
        if(!aboveMinimumRequirements) {
            Intent i = new Intent(SplashActivity.this, InvalidateVersion.class);
            if(minimumRequirements != null) {
                i.putExtra(Constants.FORCE_UPDATE_TITLE, minimumRequirements.getUpdate_title());
                i.putExtra(Constants.FORCE_UPDATE_DESCRIPTION, minimumRequirements.getUpdate_description());
            }
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            startActivity(i);
            finish();
        } else {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                navigateToActivity(IntroFragmentActivity.class);
            } else {
                navigateToActivity(MyAlarmsFragmentActivity.class);
            }
        }
    }
}
