package com.roostermornings.android.keys

/**
 * Created by bscholtz on 2018/02/02.
 */
enum class NotificationID {
    SPACE_FILLER, // Notification can't have ID of 0: https://stackoverflow.com/questions/8725909/startforeground-does-not-show-my-notification
    AUDIO_SERVICE,
    UPLOAD_SERVICE,
    FIREBASE_MESSAGING
}