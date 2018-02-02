package com.roostermornings.android.keys

/**
 * Created by bscholtz on 2018/02/02.
 */
enum class PrefsKey {
    SHARED_PREFS_KEY,
    // Use Keys._.name to access shared pref

    // Activity state
    IS_ACTIVITY_FOREGROUND,
    APP_BROUGHT_FOREGROUND,

    USER_VIEWED_FAQS,
    MOBILE_NUMBER_VALIDATED,
    MOBILE_NUMBER_ENTRY_DISMISSED,
    MOBILE_NUMBER_ENTRY,
    PERMISSIONS_DIALOG_OPTIMIZATION,
    USER_FINISHED_ONBOARDING,
    USER_GEOHASH,
    ANONYMOUS_USER_UID
}