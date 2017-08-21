/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.apis

import com.roostermornings.android.domain.*
import retrofit.Call
import retrofit.http.Body
import retrofit.http.POST
import retrofit.http.Path
import retrofit.http.Query

/**
 * com.roostermornings.android.apis
 * Rooster Mornings Android
 *
 * Created by bscholtz on 21/08/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */

interface GoogleIHTTPClient {

    @POST("geolocation/v1/geolocate")
    fun getGeolocation(@Query("key") apiKey : String, @Body body: GeolocationRequest): Call<GeolocationAPIResult>
}