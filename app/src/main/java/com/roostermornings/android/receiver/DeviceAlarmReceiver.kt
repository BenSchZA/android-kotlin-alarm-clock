/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.receiver

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Vibrator
import android.support.v4.content.WakefulBroadcastReceiver

import com.roostermornings.android.BaseApplication
import com.roostermornings.android.service.AudioService
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager
import com.roostermornings.android.sqlutil.DeviceAlarmController
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.RoosterUtils

import javax.inject.Inject
import javax.inject.Named

import android.content.Context.VIBRATOR_SERVICE
import com.facebook.FacebookSdk.getApplicationContext
import com.roostermornings.android.keys.Action
import com.roostermornings.android.keys.Extra
import com.roostermornings.android.realm.RealmAlarmFailureLog
import java.util.*

class DeviceAlarmReceiver : WakefulBroadcastReceiver() {

    @Inject lateinit var alarmController: DeviceAlarmController
    @Inject lateinit var alarmTableManager: DeviceAlarmTableManager
    @Inject
    @field:Named("default") lateinit var defaultSharedPrefs: SharedPreferences
    @Inject lateinit var realmAlarmFailureLog: RealmAlarmFailureLog

    private var alarmUid = ""
    private var requestCode = -1
    private var millisSlot = -1L
    private var isRecurring = false

    override fun onReceive(context: Context, intent: Intent) {

        BaseApplication.roosterApplicationComponent.inject(this)

        // Get extras passed in from DeviceAlarmController class
        val isSnoozeActivation = intent.getBooleanExtra(Extra.SNOOZE_ACTIVATION.name, false)
        isRecurring = intent.getBooleanExtra(Extra.RECURRING.name, false)
        alarmUid = intent.getStringExtra(Extra.ALARM_SET_ID.name)
        requestCode = intent.getIntExtra(Extra.REQUEST_CODE.name, -1)
        millisSlot = intent.getLongExtra(Extra.MILLIS_SLOT.name, -1L)

        //Check if vibrator enabled in user settings
        setVibrate(context)

        if (isSnoozeActivation) {
            //Activate snooze alarm
            val broadcastIntent = Intent(Action.SNOOZE_ACTIVATION.name)
            broadcastIntent.putExtra(Extra.ALARM_SET_ID.name, alarmUid)
            broadcastIntent.putExtra(Extra.WAKEFUL_INTENT.name, Intent(context, DeviceAlarmReceiver::class.java))
            broadcastIntent.putExtra(Extra.REQUEST_CODE.name, requestCode)
            context.sendBroadcast(broadcastIntent)
            return
        }

        //Start audio service with alarm UID
        val audioServiceIntent = Intent(context, AudioService::class.java)
        audioServiceIntent.putExtra(Extra.ALARM_SET_ID.name, alarmUid)
        //Include intent to enable finishing wakeful intent later in AudioService
        audioServiceIntent.putExtra(Extra.WAKEFUL_INTENT.name, Intent(context, DeviceAlarmReceiver::class.java))
        audioServiceIntent.putExtra(Extra.REQUEST_CODE.name, requestCode)
        // Put millis slot as extra, to refer to when accessing Realm log
        audioServiceIntent.putExtra(Extra.MILLIS_SLOT.name, millisSlot)

        if (RoosterUtils.hasO()) {
            context.startForegroundService(audioServiceIntent)
        } else {
            context.startService(audioServiceIntent)
        }

        realmAlarmFailureLog.getAlarmFailureLogMillisSlot(millisSlot) {
            it.fired = true
            it.firedTime = Calendar.getInstance().timeInMillis
        }
        // Close Realm object
        realmAlarmFailureLog.closeRealm()

        /** Reschedule alarm intents for recurring weekly,
         * ensure this is done after Realm log processed. */
        rescheduleAlarmIntents(intent)
    }

    private fun rescheduleAlarmIntents(intent: Intent) {
        //if this is a recurring alarm, set another pending intent for next week same time
        if (isRecurring) {
            //make record that this alarm has been changed, refresh as necessary
            alarmTableManager.setAlarmChanged(requestCode.toLong())
            alarmController.refreshAlarms(alarmTableManager.selectChanged())
        } else {
            //Set alarm to disabled if fired and not recurring - once all alarms in set are disabled, then toggle enabled in GUI
            alarmTableManager.setAlarmEnabled(requestCode.toLong(), false)
        }
    }

    private fun setVibrate(context: Context) {
        val vibrateEnabled = defaultSharedPrefs.getBoolean(Constants.USER_SETTINGS_VIBRATE, false)
        if (vibrateEnabled) {
            val vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator
            val vibratePattern = Constants.VIBRATE_PATTERN
            val vibrateRepeat = 2
            vibrator.vibrate(vibratePattern, vibrateRepeat)
        } else {
            //If vibrating then cancel
            val vibrator = getApplicationContext().getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                vibrator.cancel()
            }
        }
    }
}
