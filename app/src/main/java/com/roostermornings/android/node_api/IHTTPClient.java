/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.node_api;

import com.roostermornings.android.domain.FCMPayloadSocialRooster;
import com.roostermornings.android.domain.LocalContacts;
import com.roostermornings.android.domain.NodeAPIResult;
import com.roostermornings.android.domain.NodeUsers;
import com.roostermornings.android.domain.Users;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface IHTTPClient {

    @GET("api/friends/{userId}")
    Call<Users> listUserFriendList(@Path("userId") String userId);

    @POST("api/my_contacts")
    Call<NodeUsers> checkLocalContacts(@Body LocalContacts body);

    @POST("api/social_upload_notification")
    Call<NodeAPIResult> notifySocialUploadRecipient(@Body FCMPayloadSocialRooster body);

}
