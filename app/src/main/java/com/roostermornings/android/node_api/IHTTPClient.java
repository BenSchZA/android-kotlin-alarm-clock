/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.node_api;

import com.roostermornings.android.domain.LocalContacts;
import com.roostermornings.android.domain.NodeUsers;
import com.roostermornings.android.domain.Users;
import com.squareup.okhttp.ResponseBody;

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

}
