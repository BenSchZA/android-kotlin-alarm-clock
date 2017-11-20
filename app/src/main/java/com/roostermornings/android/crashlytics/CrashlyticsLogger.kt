package com.roostermornings.android.crashlytics

import com.crashlytics.android.Crashlytics

/**
 * Created by bscholtz on 2017/11/20.
 */

/** This class allows us to log a non-fatal exception, and have it appear under the same report. */
class CrashlyticsLogger {
    companion object {
        fun logException(throwable: Throwable?) {
            throwable?.let { Crashlytics.logException(throwable) }
        }
    }
}