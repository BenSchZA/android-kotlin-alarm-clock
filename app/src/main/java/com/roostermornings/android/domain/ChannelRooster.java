/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ChannelRooster {
    private String name = "";
    private boolean active = false;
    private String audio_file_name = "";
    private String audio_file_url = "";
    private String channel_uid = "";
    private String description = "";
    private String photo = "";
    private int rooster_cycle_iteration = -1;
    private long upload_date = -1;
    private String action_title = "";
    private String action_url = "";

    @Exclude
    private Boolean selected = false;
    @Exclude
    private String channel_description = "";
    @Exclude
    private String channel_photo = "";

    @Exclude
    private boolean downloading = false;
    @Exclude
    private boolean playing = false;
    @Exclude
    private boolean paused = false;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public ChannelRooster() {
    }

    public ChannelRooster(String name, boolean active, String audio_file_name, String audio_file_url, String channel_uid, String description, String photo, int rooster_cycle_iteration, long upload_date, String action_title, String action_url) {
        this.name = name;
        this.active = active;
        this.audio_file_name = audio_file_name;
        this.audio_file_url = audio_file_url;
        this.channel_uid = channel_uid;
        this.description = description;
        this.photo = photo;
        this.rooster_cycle_iteration = rooster_cycle_iteration;
        this.upload_date = upload_date;
        this.action_title = action_title;
        this.action_url = action_url;
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

    @Exclude
    public String getChannel_description() {
        return channel_description;
    }

    @Exclude
    public void setChannel_description(String channel_description) {
        this.channel_description = channel_description;
    }

    @Exclude
    public String getChannel_photo() {
        return channel_photo;
    }

    @Exclude
    public void setChannel_photo(String channel_photo) {
        this.channel_photo = channel_photo;
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
