/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util

import com.crashlytics.android.Crashlytics
import com.google.firebase.crash.FirebaseCrash
import java.util.logging.Logger

/**
 * com.roostermornings.android.util
 * Rooster Mornings Android
 *
 * Created by bscholtz on 29/08/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */

class KotlinUtils {
    companion object {
        inline fun catchAll(action: () -> Unit) {
            try {
                action()
            } catch (t: Throwable) {
                Crashlytics.logException(t)
            }
        }
    }
}