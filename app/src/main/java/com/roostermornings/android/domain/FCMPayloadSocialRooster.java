package com.roostermornings.android.domain;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by steven on 2017/03/15.
 */

@IgnoreExtraProperties
public class FCMPayloadSocialRooster {

    @Expose @SerializedName("user_token_id")
    private String user_token_id;
    @Expose @SerializedName("recipient_user_uid")
    private String recipient_user_uid;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    public FCMPayloadSocialRooster() {

    }

    public FCMPayloadSocialRooster(String user_token_id, String recipient_user_uid) {
        this.user_token_id = user_token_id;
        this.recipient_user_uid = recipient_user_uid;
    }
}