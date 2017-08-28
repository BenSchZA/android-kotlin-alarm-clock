/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.sync.DownloadSyncAdapter;

import javax.inject.Inject;

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

            setFirstEntry();
            return true;
        } else {
            //If no methods performed
            return false;
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
        downloadSyncAdapter.retrieveChannelContentAudio(channelRooster, context);

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
