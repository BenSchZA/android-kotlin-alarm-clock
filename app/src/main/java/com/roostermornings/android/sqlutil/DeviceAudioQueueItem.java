/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import com.google.firebase.database.Exclude;
import com.roostermornings.android.domain.database.ChannelRooster;
import com.roostermornings.android.domain.database.SocialRooster;
import com.roostermornings.android.util.Constants;

import java.io.Serializable;

/**
 * Created by bscholtz on 2/21/17.
 */

public class DeviceAudioQueueItem implements Serializable {
    private int id = -1;
    //Queue id also acts as alarm id
    private String queue_id = "";
    private String filename = "";
    private Long date_uploaded;
    private String sender_id = "";
    private String name = "";
    private String picture = "";
    private String listened = "0";
    //Type 1 = channel, type 0 = social
    private int type = -1;
    private int favourite = Constants.AUDIO_TYPE_FAVOURITE_FALSE;
    private String action_title = "";
    private String action_url = "";
    private String source_url = "";
    private Long date_created;

    //For today and favourite roosters
    @Exclude
    private boolean downloading = false;
    @Exclude
    private boolean playing = false;
    @Exclude
    private boolean paused = false;

    public void fromSocialRooster(SocialRooster socialRooster, String uniqueFileName) {
        this.queue_id = socialRooster.getQueue_id();
        this.filename = uniqueFileName;
        this.date_uploaded = socialRooster.getDate_uploaded();
        this.sender_id = socialRooster.getSender_id();
        this.name = socialRooster.getUser_name();
        this.picture = socialRooster.getProfile_pic();
        this.listened = String.valueOf(socialRooster.getListened());
        this.type = 0;
        this.favourite = 0;
        this.source_url = socialRooster.getAudio_file_url();
    }

    public void fromChannelRooster(ChannelRooster channelRooster, String uniqueFileName) {
        this.queue_id = channelRooster.getChannel_uid();
        this.filename = uniqueFileName;
        // Channel doesn't send channel ID this.sender_id = ;
        this.name = channelRooster.getName();
        this.picture = channelRooster.getPhoto();
        this.type = 1;
        this.favourite = 0;
        this.action_title = channelRooster.getAction_title();
        this.action_url = channelRooster.getAction_url();
        this.source_url = channelRooster.getAudio_file_url();
    }

    public DeviceAudioQueueItem(){}

    public DeviceAudioQueueItem(DeviceAudioQueueItem audioQueueItem) {
        this.queue_id = audioQueueItem.queue_id;
        this.filename = audioQueueItem.filename;
        this.date_uploaded = audioQueueItem.date_uploaded;
        this.sender_id = audioQueueItem.sender_id;
        this.name = audioQueueItem.name;
        this.picture = audioQueueItem.picture;
        this.listened = audioQueueItem.listened;
        this.type = audioQueueItem.type;
        this.favourite = audioQueueItem.favourite;
        this.action_title = audioQueueItem.action_title;
        this.action_url = audioQueueItem.action_url;
        this.source_url = audioQueueItem.source_url;
        this.date_created = audioQueueItem.date_created;
    }

    @Exclude
    public boolean isPaused() {
        return paused;
    }

    @Exclude
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Exclude
    public boolean isDownloading() {
        return downloading;
    }

    @Exclude
    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
    }

    @Exclude
    public boolean isPlaying() {
        return playing;
    }

    @Exclude
    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public int getFavourite() {
        return favourite;
    }

    public void setFavourite(int favourite) {
        this.favourite = favourite;
    }

    public String getSource_url() {
        return source_url;
    }

    public void setSource_url(String source_url) {
        this.source_url = source_url;
    }

    public String getAction_title() {
        return action_title;
    }

    public void setAction_title(String action_title) {
        this.action_title = action_title;
    }

    public String getAction_url() {
        return action_url;
    }

    public void setAction_url(String action_url) {
        this.action_url = action_url;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getListened() {
        return listened;
    }

    public void setListened(String listened) {
        this.listened = listened;
    }

    public Long getDate_uploaded() {
        return date_uploaded;
    }

    public void setDate_uploaded(Long date_uploaded) {
        this.date_uploaded = date_uploaded;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQueue_id() {
        return queue_id;
    }

    public void setQueue_id(String queue_id) {
        this.queue_id = queue_id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public Long getDate_created() {
        return date_created;
    }

    public void setDate_created(Long date_created) {
        this.date_created = date_created;
    }
}
