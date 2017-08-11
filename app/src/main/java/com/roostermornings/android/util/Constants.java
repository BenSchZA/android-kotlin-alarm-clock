/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.util;

public class Constants {

    //Adapter view types
    public static final int VIEW_TYPE_UNKNOWN = 99;
    public static final int VIEW_TYPE_ADD = 1;
    public static final int VIEW_TYPE_INVITE = 2;
    public static final int VIEW_TYPE_HEADER = 0;

    //Font resources
    public final static String APP_FONT = "fonts/Nunito/Nunito-Bold.ttf";

    //Broadcast receiver filters
    public final static String FINISH_AUDIO_RECORD_ACTIVITY = "rooster.android.broadcast.filter.FINISH_AUDIO_RECORD_ACTIVITY";

    //Intent actions
    public final static String ACTION_CANCEL_SNOOZE = "rooster.android.intent.action.CANCEL_SNOOZE";
    public final static String ACTION_SHARE_ROOSTER_CHOSEN = "rooster.android.intent.action.ACTION_SHARE_ROOSTER_CHOSEN";

    //AudioTable
    public final static int AUDIO_TYPE_SOCIAL = 0;
    public final static int AUDIO_TYPE_CHANNEL = 1;

    public final static int AUDIO_TYPE_FAVOURITE_FALSE = 0;
    public final static int AUDIO_TYPE_FAVOURITE_TRUE = 1;

    //Request codes
    public final static int REQUEST_CODE_SHARE_ROOSTER = 1010;

    //User settings
    //These are used in application_user_settings, but ensure strings are correct
    public final static String USER_SETTINGS_DAY_NIGHT_THEME  = "pref_key_day_night_theme";
    public final static String USER_SETTINGS_VIBRATE = "pref_key_alarm_vibrate";
    public final static String USER_SETTINGS_DEFAULT_TONE = "pref_key_alarm_default_tone";
    public final static String USER_SETTINGS_DOWNLOAD_ON_DATA = "pref_key_download_on_data";
    public final static String USER_SETTINGS_SNOOZE_TIME = "pref_key_alarm_snooze_time";
    public final static String USER_SETTINGS_ROOSTER_ORDER = "pref_key_rooster_order";
    public final static String USER_SETTINGS_ALARM_VOLUME = "pref_key_failsafe_alarm_volume";
    public final static String USER_SETTINGS_TIME_FORMAT = "pref_key_24_hour_time";
    public final static String USER_SETTINGS_ALARM_VOLUME_INCREMENT_DURATION = "pref_key_alarm_increment_duration";
    public final static String ABOUT_APP_VERSION = "pref_key_static_field_version";

    public final static long[] VIBRATE_PATTERN = {0, 1000, 500, 1000, 500};

    //Permission request codes
    public final static int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    public final static int MY_PERMISSIONS_REQUEST_CHANGE_PROFILE_PIC = 2;
    public final static int MY_PERMISSIONS_REQUEST_AUDIO_RECORD = 3;

    //FriendsFragmentActivity
    public final static String FRIENDS_ACTIVITY_CURRENT_FRAGMENT = "currentFriendsFragment";

    public final static String ARG_SHOW_DISMISS = "ARG_SHOW_DISMISS";

    //DeviceAlarmController intent actions
    public final static String ACTTION_ALARMRECEIVER = "receiver.ALARM_RECEIVER";
    // > display alarm icon
    public final static String ACTION_ALARMCHANGED = "android.intent.action.ALARM_CHANGED";
    //DeviceAlarmController intent extras
    public final static String EXTRA_REQUESTCODE = "requestCode";
    // > alarm set flag to remove alarm icon later
    public final static String EXTRA_ALARMSET = "alarmSet";

    //DeviceAlarm extras
    public static final String EXTRA_RECURRING = "extra_recurring";
    public static final String EXTRA_VIBRATE = "extra_vibrate";
    public static final String EXTRA_TONE = "extra_tone";
    public static final String EXTRA_UID = "extra_uid";
    public static final String EXTRA_SNOOZE_ACTIVATION = "extra_snooze_activation";

    //DeviceAlarmTable constants
    public static final String ALARM_CHANNEL_DOWNLOAD_FAILED = "failed";

