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

public class LifeCycle {

    private Context context;

    @Inject SharedPreferences sharedPreferences;
    @Inject AudioTableManager audioTableManager;

    private static final String firstEntry = "shared_pref_lifecycle_first_entry";

    public LifeCycle(Context context) {
        this.context = context;
        BaseApplication.getRoosterApplicationComponent().inject(this);
    }

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

    private void setFirstEntry() {
        sharedPreferences.edit()
                .putBoolean(firstEntry, false)
                .apply();
    }
}
