/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

/**
 * Constants Class
 *
 * Store any and all public final static constants for configuration, intent ACTIONs,
 * shared pref keys, settings keys, etc.
 *
 * @author bscholtz
 * @version 1
 * @since 22/03/17
 */

public class Constants {
    // Font resources
    public final static String APP_FONT = "fonts/Nunito/Nunito-Bold.ttf";

    // AudioTable
    public final static int AUDIO_TYPE_SOCIAL = 0;
    public final static int AUDIO_TYPE_CHANNEL = 1;

    public final static int AUDIO_TYPE_FAVOURITE_FALSE = 0;
    public final static int AUDIO_TYPE_FAVOURITE_TRUE = 1;

    // User settings
    // These are used in application_user_settings, but ensure strings are correct
    public final static String USER_SETTINGS_DAY_NIGHT_THEME  = "pref_key_day_night_theme";
    public final static String USER_SETTINGS_VIBRATE = "pref_key_alarm_vibrate";
    public final static String USER_SETTINGS_DEFAULT_TONE = "pref_key_alarm_default_tone";
    public final static String USER_SETTINGS_DOWNLOAD_ON_DATA = "pref_key_download_on_data";
    public final static String USER_SETTINGS_SNOOZE_TIME = "pref_key_alarm_snooze_time";
    public final static String USER_SETTINGS_ROOSTER_ORDER = "pref_key_rooster_order";
    public final static String USER_SETTINGS_ALARM_VOLUME = "pref_key_failsafe_alarm_volume";
    public final static String USER_SETTINGS_TIME_FORMAT = "pref_key_24_hour_time";
    public final static String USER_SETTINGS_ALARM_VOLUME_INCREMENT_DURATION = "pref_key_alarm_increment_duration";
    public final static String USER_SETTINGS_LIMIT_ALARM_TIME = "pref_key_limit_alarm_time";

    public final static long[] VIBRATE_PATTERN = {0, 1000, 500, 1000, 500};

    public final static String ARG_SHOW_DISMISS = "ARG_SHOW_DISMISS";

    // DeviceAlarmTable constants
    public static final String ALARM_CHANNEL_DOWNLOAD_FAILED = "failed";

    // AudioService timers
    public final static int ALARM_DEFAULTTIME = 5 * 60 * 1000;
    public final static int AUDIOSERVICE_DOWNLOAD_TASK_LIMIT = 30 * 1000;

    // Calendar constants
    public final static long TIME_MILLIS_1_MINUTE = 60000;
    public final static long TIME_MILLIS_1_HOUR = 3600000;
    public final static long TIME_MILLIS_1_DAY = 86400000;
    public final static long TIME__MILLIS_1_WEEK = 604800000;

    // MessageStatus Activity
    public final static int MESSAGE_STATUS_SENT = 1;
    public final static int MESSAGE_STATUS_DELIVERED = 2;
    public final static int MESSAGE_STATUS_RECEIVED = 3;

    // Storage
    public final static String STORAGE_USER_PROFILE_PICTURE = "users/profile_pictures/";

    // Filenames
    public final static String FILENAME_PREFIX_ROOSTER_TEMP_RECORDING = "RoosterRecording";
    public final static String FILENAME_PREFIX_ROOSTER_CONTENT = "audio";

    public final static long MAX_ROOSTER_FILE_SIZE = 8 * 1024 * 1024;
    public final static long ABSOLUTE_MAX_FILE_SIZE = 15 * 1024 * 1024;
    public final static String APP_CUMULATIVE_RX_BYTES = "APP_CUMULATIVE_RX_BYTES";
    public final static String APP_CUMULATIVE_TX_BYTES = "APP_CUMULATIVE_TX_BYTES";

    // DownloadSyncService config
    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "com.roostermornings.android.datasync.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "roostermornings.com";
    // The account name
    public static final String ACCOUNT = "Rooster Content";

    public static final String FORCE_UPDATE_DESCRIPTION = "forceUpdateDescription";
    public static final String FORCE_UPDATE_TITLE = "forceUpdateTitle";

}
