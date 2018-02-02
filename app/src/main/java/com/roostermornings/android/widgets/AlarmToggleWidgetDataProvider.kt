/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.widgets

/**
 * com.roostermornings.android.widgets
 * Rooster Mornings Android
 *
 * Created by bscholtz on 12/10/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.adapter_data.RoosterAlarmManager
import com.roostermornings.android.domain.database.Alarm
import com.roostermornings.android.util.JSONPersistence
import java.util.ArrayList
import javax.inject.Inject
import android.os.Bundle
import android.view.View
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.RoosterUtils
import javax.inject.Named


class AlarmToggleWidgetDataProvider
(private val context: Context, private val intent: Intent) : RemoteViewsService.RemoteViewsFactory {

    private var mAppWidgetId: Int
    private var mAppWidgetManager: AppWidgetManager

    val mAlarms: ArrayList<Alarm> = ArrayList()

    @Inject lateinit var roosterAlarmManager: RoosterAlarmManager
    @Inject lateinit var jsonPersistence: JSONPersistence
    @Inject @field:Named("default") lateinit var defaultSharedPreferences: SharedPreferences

    init {
        BaseApplication.roosterApplicationComponent.inject(this)
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID)
        mAppWidgetManager = AppWidgetManager.getInstance(context)
    }

    override fun onCreate() {
        mAlarms.addAll(jsonPersistence.alarms)

        RoosterAlarmManager.onFlagAlarmManagerDataListener = object: RoosterAlarmManager.Companion.OnFlagAlarmManagerDataListener {
            override fun onAlarmDataChanged(freshAlarms: ArrayList<Alarm>) {
                mAlarms.clear()
                mAlarms.addAll(freshAlarms)
                //mAppWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.widget_alarmsListView)
            }

            override fun onSyncFinished() {

            }
        }

        roosterAlarmManager.fetchAlarms(mAlarms)
    }

    override fun onDataSetChanged() {

    }

    override fun onDestroy() {

    }

    override fun getCount(): Int {
        return mAlarms.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        /** Populate your widget's single list item  */
        val remoteViews = RemoteViews(context.packageName, R.layout.list_layout_alarm_toggle_widget)

        val mAlarm = mAlarms[position]

        //Set 24-hour or 12-hour time format
        val timeFormat = defaultSharedPreferences.getBoolean(Constants.USER_SETTINGS_TIME_FORMAT, true)

        try {
            //If using 12 hour format
            if(!timeFormat) {
                val twelveHourTimeString: String
                if(mAlarm.hour >= 12) {
                    twelveHourTimeString = RoosterUtils.setAlarmTimeFromHourAndMinute(mAlarm.hour, mAlarm.minute, false) + context.resources.getString(R.string.alarm_12_hour_pm)
                    remoteViews.setTextViewText(R.id.cardview_alarm_time_textview, twelveHourTimeString)
                } else {
                    twelveHourTimeString = RoosterUtils.setAlarmTimeFromHourAndMinute(mAlarm.hour, mAlarm.minute, false) + context.resources.getString(R.string.alarm_12_hour_am)
                    remoteViews.setTextViewText(R.id.cardview_alarm_time_textview, twelveHourTimeString)
                }
            } else {
                remoteViews.setTextViewText(R.id.cardview_alarm_time_textview, RoosterUtils.setAlarmTimeFromHourAndMinute(mAlarm.hour, mAlarm.minute, true))
            }

            remoteViews.setTextViewText(R.id.cardview_alarm_days_textview, RoosterUtils.getAlarmDays(mAlarm))
            if(mAlarm.getChannel() != null) {
                remoteViews.setTextViewText(R.id.cardview_alarm_channel_textview, mAlarm.channel.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if(mAlarm.allow_friend_audio_files) {
            remoteViews.setViewVisibility(R.id.cardview_alarm_person, View.VISIBLE)
            //Show notification of number of waiting Roosters for next pending alarm
            if(mAlarm.unseen_roosters != null && mAlarm.unseen_roosters > 0) {
                remoteViews.setViewVisibility(R.id.rooster_notification_parent, View.VISIBLE)
                remoteViews.setTextViewText(R.id.rooster_notification_text, mAlarm.unseen_roosters.toString())
            } else{
                remoteViews.setViewVisibility(R.id.rooster_notification_parent, View.INVISIBLE)
            }
        } else {
            remoteViews.setViewVisibility(R.id.rooster_notification_parent, View.INVISIBLE)
        }

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in AlarmToggleWidget.
        val extras = Bundle()
        extras.putInt(AlarmToggleWidget.EXTRA_ITEM, position)
        extras.putString(AlarmToggleWidget.EXTRA_ALARM_UID, mAlarm.uid)
        val fillInIntent = Intent()
        fillInIntent.putExtras(extras)
        // Make it possible to distinguish the individual on-click
        // action of a given item
        remoteViews.setOnClickFillInIntent(R.id.card_view_alarms, fillInIntent)

        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}