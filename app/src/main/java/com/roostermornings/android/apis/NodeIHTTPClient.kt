/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.apis

import com.roostermornings.android.domain.FCMPayloadSocialRooster
import com.roostermornings.android.domain.LocalContacts
import com.roostermornings.android.domain.NodeAPIResult
import com.roostermornings.android.domain.NodeUsers
import com.roostermornings.android.domain.Users

import retrofit.Call
import retrofit.http.Body
import retrofit.http.GET
import retrofit.http.POST
import retrofit.http.Path

interface NodeIHTTPClient {

    //Deprecated
    @GET("api/friends/{userId}")
    fun listUserFriendList(@Path("userId") userId: String): Call<Users>

    //Uses boolean value for friends in profile
    @GET("api/my_friends/{tokenId}")
    fun retrieveUserFriends(@Path("tokenId") tokenId: String): Call<Users>

    @POST("api/my_contacts")
    fun checkLocalContacts(@Body body: LocalContacts): Call<NodeUsers>

    @POST("api/social_upload_notification")
    fun notifySocialUploadRecipient(@Body body: FCMPayloadSocialRooster): Call<NodeAPIResult>
}
