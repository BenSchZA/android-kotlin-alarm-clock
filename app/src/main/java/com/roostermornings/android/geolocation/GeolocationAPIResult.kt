/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.geolocation

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
        var lat : Double? = 0.0
        @Expose
        @SerializedName("lng")
        var lng : Double? = 0.0

        constructor()

        constructor(lat: Double?, lng: Double?) {
            this.lat = lat
            this.lng = lng
        }
    }

    @Expose
    @SerializedName("location")
    var location : Location = Location()
    @Expose
    @SerializedName("accuracy")
    var accuracy : Double? = 0.0

    constructor()

    constructor(location: Location, accuracy: Double) {
        this.location = location
        this.accuracy = accuracy
    }
}