package com.roostermornings.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.roostermornings.android.sqlutil.DeviceAlarmController
import com.roostermornings.android.util.Toaster

/**
 * Created by bscholtz on 2018/02/02.
 */
class UpgradeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action != "android.intent.action.PACKAGE_REPLACED")
            return

        if(intent.data?.toString() == "package:${context?.packageName}") {
            // Application was upgraded
            DeviceAlarmController(context).rebootAlarms()

            Toaster.makeToast(context,
                    "Rooster was upgraded",
                    Toast.LENGTH_SHORT)
        }
    }
}