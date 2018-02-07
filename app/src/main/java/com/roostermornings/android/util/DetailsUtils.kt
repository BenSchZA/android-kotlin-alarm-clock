package com.roostermornings.android.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.roostermornings.android.BuildConfig
import com.roostermornings.android.keys.AndroidCodeNames

/**
 * Created by bscholtz on 2018/01/24.
 */
class DetailsUtils {

    companion object {
        fun isDebuggable(context: Context): Boolean {
            return 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        }

        fun isBeta(): Boolean {
            return "BetaFlavour" == BuildConfig.FLAVOR
        }

        fun appVersionName(): String {
            return BuildConfig.VERSION_NAME
        }

        fun appVersionCode(): Int {
            return BuildConfig.VERSION_CODE
        }

        fun deviceBrand(): String {
            return Build.BRAND
        }

        fun deviceModel(): String {
            return Build.MODEL
        }

        fun androidVersionCodeName(): String {
            return AndroidCodeNames.values()[Build.VERSION.SDK_INT].name
        }

        fun androidVersionSdkInt(): Int {
            return Build.VERSION.SDK_INT
        }
    }
}