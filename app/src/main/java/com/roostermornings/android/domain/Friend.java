/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class Friend implements Serializable {
    private String uid;
    private String user_name;
    private String profile_pic;
    private String cell_number;

    //Using Firebase @Exclude annotation ensures that property is not UPLOADED to db - include above getter/setter for proper operation
    @Exclude
    private Boolean selected; //this is important for list of friends that need to be selected eg for creating a new alarm

    public Friend(){}

    public Friend(String uid, String user_name, String profile_pic, String cell_number) {
        this.uid = uid;
        this.user_name = user_name;
        this.profile_pic = profile_pic;
        this.cell_number = cell_number;
    }

    @Exclude
    public Boolean getSelected() {
        return selected;
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

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getProfile_pic() {
        return profile_pic;
    }

    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }

    public String getCell_number() {
        return cell_number;
    }

    public void setCell_number(String cell_number) {
        this.cell_number = cell_number;
    }
}
