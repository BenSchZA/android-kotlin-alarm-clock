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
    private String queue_id = "";
    private String filename = "";
    private long date_uploaded = 0;
    private String sender_id = "";
    private String name = "";
    private String picture = "";
    private String listened;
    //Type 1 = channel, type 0 = social
    private int type = -1;
    private String action_title = "";
    private String action_url = "";
    private String source_url = "";

    public void fromSocialRooster(SocialRooster socialRooster, String uniqueFileName) {
        this.queue_id = socialRooster.getQueue_id();
        this.filename = uniqueFileName;
        this.date_uploaded = socialRooster.getDate_uploaded();
        this.sender_id = socialRooster.getSender_id();
        this.name = socialRooster.getUser_name();
        this.picture = socialRooster.getProfile_pic();
        this.listened = String.valueOf(socialRooster.getListened());
        this.type = 0;
        this.source_url = socialRooster.getAudio_file_url();
    }

    public void fromChannelRooster(ChannelRooster channelRooster, String uniqueFileName) {
        this.queue_id = channelRooster.getChannel_uid();
        this.filename = uniqueFileName;
        // Channel doesn't send channel ID this.sender_id = ;
        this.name = channelRooster.getName();
        this.picture = channelRooster.getPhoto();
        this.type = 1;
        this.action_title = channelRooster.getAction_title();
        this.action_url = channelRooster.getAction_url();
        this.source_url = channelRooster.getAudio_file_url();
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

    public long getDate_uploaded() {
        return date_uploaded;
    }

    public void setDate_uploaded(long date_uploaded) {
        this.date_uploaded = date_uploaded;
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
