/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.widgets

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.IBinder
import com.roostermornings.android.util.KotlinUtils

class WidgetService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when(intent.action) {
                Intent.ACTION_TIME_TICK -> {
                    //Update app widget
                    val updateWidgetIntent = Intent(ctx, AlarmToggleWidget::class.java)
                    updateWidgetIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    val appWidgetManager = AppWidgetManager.getInstance(ctx.applicationContext)
                    val ids = appWidgetManager.getAppWidgetIds(ComponentName(ctx.applicationContext, AlarmToggleWidget::class.java))
                    updateWidgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    ctx.sendBroadcast(updateWidgetIntent)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Register system 1 minute tick broadcast receiver
        KotlinUtils.catchAll { unregisterReceiver(receiver) }
        registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        KotlinUtils.catchAll { unregisterReceiver(receiver) }
    }
}
