/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;
import com.roostermornings.android.util.StrUtils;

import static com.roostermornings.android.util.RoosterUtils.notNull;

/**
 * Created by steven on 2017/02/16.
 */

@IgnoreExtraProperties
public class AlarmChannel {

    public String id = "";
    public String name = "Default tone";

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public AlarmChannel() {
    }

    public AlarmChannel(String name, String uid) {
        if(StrUtils.notNullOrEmpty(name)) this.name = name;
        if(StrUtils.notNullOrEmpty(uid)) this.id = uid;
    }

    public String getId() {
        return id;
    }

    public void setId(String uid) {
        this.id = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