    //DeviceAlarmReceiver wakeful intent
    public static final String DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT = "DeviceAlarmReceiver.WAKEFUL_INTENT";

    //Background task service intent actions
    public static final String ACTION_DAILYTASK = "service.action.DAILY_TASK";

    //AudioService intent actions
    public final static String ACTION_ALARMDISPLAY = "rooster.update.ALARMDISPLAY";
    public final static String ACTION_ALARMTIMESUP = "rooster.update.ALARMTIMESUP";
    public final static String ACTION_CHANNEL_DOWNLOAD_FINISHED = "rooster.notify.CHANNEL_DOWNLOAD_FINISHED";
    public final static String ACTION_END_AUDIO_SERVICE = "EndAudioServiceBroadcastReceiver";
    public final static String ACTION_SNOOZE_ACTIVATION = "ActionSnoozeActivation";

    //AudioService timers
    public final static int ALARM_DEFAULTTIME = 5 * 60 * 1000;
    public final static int AUDIOSERVICE_DOWNLOAD_TASK_LIMIT = 30 * 1000;

    //NewAudio activities
    public final static String EXTRA_LOCAL_FILE_STRING = "localFileString";

    //AudioService
    public final static String EXTRA_FRIENDS_LIST = "friendsList";

    //Notification IDs
    public final static int AUDIOSERVICE_NOTIFICATION_ID = 1000;
    public final static int UPLOADSERVICE_NOTIFICATION_ID = 2000;

    //FirebaseListenerService intent actions
    public final static String ACTION_REQUESTNOTIFICATION = "rooster.update.REQUEST_NOTIFICATION";
    public final static String ACTION_ROOSTERNOTIFICATION = "rooster.update.ROOSTER_NOTIFICATION";

    public final static String EXTRA_SOCIAL_ROOSTERS = "socialRoosterCount";

    //BaseApplication flags
    public final static String FLAG_FRIENDREQUESTS = "friendRequests";
    public final static String FLAG_ROOSTERCOUNT = "roosterCount";

    //Intent extras
    public final static String EXTRA_ALARMID = "alarmId";

    //Shared prefs
    public final static String SHARED_PREFS_KEY = "SHARED_PREFS_KEY";
    //Dialog boolean to indicate viewed
    public final static String PERMISSIONS_DIALOG_OPTIMIZATION = "permissionsDialogOptimization";
    public final static String MOBILE_NUMBER_VALIDATED = "mobileNumberValid";
    public final static String MOBILE_NUMBER_ENTRY_DISMISSED = "mobileNumberEntryDismissed";
    public final static String MOBILE_NUMBER_ENTRY = "mobileNumberEntry";

    //Calendar constants
    public final static long TIME_MILLIS_1_MINUTE = 60000;
    public final static long TIME_MILLIS_1_HOUR = 3600000;
    public final static long TIME_MILLIS_1_DAY = 86400000;
    public final static long TIME__MILLIS_1_WEEK = 604800000;

    //MessageStatus Activity
    public final static int MESSAGE_STATUS_SENT = 1;
    public final static int MESSAGE_STATUS_DELIVERED = 2;
    public final static int MESSAGE_STATUS_RECEIVED = 3;

    public final static String MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE = "MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE";
    public final static String MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_TODAY = "MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_TODAY";
    public final static String MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_FAVOURITE = "MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_FAVOURITE";

    //Storage
    public final static String STORAGE_USER_PROFILE_PICTURE = "users/profile_pictures/";

    //Filenames
    public final static String FILENAME_PREFIX_ROOSTER_TEMP_RECORDING = "RoosterRecording";
    public final static String FILENAME_PREFIX_ROOSTER_CONTENT = "audio";
    public final static String FILENAME_PREFIX_ROOSTER_EXAMPLE_CONTENT = "example_clip";

    public final static long MAX_ROOSTER_FILE_SIZE = 8 * 1024 * 1024;

    //DownloadSyncService config
    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "com.roostermornings.android.datasync.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "roostermornings.com";
    // The account name
    public static final String ACCOUNT = "Rooster Content";

    public static final String FORCE_UPDATE_DESCRIPTION = "forceUpdateDescription";
    public static final String FORCE_UPDATE_TITLE = "forceUpdateTitle";

}
