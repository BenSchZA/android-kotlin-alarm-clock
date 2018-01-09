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
import com.roostermornings.android.realm.RealmAlarmFailureLog
import java.util.*

class DeviceAlarmReceiver : WakefulBroadcastReceiver() {

    @Inject lateinit var alarmController: DeviceAlarmController
    @Inject lateinit var alarmTableManager: DeviceAlarmTableManager
    @Inject
    @Named("default") lateinit var sharedPreferences: SharedPreferences
    @Inject lateinit var realmAlarmFailureLog: RealmAlarmFailureLog

    private var alarmUid = ""
    private var requestCode = -1
    private var millisSlot = -1L
    private var isRecurring = false

    override fun onReceive(context: Context, intent: Intent) {

        BaseApplication.getRoosterApplicationComponent().inject(this)

        // Get extras passed in from DeviceAlarmController class
        val isSnoozeActivation = intent.getBooleanExtra(Constants.EXTRA_SNOOZE_ACTIVATION, false)
        isRecurring = intent.getBooleanExtra(Constants.EXTRA_RECURRING, false)
        alarmUid = intent.getStringExtra(Constants.EXTRA_UID)
        requestCode = intent.getIntExtra(Constants.EXTRA_REQUESTCODE, -1)
        millisSlot = intent.getLongExtra(Constants.EXTRA_MILLIS_SLOT, -1L)

        //Check if vibrator enabled in user settings
        setVibrate(context)

        if (isSnoozeActivation) {
            //Activate snooze alarm
            val broadcastIntent = Intent(Constants.ACTION_SNOOZE_ACTIVATION)
            broadcastIntent.putExtra(Constants.EXTRA_ALARMID, alarmUid)
            broadcastIntent.putExtra(Constants.DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT, Intent(context, DeviceAlarmReceiver::class.java))
            broadcastIntent.putExtra(Constants.EXTRA_REQUESTCODE, requestCode)
            context.sendBroadcast(broadcastIntent)
            return
        }

        //Start audio service with alarm UID
        val audioServiceIntent = Intent(context, AudioService::class.java)
        audioServiceIntent.putExtra(Constants.EXTRA_ALARMID, alarmUid)
        //Include intent to enable finishing wakeful intent later in AudioService
        audioServiceIntent.putExtra(Constants.DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT, Intent(context, DeviceAlarmReceiver::class.java))
        audioServiceIntent.putExtra(Constants.EXTRA_REQUESTCODE, requestCode)
        // Put millis slot as extra, to refer to when accessing Realm log
        audioServiceIntent.putExtra(Constants.EXTRA_MILLIS_SLOT, millisSlot)

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
        if (sharedPreferences.getBoolean(Constants.USER_SETTINGS_VIBRATE, false)) {
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
