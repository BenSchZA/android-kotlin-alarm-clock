/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by steven on 2017/02/10.
 */
@IgnoreExtraProperties
public class SocialRooster {

    private String audio_file_url;
    private String user_name;
    private Boolean listened;
    private String profile_pic;
    private Long date_uploaded;
    private String receiver_id;
    private String queue_id;
    private String sender_id;

    @Exclude
    private Integer status;

    public SocialRooster(String audio_file_url, String user_name,
                         Boolean listened, String profile_pic, Long date_uploaded,
                         String receiver_id, String queue_id, String sender_id) {
        this.audio_file_url = audio_file_url;
        this.user_name = user_name;
        this.listened = listened;
        this.profile_pic = profile_pic;
        this.date_uploaded = date_uploaded;
        this.receiver_id = receiver_id;
        this.queue_id = queue_id;
        this.sender_id = sender_id;
    }

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public SocialRooster() {
    }

    @Exclude
    public Integer getStatus() {
        return status;
    }

    @Exclude
    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getQueue_id() {
        return queue_id;
    }

    public void setQueue_id(String queue_id) {
        this.queue_id = queue_id;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getAudio_file_url() {
        return audio_file_url;
    }

    public void setAudio_file_url(String audio_file_url) {
        this.audio_file_url = audio_file_url;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public Boolean getListened() {
        return listened;
    }

    public void setListened(Boolean listened) {
        this.listened = listened;
    }

    public String getProfile_pic() {
        return profile_pic;
    }

    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }

    public Long getDate_uploaded() {
        return date_uploaded;
    }

    public void setDate_uploaded(Long date_uploaded) {
        this.date_uploaded = date_uploaded;
    }

    public String getReceiver_id() {
        return receiver_id;
    }

    public void setReceiver_id(String receiver_id) {
        this.receiver_id = receiver_id;
    }
}
