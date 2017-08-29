/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.geolocation.GeoHashUtils
import com.roostermornings.android.util.ConnectivityUtils
import javax.inject.Inject

class NetworkChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var connectivityUtils: ConnectivityUtils
    @Inject lateinit var geoHashUtils: GeoHashUtils

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if(connectivityUtils.isConnectedMobile()) {
            //Check if the user's geohash location entry is still valid
            geoHashUtils.checkUserGeoHash()
        }
    }

    companion object {
       fun registerReceiverSelf(context: Context) {
           val connectivityIntentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
           val networkChangeReceiver = NetworkChangeReceiver()
           context.registerReceiver(networkChangeReceiver, connectivityIntentFilter)
       }
    }
}
