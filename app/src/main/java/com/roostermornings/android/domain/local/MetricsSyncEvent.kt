package com.roostermornings.android.domain.local

import com.roostermornings.android.BuildConfig
import java.util.*

/**
 * Created by bscholtz on 2018/03/09.
 */
class MetricsSyncEvent(
        var details: String? = null,
        var timestamp: Long? = null,
        var channel_uid: String? = null,
        var audio_file_url: String? = null,
        var audio_file_uid: String? = null) {

    companion object {
        sealed class Event {
            object NONE: Event()
            class ALARM(val type: Type): Event() {
                enum class Type {
                    NONE,
                    STARTED,
                    ACTIVATION,
                    STREAM,
                    DEFAULT
                }
            }
            class SYNC(val type: Type): Event() {
                enum class Type {
                    NONE,
                    STARTED,
                    FRESH,
                    NOT_FRESH,
                    REFRESHED,
                    FAILURE
                }
            }
        }
    }

    var event: String? = null
    var type: String? = null

    val version = BuildConfig.VERSION_NAME

    fun setEventAndType(value: Event): MetricsSyncEvent {
        when(value) {
            Event.NONE -> {}
            is Event.ALARM -> {
                event = value.javaClass.simpleName.toLowerCase()
                type = value.type.name.toLowerCase()
            }
            is Event.SYNC -> {
                event = value.javaClass.simpleName.toLowerCase()
                type = value.type.name.toLowerCase()
            }
        }
        initializeEvent(value)
        return this@MetricsSyncEvent
    }

    private fun initializeEvent(value: Event) {
        if(timestamp == null)
            timestamp = Calendar.getInstance().timeInMillis
        // Initialize event object based on type, if null
        when(value) {
            Event.NONE -> {
                return
            }
            else -> {}
        }
    }
}