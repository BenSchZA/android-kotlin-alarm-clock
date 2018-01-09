/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roostermornings.android.util.RoosterUtils
import com.roostermornings.android.widgets.AlarmToggleWidget
import com.roostermornings.android.widgets.WidgetService

class WidgetBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(RoosterUtils.hasM()) {
            //AlarmToggleWidget.sendUpdateBroadcast(context)
        } else {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> context.startService(Intent(context, WidgetService::class.java))
                Intent.ACTION_DREAMING_STOPPED -> context.startService(Intent(context, WidgetService::class.java))
                Intent.ACTION_BOOT_COMPLETED -> context.startService(Intent(context, WidgetService::class.java))
            }
        }
    }
}
