package com.roostermornings.android.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.roostermornings.android.domain.OnboardingJourneyEvent
import java.util.*

/**
 * Created by bscholtz on 2017/12/14.
 */
object UserMetrics {
    private fun isUserSignedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
                && FirebaseAuth.getInstance().currentUser?.isAnonymous == false
    }

    fun migrateOnboardingJourney(anonymousUID: String?, signInUID: String?) {
        val fDB = FirebaseDatabase.getInstance().reference

        val childUpdates = HashMap<String, Any>()

        if (!anonymousUID.isNullOrBlank() && !signInUID.isNullOrBlank()) {
            childUpdates.put("user_metrics/$anonymousUID/convert_uid", signInUID!!)
            fDB.updateChildren(childUpdates)
        }
    }

    fun migrateUserUID(anonymousUID: String?, signInUID: String?) {
        val fDB = FirebaseDatabase.getInstance().reference

        val childUpdates = HashMap<String, Any>()

        if (!anonymousUID.isNullOrBlank() && !signInUID.isNullOrBlank()) {
            childUpdates.put("user_metrics/$anonymousUID/migrate_uid", signInUID!!)
            fDB.updateChildren(childUpdates)
        }
    }

    fun logOnboardingEvent(event: OnboardingJourneyEvent) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val childUpdates = HashMap<String, Any>()

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("user_metrics/${fUser.uid}/onboarding_journey/${event.timestamp}", event)
            fDB.updateChildren(childUpdates)
        }
    }

    fun updateLastSeen() {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser
        val calendar = Calendar.getInstance()

        val childUpdates = HashMap<String, Any>()
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("user_metrics/${fUser.uid}/last_seen", calendar.timeInMillis)
            fDB.updateChildren(childUpdates)
        }
    }

    fun setAuthMethod() {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val childUpdates = HashMap<String, Any>()

        var providerId = "anonymous"
        fUser?.providerData?.forEach { userInfo ->
            userInfo.providerId.takeIf { it.contains("google") || it.contains("facebook") || it.contains("password") }?.let { providerId = userInfo.providerId }
        }

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("user_metrics/${fUser.uid}/auth", providerId)
            fDB.updateChildren(childUpdates)
        }
    }

    fun generateNewUserMetricsEntry() {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser
        val calendar = Calendar.getInstance()

        val childUpdates = HashMap<String, Any>()
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        if (fUser?.uid?.isNotBlank() == true) {
            val signUpDateListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(!dataSnapshot.exists()) {
                        childUpdates.put("user_metrics/${fUser.uid}/signup_date", calendar.timeInMillis)
                        childUpdates.put("user_metrics/${fUser.uid}/uid", fUser.uid)
                        setAuthMethod()
                        fDB.updateChildren(childUpdates)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {}
            }

            val ref = fDB.child("user_metrics/${fUser.uid}/signup_date")
            ref.keepSynced(true)
            ref.addListenerForSingleValueEvent(signUpDateListener)
        }
    }
}