package com.roostermornings.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.roostermornings.android.sqlutil.DeviceAlarmController

/**
 * Created by bscholtz on 2018/02/02.
 */
class UpgradeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action != "android.intent.action.PACKAGE_REPLACED")
            return

        if(intent.data?.toString() == "package:${context?.packageName}") {
            // Application was upgraded
            Log.i(this.javaClass.simpleName, "PACKAGE_REPLACED action received for Rooster")
            context?.let {
                DeviceAlarmController(it).rebootAlarms()
            }
        }
    }
}