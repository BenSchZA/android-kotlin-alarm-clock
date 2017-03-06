package com.roostermornings.android.domain;

import com.google.gson.annotations.Expose;

/**
 * Created by bscholtz on 06/03/17.
 */

public class NodeUser {

    @Expose
    private String id;
    @Expose
    private String user_name;
    @Expose
    private String profile_pic;
    @Expose
    private String cell_number;

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
