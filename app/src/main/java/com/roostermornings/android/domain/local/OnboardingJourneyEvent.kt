package com.roostermornings.android.domain.local

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*

/**
 * Created by bscholtz on 2017/12/06.
 */
@IgnoreExtraProperties
class OnboardingJourneyEvent(
        var subject: String? = null,
        var content_uid: String? = null,
        var length: Int? = null,
        var target_time: Int? = null,
        var target: String? = null,
        var category: String? = null,
        var granted: Boolean? = null,
        var send_requests: Int? = null,
        var count: Int? = null) {

    companion object {
        enum class Event {
            NONE, VIEW, SCROLL, CLICK_CONTENT, LISTEN, SEEK_TRACK, CLICK_ON, PERMISSION, SEND_REQUESTS
        }
    }

    var event: String? = null
    @Exclude
    private var type: Event = Event.NONE

    val timestamp = Calendar.getInstance().timeInMillis

    fun setType(value: Event): OnboardingJourneyEvent {
        event = value.name.toLowerCase()
        initializeEvent(value)
        return this@OnboardingJourneyEvent
    }

    private fun initializeEvent(type: Event) {
        // Initialize event object based on type, if null
        when(type) {
            Event.VIEW -> {
                subject = subject?:""
            }
            Event.SCROLL -> {
                subject = subject?:""
            }
            Event.CLICK_CONTENT -> {
                content_uid = content_uid?:""
            }
            Event.LISTEN -> {
                length = length?:-1
                content_uid = content_uid?:""
            }
            Event.SEEK_TRACK -> {
                target_time = target_time?:-1
                content_uid = content_uid?:""
            }
            Event.CLICK_ON -> {
                target = target?:""
            }
            Event.PERMISSION -> {
                category = category?:""
            }
            Event.SEND_REQUESTS -> {
                count = count?:-1
            }
            else -> {}
        }
    }
}