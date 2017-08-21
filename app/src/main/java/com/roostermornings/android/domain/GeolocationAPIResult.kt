/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.domain

import com.google.firebase.database.IgnoreExtraProperties
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * com.roostermornings.android.domain
 * Rooster Mornings Android
 *
 * Created by bscholtz on 21/08/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */
@IgnoreExtraProperties
class GeolocationAPIResult {
    class Location {
        @Expose
        @SerializedName("lat")
        var lat : Float? = 0f
        @Expose
        @SerializedName("lng")
        var lng : Float? = 0f

        constructor()

        constructor(lat: Float?, lng: Float?) {
            this.lat = lat
            this.lng = lng
        }
    }

    @Expose
    @SerializedName("location")
    var location : Location = Location()
    @Expose
    @SerializedName("accuracy")
    var accuracy : Float? = 0f

    constructor()

    constructor(location: Location, accuracy: Float) {
        this.location = location
        this.accuracy = accuracy
    }
}