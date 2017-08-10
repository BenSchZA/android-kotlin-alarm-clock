/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;

import static com.roostermornings.android.util.RoosterUtils.notNull;

@IgnoreExtraProperties
public class User implements Serializable {

    public HashMap<String, Boolean> channels;
    public String device_type = "";
    public String device_token = "";
    public String profile_pic = "";
    public String user_name = "";
    public String cell_number = "";
    public HashMap<String, Object> friends;
    public String uid = "";
    public Integer unseen_roosters = 0;

    @Exclude
    private boolean selected = false; //this is important for list of friends that need to be selected eg for creating a new alarm

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
                HashMap<String, Object> friends,
                Integer unseen_roosters) {

        if(notNull(channels)) this.channels = channels;
        if(notNull(cell_number)) this.cell_number = cell_number;
        if(notNull(device_token)) this.device_token = device_token;
        if(notNull(device_type)) this.device_type = device_type;
        if(notNull(profile_pic)) this.profile_pic = profile_pic;
        if(notNull(user_name)) this.user_name = user_name;
        if(notNull(uid)) this.uid = uid;
        if(notNull(unseen_roosters)) this.unseen_roosters = unseen_roosters;
    }

    public Integer getUnseen_roosters() {
        return unseen_roosters;
    }

    public void setUnseen_roosters(Integer unseen_roosters) {
        this.unseen_roosters = unseen_roosters;
    }

    public HashMap<String, Object> getFriends() {
        return friends;
    }

    public void setFriends(HashMap<String, Object> friends) {
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
    public boolean getSelected() {
        return this.selected;
    }

    @Exclude
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
