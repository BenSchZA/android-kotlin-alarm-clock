package com.roostermornings.android.domain;

/**
 * Created by bscholtz on 08/03/17.
 */

public class Friend {
    private String uid;
    private String user_name;
    private String profile_pic;
    private String cell_number;

    public Friend(){}

    public Friend(String uid, String user_name, String profile_pic, String cell_number) {
        this.uid = uid;
        this.user_name = user_name;
        this.profile_pic = profile_pic;
        this.cell_number = cell_number;
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
