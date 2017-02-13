package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by steven on 2017/02/10.
 */
@IgnoreExtraProperties
public class SocialRooster {

    private String audio_file_url;
    private String cell_number;
    private String user_name;
    private Boolean listened;
    private String profile_pic;
    private Long date_uploaded;
    private String uid;

    public SocialRooster(String audio_file_url, String cell_number, String user_name, Boolean listened, String profile_pic, Long date_uploaded, String uid) {
        this.audio_file_url = audio_file_url;
        this.cell_number = cell_number;
        this.user_name = user_name;
        this.listened = listened;
        this.profile_pic = profile_pic;
        this.date_uploaded = date_uploaded;
        this.uid = uid;
    }

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public SocialRooster() {
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

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
