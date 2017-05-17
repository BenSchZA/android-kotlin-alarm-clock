/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.receiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.activity.DeviceAlarmFullScreenActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.service.AudioService;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.util.Constants;

import javax.inject.Inject;
import javax.inject.Named;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.facebook.FacebookSdk.getApplicationContext;

public class DeviceAlarmReceiver extends WakefulBroadcastReceiver {

    @Inject DeviceAlarmController alarmController;
    @Inject DeviceAlarmTableManager alarmTableManager;
    @Inject @Named("default") SharedPreferences sharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        BaseApplication baseApplication = (BaseApplication) getApplicationContext();
        baseApplication.getRoosterApplicationComponent().inject(this);

        //Check if vibrator enabled in user settings
        setVibrate(context);

        if(intent.getBooleanExtra(Constants.EXTR_SNOOZE_ACTIVATION, false)) {
            //Activate snooze alarm
            Intent broadcastIntent = new Intent(Constants.ACTION_SNOOZE_ACTIVATION);
            broadcastIntent.putExtra(Constants.EXTRA_ALARMID, intent.getStringExtra(Constants.EXTRA_UID));
            broadcastIntent.putExtra(Constants.DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT, new Intent(context, DeviceAlarmReceiver.class));
            context.sendBroadcast(broadcastIntent);
            return;
        }

        //Reschedule alarm intents for recurring weekly
        rescheduleAlarmIntents(intent);

        //Start audio service with alarm UID
        Intent audioServiceIntent = new Intent(context, AudioService.class);
        audioServiceIntent.putExtra(Constants.EXTRA_ALARMID, intent.getStringExtra(Constants.EXTRA_UID));
        //Include intent to enable finishing wakeful intent later in AudioService
        audioServiceIntent.putExtra(Constants.DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT, new Intent(context, DeviceAlarmReceiver.class));
        context.startService(audioServiceIntent);
    }

    private void rescheduleAlarmIntents(Intent intent) {
        //if this is a recurring alarm, set another pending intent for next week same time
        if (intent.getBooleanExtra(Constants.EXTRA_RECURRING, false)) {
            //make record that this alarm has been changed, refresh as necessary
            alarmTableManager.setAlarmChanged(intent.getIntExtra(Constants.EXTRA_REQUESTCODE, 0));
            alarmController.refreshAlarms(alarmTableManager.selectChanged());

        } else {
            //Set alarm to disabled if fired and not recurring - once all alarms in set are disabled, then toggle enabled in GUI
            alarmTableManager.setAlarmEnabled(intent.getIntExtra(Constants.EXTRA_REQUESTCODE, 0), false);
        }
    }

    private void setVibrate(Context context) {
        if (sharedPreferences.getBoolean(Constants.USER_SETTINGS_VIBRATE, false)) {
            Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
            long[] vibratePattern = Constants.VIBRATE_PATTERN;
            int vibrateRepeat = 2;
            vibrator.vibrate(vibratePattern, vibrateRepeat);
        } else {
            //If vibrating then cancel
            Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
            if (vibrator.hasVibrator()) {
                vibrator.cancel();
            }
        }
    }
}
