/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class MinimumRequirements {
    private String app_version_string = "0.0.0";
    private boolean invalidate_user = false;
    private String update_title = "";
    private String update_description = "";
    private String update_link = "";

    @SuppressWarnings("unused")
    public MinimumRequirements() {
    }

    public MinimumRequirements(String app_version, boolean invalidate_user, String update_title, String update_description, String update_link) {
        this.app_version_string = app_version;
        this.invalidate_user = invalidate_user;
        this.update_title = update_title;
        this.update_description = update_description;
        this.update_link = update_link;
    }

    public String getApp_version_string() {
        return app_version_string;
    }

    public void setApp_version_string(String app_version_string) {
        this.app_version_string = app_version_string;
    }

    public boolean isInvalidate_user() {
        return invalidate_user;
    }

    public void setInvalidate_user(boolean invalidate_user) {
        this.invalidate_user = invalidate_user;
    }

    public String getUpdate_title() {
        return update_title;
    }

    public void setUpdate_title(String update_title) {
        this.update_title = update_title;
    }

    public String getUpdate_description() {
        return update_description;
    }

    public void setUpdate_description(String update_description) {
        this.update_description = update_description;
    }

    public String getUpdate_link() {
        return update_link;
    }

    public void setUpdate_link(String update_link) {
        this.update_link = update_link;
    }
}
