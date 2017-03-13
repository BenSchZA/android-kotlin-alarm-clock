/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.gson.annotations.Expose;

/**
 * Created by bscholtz on 06/03/17.
 */

//This class maps the JSON data received from Node to a user object
public class NodeUser {

    @Expose
    private String id;
    @Expose
    private String user_name;
    @Expose
    private String profile_pic;
    @Expose
    private String cell_number;

    private Boolean selected; //this is important for list of friends that need to be selected

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public NodeUser() {
    }

    public NodeUser(String id, String user_name, String profile_pic, String cell_number) {
        this.id = id;
        this.user_name = user_name;
        this.profile_pic = profile_pic;
        this.cell_number = cell_number;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
