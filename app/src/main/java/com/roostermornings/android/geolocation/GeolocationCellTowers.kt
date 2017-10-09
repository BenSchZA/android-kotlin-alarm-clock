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
class GeolocationCellTowers {
    @Expose
    @SerializedName("cellId")
    private var cellId : Int? = 0
    @Expose
    @SerializedName("locationAreaCode")
    private var locationAreaCode : Int? = 0
    @Expose
    @SerializedName("mobileCountryCode")
    private var mobileCountryCode : Int? = 0
    @Expose
    @SerializedName("mobileNetworkCode")
    private var mobileNetworkCode : Int? = 0
    @Expose
    @SerializedName("age")
    private var age : Int? = 0
    @Expose
    @SerializedName("signalStrength")
    private var signalStrength : Int? = 0
    @Expose
    @SerializedName("timingAdvance")
    private var timingAdvance : Int? = 0

    constructor()

    constructor(cellId: Int, locationAreaCode: Int, mobileCountryCode: Int, mobileNetworkCode: Int, age: Int, signalStrength: Int, timingAdvance: Int) {
        this.cellId = cellId
        this.locationAreaCode = locationAreaCode
        this.mobileCountryCode = mobileCountryCode
        this.mobileNetworkCode = mobileNetworkCode
        this.age = age
        this.signalStrength = signalStrength
        this.timingAdvance = timingAdvance
    }
}