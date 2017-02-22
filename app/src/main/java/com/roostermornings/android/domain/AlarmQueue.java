package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by steven on 2017/02/15.
 */

@IgnoreExtraProperties
public class AlarmQueue {

    private String queue_id;
    private String alarm_id;
    private String audio_file_url;
    private String cell_number;
    private String profile_pic;
    private String user_name;
    private String sender_id;
    private long date_uploaded;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public AlarmQueue() {
    }

    public AlarmQueue(String alarm_id, String audio_file_url, String cell_number, String profile_pic, String user_name, String queue_id, String sender_id, long date_uploaded) {
        this.alarm_id = alarm_id;
        this.audio_file_url = audio_file_url;
        this.cell_number = cell_number;
        this.profile_pic = profile_pic;
        this.user_name = user_name;
        this.queue_id = queue_id;
        this.sender_id = sender_id;
        this.date_uploaded = date_uploaded;
    }

    public long getDate_uploaded() {
        return date_uploaded;
    }

    public void setDate_uploaded(long date_created) {
        this.date_uploaded = date_created;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getQueue_id() {
        return queue_id;
    }

    public void setQueue_id(String queue_id) {
        this.queue_id = queue_id;
    }

    public String getAlarm_id() {
        return alarm_id;
    }

    public void setAlarm_id(String alarm_id) {
        this.alarm_id = alarm_id;
    }

    public String getAudio_file_url() {
        return audio_file_url;
    }

    public void setAudio_file_url(String audio_file_url) {
        this.audio_file_url = audio_file_url;
    }

    public String getCell_number() {
        return cell_number;
    }

    public void setCell_number(String cell_number) {
        this.cell_number = cell_number;
    }

    public String getProfile_pic() {
        return profile_pic;
    }

    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }
}



