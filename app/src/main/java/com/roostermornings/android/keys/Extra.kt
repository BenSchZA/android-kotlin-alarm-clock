package com.roostermornings.android.keys

/**
 * Created by bscholtz on 2018/02/02.
 */
enum class Extra {
    SOCIAL_ROOSTERS,

    // DeviceAlarmController intent extras
    REQUEST_CODE,
    MILLIS_SLOT,
    // > alarm set flag to remove alarm icon later
    ALARM_SET,

    // DeviceAlarm extras
    RECURRING,
    ALARM_SET_ID,
    SNOOZE_ACTIVATION,

    // NewAudio activities
    LOCAL_FILE_STRING,
    FRIENDS_LIST,

    // DeviceAlarmReceiver wakeful intent
    WAKEFUL_INTENT
}