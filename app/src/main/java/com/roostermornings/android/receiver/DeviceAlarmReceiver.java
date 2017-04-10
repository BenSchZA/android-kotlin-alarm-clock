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

import com.roostermornings.android.activity.DeviceAlarmFullScreenActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.util.Constants;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.facebook.FacebookSdk.getApplicationContext;

public class DeviceAlarmReceiver extends WakefulBroadcastReceiver {

    private Context context;
    private DeviceAlarmController alarmController;
    private DeviceAlarmTableManager alarmTableManager;
    private SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        //instantiate new alarm controller
        //alarm controller provides interface for synchronising pending intents with SQLLite database on device
        alarmController = new DeviceAlarmController(context);

        //instantiate new alarm table manager
        //this interfaces directly with database
        alarmTableManager = new DeviceAlarmTableManager(context);

        Intent intentAlarmFullscreen = new Intent(context, DeviceAlarmFullScreenActivity.class);
        intentAlarmFullscreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        //Append alarm UID to intent for alarm activation
        intentAlarmFullscreen.putExtra(Constants.EXTRA_UID, intent.getStringExtra(Constants.EXTRA_UID));

        //if this is a recurring alarm, set another pending intent for next week same time
        if (intent.getBooleanExtra(Constants.EXTRA_RECURRING, false)) {
            //make record that this alarm has been changed, refresh as necessary
            alarmTableManager.setAlarmChanged(intent.getIntExtra(Constants.EXTRA_REQUESTCODE, 0));
            alarmController.refreshAlarms(alarmTableManager.selectChanged());

        } else {
            //Set alarm to disabled if fired and not recurring - once all alarms in set are disabled, then toggle enabled in GUI
            alarmTableManager.setAlarmEnabled(intent.getIntExtra(Constants.EXTRA_REQUESTCODE, 0), false);
        }


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

        if (intent.getBooleanExtra(Constants.EXTRA_TONE, false)) {
            intentAlarmFullscreen.putExtra(Constants.EXTRA_TONE, true);
        }

        //Include intent to allow completeWakefulIntent from activity
        intentAlarmFullscreen.putExtra(Constants.DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT, new Intent(context, DeviceAlarmReceiver.class));

        context.startActivity(intentAlarmFullscreen);
    }
}
