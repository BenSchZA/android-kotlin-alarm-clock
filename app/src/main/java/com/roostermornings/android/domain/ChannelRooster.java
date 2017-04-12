/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ChannelRooster {
    private String name;
    private boolean active;
    private String audio_file_name;
    private String audio_file_url;
    private String channel_uid;
    private String description;
    private String photo;
    private int rooster_cycle_iteration;
    private long upload_date;

    @Exclude
    private Boolean selected;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public ChannelRooster() {
    }

    public ChannelRooster(String name, boolean active, String audio_file_name, String audio_file_url, String channel_uid, String description, String photo, int rooster_cycle_iteration, long upload_date) {
        this.name = name;
        this.active = active;
        this.audio_file_name = audio_file_name;
        this.audio_file_url = audio_file_url;
        this.channel_uid = channel_uid;
        this.description = description;
        this.photo = photo;
        this.rooster_cycle_iteration = rooster_cycle_iteration;
        this.upload_date = upload_date;
    }

    @Exclude
    public boolean isSelected() {
        return selected;
    }

    @Exclude
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAudio_file_name() {
        return audio_file_name;
    }

    public void setAudio_file_name(String audio_file_name) {
        this.audio_file_name = audio_file_name;
    }

    public String getAudio_file_url() {
        return audio_file_url;
    }

    public void setAudio_file_url(String audio_file_url) {
        this.audio_file_url = audio_file_url;
    }

    public String getChannel_uid() {
        return channel_uid;
    }

    public void setChannel_uid(String channel_uid) {
        this.channel_uid = channel_uid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getRooster_cycle_iteration() {
        return rooster_cycle_iteration;
    }

    public void setRooster_cycle_iteration(int rooster_cycle_iteration) {
        this.rooster_cycle_iteration = rooster_cycle_iteration;
    }

    public long getUpload_date() {
        return upload_date;
    }

    public void setUpload_date(long upload_date) {
        this.upload_date = upload_date;
    }
}
