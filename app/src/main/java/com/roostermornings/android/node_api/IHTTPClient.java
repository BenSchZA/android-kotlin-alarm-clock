package com.roostermornings.android.node_api;

import com.roostermornings.android.domain.Users;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;

public interface IHTTPClient {

    @GET("api/friends/{userId}")
    Call<Users> listUserFriendList(@Path("userId") String userId);


}
