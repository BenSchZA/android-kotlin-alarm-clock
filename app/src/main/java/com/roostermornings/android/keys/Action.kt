package com.roostermornings.android.keys

/**
 * Created by bscholtz on 2018/02/02.
 */
enum class Action {
    // FirebaseListenerService intent actions
    REQUEST_NOTIFICATION,
    ROOSTER_NOTIFICATION,

    CANCEL_SNOOZE,
    FROM_ROOSTER_SEND,
    FINISH_AUDIO_RECORD_ACTIVITY,

    // DeviceAlarmController intent actions
    ALARM_RECEIVER,
    ALARM_CHANGED,

    // Background task service intent actions
    DAILY_BACKGROUND_TASK,

    // AudioService intent actions
    ALARM_DISPLAY,
    ALARM_TIMESUP,
    CHANNEL_DOWNLOAD_FINISHED,
    END_AUDIO_SERVICE,
    SNOOZE_ACTIVATION
}