/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.MinimumRequirements;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.InternetHelper;

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
                boolean aboveMinReq = true;
                MinimumRequirements minimumRequirements = dataSnapshot.getValue(MinimumRequirements.class);
                if(minimumRequirements == null) {
                    chooseActivity(true, null);
                    return;
                }
                if(minimumRequirements.isInvalidate_user()) {
                    try {
                        String buildVersionComponents[] = BuildConfig.VERSION_NAME.split("\\.");
                        String minVersionComponents[] = minimumRequirements.getApp_version().split("\\.");
                        int position = 0;
                        for (String component :
                                minVersionComponents) {
                            if (!component.isEmpty()) {
                                Integer componentInteger = Integer.valueOf(component);
                                if (position >= buildVersionComponents.length) return;
                                Integer buildComponentInteger = Integer.valueOf(buildVersionComponents[position]);
                                aboveMinReq = aboveMinReq && (buildComponentInteger >= componentInteger);
                                position++;
                            }
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        chooseActivity(true, null);
                    }
                }
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
