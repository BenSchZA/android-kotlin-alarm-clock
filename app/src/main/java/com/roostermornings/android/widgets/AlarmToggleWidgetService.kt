/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.widgets

import android.content.Intent
import android.widget.RemoteViewsService

class AlarmToggleWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AlarmToggleWidgetDataProvider(this, intent)
    }
}
