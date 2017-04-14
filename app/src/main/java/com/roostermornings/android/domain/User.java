/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;

@IgnoreExtraProperties
public class User implements Serializable {

    private HashMap<String, Boolean> channels;
    private String device_type;
    private String device_token;
    private String profile_pic;
    private String user_name;
    private String cell_number;
    private HashMap<String, Boolean> friends;
    private String uid;
    private Integer unseen_roosters;

    @Exclude
    private Boolean selected = false; //this is important for list of friends that need to be selected eg for creating a new alarm

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public User() {
    }

    public User(HashMap<String, Boolean> channels,
                String device_type,
                String device_token,
                String profile_pic,
                String user_name,
                String cell_number,
                String uid,
                HashMap<String, Boolean> friends,
                Integer unseen_roosters) {

        this.channels = channels;
        this.cell_number = cell_number;
        this.device_token = device_token;
        this.device_type = device_type;
        this.profile_pic = profile_pic;
        this.user_name = user_name;
        this.uid = uid;
        this.unseen_roosters = unseen_roosters;
    }

    public Integer getUnseen_roosters() {
        return unseen_roosters;
    }

    public void setUnseen_roosters(Integer unseen_roosters) {
        this.unseen_roosters = unseen_roosters;
    }

    public HashMap<String, Boolean> getFriends() {
        return friends;
    }

    public void setFriends(HashMap<String, Boolean> friends) {
        this.friends = friends;
    }

    public HashMap<String, Boolean> getChannels() {
        return channels;
    }

    public void setChannels(HashMap<String, Boolean> channels) {
        this.channels = channels;
    }

    public String getDevice_type() {
        return device_type;
    }

    public void setDevice_type(String device_type) {
        this.device_type = device_type;
    }

    public String getDevice_token() {
        return device_token;
    }

    public void setDevice_token(String device_token) {
        this.device_token = device_token;
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

    public String getCell_number() {
        return cell_number;
    }

    public void setCell_number(String cell_number) {
        this.cell_number = cell_number;
    }

    @Exclude
    public Boolean getSelected() {
        return this.selected;
    }

    @Exclude
    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
