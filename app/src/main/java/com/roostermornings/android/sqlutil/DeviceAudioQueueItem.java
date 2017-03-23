/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.domain.SocialRooster;

import java.io.Serializable;

/**
 * Created by bscholtz on 2/21/17.
 */

public class DeviceAudioQueueItem implements Serializable {
    private int id;
    //Queue id also acts as alarm id
    private String queue_id;
    private String filename;
    private long date_created;
    private String sender_id;
    private String name;
    private String picture;
    private String listened;

    public void fromSocialRooster(SocialRooster socialRooster, String audioFileUrl) {
        this.queue_id = socialRooster.getQueue_id();
        this.filename = audioFileUrl;
        this.date_created = socialRooster.getDate_uploaded();
        this.sender_id = socialRooster.getSender_id();
        this.name = socialRooster.getUser_name();
        this.picture = socialRooster.getProfile_pic();
        this.listened = String.valueOf(socialRooster.getListened());
    }

    public void fromChannelRooster(ChannelRooster channelRooster, String audioFileUrl) {
        this.queue_id = channelRooster.getChannel_uid();
        this.filename = audioFileUrl;
        // Channel doesn't send channel ID this.sender_id = ;
        this.name = channelRooster.getChannel_name();
        this.picture = channelRooster.getPhoto();
    }

    public String getListened() {
        return listened;
    }

    public void setListened(String listened) {
        this.listened = listened;
    }

    public long getDate_created() {
        return date_created;
    }

    public void setDate_created(long date_created) {
        this.date_created = date_created;
    }

    public DeviceAudioQueueItem(){};

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
}
