package com.roostermornings.android.domain.local

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.roostermornings.android.BuildConfig
import java.util.*

/**
 * Created by bscholtz on 2017/12/06.
 */
@IgnoreExtraProperties
class MetricsEvent(
        var details: String? = null,
        var timestamp: Long? = null) {

    companion object {
        sealed class Event {
            object NONE: Event()
            class ALARM_FAILURE(val type: Type): Event() {
                enum class Type {
                    NONE,
                    STREAM,
                    DEFAULT,
                    NO_FIRE,
                    DELAYED,
                    NOT_HEARD,
                    NOT_SEEN
                }
            }
        }
    }

    var event: String? = null
    var type: String? = null

    val version = BuildConfig.VERSION_NAME

    fun setEventAndType(value: Event): MetricsEvent {
        when(value) {
            Event.NONE -> {}
            is Event.ALARM_FAILURE -> {
                event = value.javaClass.simpleName.toLowerCase()
                type = value.type.name.toLowerCase()
            }
        }
        initializeEvent(value)
        return this@MetricsEvent
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