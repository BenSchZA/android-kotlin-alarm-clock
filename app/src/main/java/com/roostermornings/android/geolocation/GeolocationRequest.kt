/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.geolocation

import android.content.Context
import android.telephony.TelephonyManager
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
class GeolocationRequest {

    @Expose
    @SerializedName("homeMobileCountryCode")
    private var homeMobileCountryCode : Int? = null
    @Expose
    @SerializedName("homeMobileNetworkCode")
    private var homeMobileNetworkCode : Int? = null
    @Expose
    @SerializedName("radioType")
    private var radioType : String? = null
    @Expose
    @SerializedName("carrier")
    private var carrier : String? = null
    @Expose
    @SerializedName("considerIp")
    private var considerIp : String = "True"
    @Expose
    @SerializedName("cellTowers")
    private var cellTowers : ArrayList<GeolocationCellTowers>? = null
    @Expose
    @SerializedName("wifiAccessPoints")
    private var wifiAccessPoints : GeolocationWiFiAccessPoints? = null

    // Required default constructor for Firebase object mapping
    constructor()

    constructor(context: Context, useIPOnly: Boolean) {

        //Fallback to IP if requested, by making other data null
        if(!useIPOnly) {
            val telephonyManager: TelephonyManager? = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            val networkOperator: String? = telephonyManager?.networkOperator

            this.homeMobileCountryCode = networkOperator?.substring(0, 3)?.toInt()
            this.homeMobileNetworkCode = networkOperator?.substring(3)?.toInt()
            this.radioType = when (telephonyManager?.networkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "lte"
                TelephonyManager.NETWORK_TYPE_GSM -> "gsm"
                TelephonyManager.NETWORK_TYPE_CDMA -> "cdma"
                TelephonyManager.NETWORK_TYPE_HSDPA -> "wcdma"
                else -> null
            }
            this.carrier = telephonyManager?.simOperatorName

            this.wifiAccessPoints = GeolocationWiFiAccessPoints(context)
        }
    }
}