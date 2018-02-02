package com.roostermornings.android.service

import android.app.Service
import android.content.*
import android.os.IBinder
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.keys.PrefsKey
import com.roostermornings.android.util.KotlinUtils
import javax.inject.Inject

class ForegroundService : Service() {

    @Inject lateinit var sharedPreferences: SharedPreferences

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when(intent.action) {
                Intent.ACTION_TIME_TICK -> {
                    BaseApplication.isAppForeground = if(sharedPreferences
                            .getBoolean(PrefsKey.IS_ACTIVITY_FOREGROUND.name, false)) {
                        true
                    } else {
                        stopSelf()
                        false
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        BaseApplication.roosterApplicationComponent.inject(this)

        // Register system 1 minute tick broadcast receiver
        KotlinUtils.catchAll(false) { unregisterReceiver(receiver) }
        registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if(!BaseApplication.isAppForeground) {
            sendBroadcast(Intent(PrefsKey.APP_BROUGHT_FOREGROUND.name))
        }

        // Initially set foreground to true
        BaseApplication.isAppForeground = true

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        KotlinUtils.catchAll(false) { unregisterReceiver(receiver) }
    }
}
