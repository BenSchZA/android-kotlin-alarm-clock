/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.roostermornings.android.R
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.widget.Toast




/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [AlarmToggleWidgetConfigureActivity]
 */
class AlarmToggleWidget : AppWidgetProvider() {

    companion object {
        val TOAST_ACTION = "AlarmToggleWidget.TOAST_ACTION"
        val EXTRA_ITEM = "AlarmToggleWidget.EXTRA_ITEM"
        val EXTRA_ALARM_UID = "AlarmToggleWidget.EXTRA_ALARM_UID"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                    appWidgetId: Int) {

            // Sets up the intent that points to the StackViewService that will
            // provide the views for this collection.
            val intent = Intent(context, AlarmToggleWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // When intents are compared, the extras are ignored, so we need to embed the extras
            // into the data so that the extras will not be ignored.
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            val rv = RemoteViews(context.packageName, R.layout.widget_alarm_toggle)
            rv.setRemoteAdapter(R.id.widget_alarmsListView, intent)

            // The empty view is displayed when the collection has no items. It should be a sibling
            // of the collection view.
            rv.setEmptyView(R.id.widget_alarmsListView, R.id.widget_alarmsListView)

            // This section makes it possible for items to have individualized behavior.
            // It does this by setting up a pending intent template. Individuals items of a collection
            // cannot set up their own pending intents. Instead, the collection as a whole sets
            // up a pending intent template, and the individual items set a fillInIntent
            // to create unique behavior on an item-by-item basis.
            val toastIntent = Intent(context, AlarmToggleWidget::class.java)
            // Set the action for the intent.
            // When the user touches a particular view, it will have the effect of
            // broadcasting TOAST_ACTION.
            toastIntent.action = TOAST_ACTION
            toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            val toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            rv.setPendingIntentTemplate(R.id.widget_alarmsListView, toastPendingIntent)

//            /** PendingIntent to launch the MainActivity when the widget was clicked **/
//            val launchMain = Intent(context, MyAlarmsFragmentActivity::class.java)
//            val pendingMainIntent = PendingIntent.getActivity(context, 0, launchMain, 0)
//            views.setOnClickPendingIntent(R.id.widget_alarm_toggle, pendingMainIntent)
//            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_alarmsListView)

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        }
    }

    // Called when the BroadcastReceiver receives an Intent broadcast.
    // Checks to see whether the intent's action is TOAST_ACTION. If it is, the app widget
    // displays a Toast message for the current item.
    override fun onReceive(context: Context, intent: Intent) {
        val mgr = AppWidgetManager.getInstance(context)
        if (intent.action == TOAST_ACTION) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)
            val viewIndex = intent.getIntExtra(EXTRA_ITEM, 0)
            Toast.makeText(context, "Touched view " + viewIndex, Toast.LENGTH_SHORT).show()
        }
        super.onReceive(context, intent)
    }


    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            AlarmToggleWidgetConfigureActivity.deleteTitlePref(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

