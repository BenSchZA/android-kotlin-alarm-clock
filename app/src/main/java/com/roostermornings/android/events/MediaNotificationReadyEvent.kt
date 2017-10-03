/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.events

import android.app.Notification
import android.content.Context
import android.support.v4.app.NotificationManagerCompat

/**
 * com.roostermornings.android.events
 * Rooster Mornings Android
 *
 * Created by bscholtz on 28/09/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */

class MediaNotificationReadyEvent(var context: Context, var notification: Notification) {

    var mNotificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    fun postNotification() {
        mNotificationManager.notify(1, notification)
    }
}