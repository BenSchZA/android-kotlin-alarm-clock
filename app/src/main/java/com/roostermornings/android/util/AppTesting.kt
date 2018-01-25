package com.roostermornings.android.util

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Created by bscholtz on 2018/01/24.
 */
class AppTesting {

    companion object {
        fun isDebuggable(context: Context): Boolean {
            return 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        }
    }
}