/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.domain.database.ChannelRooster;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.sync.DownloadSyncAdapter;

import javax.inject.Inject;

import static com.roostermornings.android.util.Constants.USER_FINISHED_ONBOARDING;

/**
 * <h1>LifeCycle Class</h1>
 *
 * Handles the user lifecycle, i.e.
 *  - onboarding
 *  - first use guides
 *  - first social and channel roosters, etc.
 *
 * It does this by keeping a record of what the user has seen using sharedPrefs, etc.
 *
 * @author bscholtz
 * @version 1
 * @since 15/08/17
 */

public class LifeCycle {

    private Context context;

    @Inject SharedPreferences sharedPreferences;
    @Inject AudioTableManager audioTableManager;

    private static final String firstEntry = "shared_pref_lifecycle_first_entry";

    /**
     * Instantiate class and perform Dagger 2 dependency injection
     * @param context
     */

    public LifeCycle(Context context) {
        this.context = context;
        BaseApplication.getRoosterApplicationComponent().inject(this);
    }

    /**
     * Performs all tasks for the user's first entry, or inception into Rooster...
     * (sounds a bit like a cult...)
     * @return boolean isFirstEntry?
     */

    public boolean performInception() {
        if(sharedPreferences.getBoolean(firstEntry, true)) {

            createFillerChannel();

            sharedPreferences.edit()
                    .putBoolean(USER_FINISHED_ONBOARDING, true)
                    .apply();

            setFirstEntry();
            return true;
        } else {
            //If no methods performed
            return false;
        }
    }

    /**
     * This method is run on the [n]th new app launch, to request the user to rate the app.
     * https://stackoverflow.com/questions/10816757/rate-this-app-link-in-google-play-store-app-on-the-phone
     * */

    public void requestAppRating() {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        if(RoosterUtils.hasLollipop()) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.share_rooster_message));
        sendIntent.setType("text/plain");
        sendIntent = Intent.createChooser(sendIntent, "Share Rooster").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(sendIntent);
    }

    public void sendFeedback(String name) {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{  "ben.scholtz@roostermornings.com"});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, name + ": Hello there");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Add message here");
        emailIntent.setType("message/rfc822");

        try {
            context.startActivity(Intent.createChooser(emailIntent,
                    "Send feedback").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (android.content.ActivityNotFoundException ex) {
            Toaster.makeToast(context, "No email clients installed.", Toast.LENGTH_LONG);
        }
    }

    /**
     * This method creates the first channel the user sees, and inserts it in
     * today's roosters, as well as sets it as a favourite.
     */

    private void createFillerChannel() {
        final ChannelRooster channelRooster = new ChannelRooster(
                "Bird Calls",
                true,
                "",
                "https://firebasestorage.googleapis.com/v0/b/roosta-media.appspot.com/o/channel_roosters%2FBRD-002%20emerald_wooddove.mp3?alt=media&token=407fc34b-bb89-41d2-b307-31c1a17edf07",
                "-KkGc979MLXKA5VBfFtY",
                "Emerald Spotted Wood Dove",
                "https://firebasestorage.googleapis.com/v0/b/roosta-media.appspot.com/o/channel_roosters_photos%2Femerald%20spotted%20wood%20dove.jpg?alt=media&token=72479ea3-2094-44d7-b619-a5aa88463fd2",
                0,
                0,
                "",
                "");

        DownloadSyncAdapter downloadSyncAdapter = new DownloadSyncAdapter(context, true);

        DownloadSyncAdapter.setOnChannelDownloadListener(new DownloadSyncAdapter.OnChannelDownloadListener() {
            @Override
            public void onChannelDownloadStarted(String channelId) {
            }

            @Override
            public void onChannelDownloadComplete(boolean valid, String channelId) {
                DeviceAudioQueueItem audioItem = audioTableManager.extractChannelAudioItem(channelRooster.getChannel_uid());
                if(audioItem != null) {
                    audioTableManager.setListened(audioItem);
                    audioTableManager.setFavourite(audioItem.getId(), true);
                }
            }
        });

        downloadSyncAdapter.retrieveChannelContentAudio(channelRooster, context);
    }

    /**
     * Set the shared pref to indicate whether this is a user's first entry, this is persisted.
     */

    private void setFirstEntry() {
        sharedPreferences.edit()
                .putBoolean(firstEntry, false)
                .apply();
    }
}
