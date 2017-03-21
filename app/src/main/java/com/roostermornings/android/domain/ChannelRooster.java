/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ChannelRooster {
    private String alarm_uid;
    private String audio_file_url;
    private String description;
    private String name;
    private String photo;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public ChannelRooster() {
    }

    public ChannelRooster(String alarm_uid, String audio_file_url, String description, String name, String photo) {
        this.alarm_uid = alarm_uid;
        this.audio_file_url = audio_file_url;
        this.description = description;
        this.name = name;
        this.photo = photo;
    }

    public String getAlarm_uid() {
        return alarm_uid;
    }

    public void setAlarm_uid(String alarm_uid) {
        this.alarm_uid = alarm_uid;
    }

    public String getAudio_file_url() {
        return audio_file_url;
    }

    public void setAudio_file_url(String audio_file_url) {
        this.audio_file_url = audio_file_url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
