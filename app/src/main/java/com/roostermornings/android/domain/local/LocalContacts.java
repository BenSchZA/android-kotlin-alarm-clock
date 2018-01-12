/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain.local;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by bscholtz on 06/03/17.
 */

@IgnoreExtraProperties
public class LocalContacts {

    @Expose @SerializedName("user_contacts")
    private ArrayList<String> user_contacts;
    @Expose @SerializedName("user_token_id")
    private String user_token_id;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public LocalContacts() {

    }

    public LocalContacts(ArrayList<String> user_contacts, String user_token_id) {
        this.user_contacts = user_contacts;
        this.user_token_id = user_token_id;
    }
}
