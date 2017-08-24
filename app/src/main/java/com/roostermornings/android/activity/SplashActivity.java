/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

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
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.FileUtils;
import com.roostermornings.android.util.InternetHelper;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.Toaster;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static java.text.DateFormat.getDateTimeInstance;

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

    Intent receivedIntent;
    String receivedAction;
    String receivedType;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receivedIntent = getIntent();
        if(receivedIntent != null) {
            receivedAction = receivedIntent.getAction();
            receivedType = receivedIntent.getType();

            if(Intent.ACTION_SEND.equals(receivedAction)) {
                if(receivedType != null && receivedType.startsWith("audio/")) {
                    Uri receivedUri = receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if(receivedUri != null) {

                        File shareFilePath = new File(getCacheDir(), "Cache");
                        String timeStamp = getDateTimeInstance().format(new Date());
                        String shareFileName = "Rooster_Shared_Audio_" + RoosterUtils.createRandomUID(5) + "_" + timeStamp + ".3gp";
                        File shareFile = new File(shareFilePath, shareFileName);

                        if(shareFile.getParentFile().exists() || shareFile.getParentFile().mkdirs()) {

                            try {
                                InputStream inputStream = getContentResolver().openInputStream(receivedUri);
                                //Copy file contents to new public file
                                FileUtils.copyFromStream(inputStream, shareFile);

                                //If file is valid, open a share dialog
                                if (shareFile.isFile() && validMimeType(shareFile) && shareFile.length() < Constants.MAX_ROOSTER_FILE_SIZE) {
                                    //Uri shareFileUri = FileProvider.getUriForFile(this, "com.roostermornings.android.fileprovider", shareFile);
                                    //Send audio file to friends selection activity
                                    Intent intent = new Intent(SplashActivity.this, NewAudioFriendsActivity.class);
                                    Bundle bun = new Bundle();
                                    bun.putString(Constants.EXTRA_LOCAL_FILE_STRING, shareFile.getPath());
                                    intent.putExtras(bun);
                                    startActivity(intent);
                                    return;
                                } else {
                                    Toaster.makeToast(this, "File is greater than 8 MB, or is corrupt.", Toast.LENGTH_LONG);
                                    finish();
                                    return;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toaster.makeToast(this, "Error loading file.", Toast.LENGTH_LONG);
                                finish();
                                return;
                            }
                        }
                    }
                }
            } else if(receivedAction.equals(Intent.ACTION_MAIN)) {

            }
            startMain();
        }
    }

    private long getAudioDuration(File audioFile) {
        long duration = 0;

        if(audioFile != null) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(audioFile.getPath());
            String durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if(durationStr != null) {
                duration = Long.parseLong(durationStr);
            }
        }
        return duration;
    }

    private boolean validMimeType(File audioFile) {
        if(audioFile != null) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(audioFile.getPath());
            String mimeTypeStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            if(mimeTypeStr != null &&
                    (mimeTypeStr.contains("audio/mpeg")
                    || mimeTypeStr.contains("audio/mp4")
                    || mimeTypeStr.contains("audio/3gpp"))) {
                return true;
            } else if(mimeTypeStr != null) {
                Toaster.makeToast(this, "Invalid audio mime-type " + mimeTypeStr, Toast.LENGTH_SHORT);
            }
        }
        return false;
    }

    private void startMain() {
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
                .child("minimum_requirements_android");
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
                        String buildVersionComponents[] = BuildConfig.VERSION_NAME.replaceAll("[^\\d.]", "").split("\\.");
                        String minVersionComponents[] = minimumRequirements.getApp_version().replaceAll("[^\\d.]", "").split("\\.");
                        int position = 0;
                        for (String component :
                                minVersionComponents) {
                            if (!component.isEmpty()) {
                                Integer componentInteger = Integer.valueOf(component);
                                if (position >= buildVersionComponents.length) break;
                                Integer buildComponentInteger = Integer.valueOf(buildVersionComponents[position]);
                                if(buildComponentInteger < componentInteger) {
                                    aboveMinReq = false;
                                    break;
                                } else if(buildComponentInteger > componentInteger) {
                                    aboveMinReq = true;
                                    break;
                                } else {
                                    position++;
                                }
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
