/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.firebase

import android.content.Context
import android.net.Uri

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.roostermornings.android.domain.OnboardingJourneyEvent
import com.roostermornings.android.domain.User
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.MyContactsController
import com.roostermornings.android.util.StrUtils

import java.util.Calendar
import java.util.HashMap
import java.util.TimeZone

object FirebaseNetwork {

    private var onFlagValidMobileNumberCompleteListener: OnFlagValidMobileNumberCompleteListener? = null
    private var onFlagChannelNameReceivedListener: OnFlagChannelNameReceivedListener? = null

    private fun isUserSignedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
                && FirebaseAuth.getInstance().currentUser?.isAnonymous == false
    }

    fun getRoosterUser(uid: String?, operation: (User?) -> Unit) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                operation(user)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                operation(null)
            }
        }

        if(FirebaseAuth.getInstance().currentUser != null
                && FirebaseAuth.getInstance().currentUser?.isAnonymous == false
                && !uid.isNullOrBlank()) {
            val ref = fDB.child("users/$uid")
            ref.keepSynced(true)
            ref.addListenerForSingleValueEvent(userListener)
        } else operation(null)
    }

    fun updateLastSeen() {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser
        val calendar = Calendar.getInstance()

        val childUpdates = HashMap<String, Any>()
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("users/${fUser.uid}/last_seen", calendar.timeInMillis)
            fDB.updateChildren(childUpdates)
        }
    }

    fun updateProfileUserName(userName: String) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val childUpdates = HashMap<String, Any>()

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("users/${fUser.uid}/user_name", userName)
            fDB.updateChildren(childUpdates)
        }
    }

    fun updateProfileCellNumber(context: Context, cellNumberString: String) {
        val myContactsController = MyContactsController(context)
        val nsnNumber: String
        nsnNumber = if (StrUtils.notNullOrEmpty(cellNumberString)) myContactsController.processUserContactNumber(cellNumberString) else ""

        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val childUpdates = HashMap<String, Any>()

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("users/${fUser.uid}/cell_number", nsnNumber)
            fDB.updateChildren(childUpdates)

            if (!nsnNumber.isBlank()) {
                val editor = context.getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit()
                editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, true)
                editor.apply()
            } else {
                val editor = context.getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit()
                editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, false)
                editor.apply()
            }
        }
    }

    fun updateProfileGeoHashLocation(geohash: String) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val childUpdates = HashMap<String, Any>()

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("users/${fUser.uid}/geohash_location", geohash)
            fDB.updateChildren(childUpdates)
        }
    }

    interface OnFlagValidMobileNumberCompleteListener {
        fun onEvent(valid: Boolean)
    }

    fun setOnFlagValidMobileNumberCompleteListener(listener: OnFlagValidMobileNumberCompleteListener) {
        onFlagValidMobileNumberCompleteListener = listener
    }

    fun flagValidMobileNumber(context: Context, notify: Boolean) {
        if(!isUserSignedIn()) {
            onFlagValidMobileNumberCompleteListener?.onEvent(false)
            return
        }

        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        if (fUser?.uid?.isNotBlank() == true) {
            val mMobileNumberReference = fDB
                    .child("users").child(fUser.uid).child("cell_number")

            mMobileNumberReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userCellNumber = dataSnapshot.getValue(String::class.java)

                    val valid: Boolean
                    val editor = context.getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit()

                    if (userCellNumber.isNullOrBlank()) {
                        editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, false)
                        editor.apply()
                        valid = false
                    } else {
                        editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, true)
                        editor.apply()
                        valid = true

                        FA.Log(FA.Event.onboarding_number_provided::class.java, null, null)
                    }

                    if (notify) onFlagValidMobileNumberCompleteListener?.onEvent(valid)
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })
        }
    }

    fun updateProfileProfilePic(url: Uri) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val childUpdates = HashMap<String, Any>()

        if (fUser?.uid?.isNotBlank() == true) {
            childUpdates.put("users/${fUser.uid}/profile_pic", url.toString())
            fDB.updateChildren(childUpdates)
        }
    }

    fun updateFirebaseAlarmEnabled(setId: String, enabled: Boolean) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        val childUpdates = HashMap<String, Any>()

        if (fUser?.uid?.isNotBlank() == true && setId.isNotBlank()) {
            childUpdates.put("alarms/${fUser.uid}/$setId/enabled", enabled)
            fDB.updateChildren(childUpdates)
        }
    }

    fun removeFirebaseAlarm(setId: String) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        if (fUser?.uid?.isNotBlank() == true && setId.isNotBlank()) {
            fDB.child("alarms/${fUser.uid}/$setId").removeValue()
        }
    }

    //Ensure used on social roosters
    fun setListened(senderId: String, queueId: String) {
        val fDB = FirebaseDatabase.getInstance().reference

        val childUpdates = HashMap<String, Any>()

        if (senderId.isNotBlank() && queueId.isNotBlank()) {
            childUpdates.put("social_rooster_uploads/$senderId/$queueId/listened", true)
            fDB.updateChildren(childUpdates)
        }
    }

    interface OnFlagChannelNameReceivedListener {
        fun onChannelNameReceived(channelName: String?)
    }

    fun setOnFlagChannelNameReceivedListener(listener: OnFlagChannelNameReceivedListener) {
        onFlagChannelNameReceivedListener = listener
    }

    fun getChannelNameFromUID(UID: String) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        if (fUser?.uid?.isNotBlank() == true) {
            val mChannelNameReference = fDB.child("channels/$UID/name")

            mChannelNameReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val channelName = dataSnapshot.getValue(String::class.java)

                    onFlagChannelNameReceivedListener?.onChannelNameReceived(channelName)
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })
        }
    }

    fun createOrUpdateRoosterUser(deviceToken: String?, photoURL: String?) {
        val fDB = FirebaseDatabase.getInstance().reference
        val fUser = FirebaseAuth.getInstance().currentUser

        if (fUser?.uid?.isNotBlank() == true) {
            val user = User(null,
                    "android",
                    deviceToken,
                    photoURL,
                    fUser.displayName?:"Anonymous Rooster (Me)",
                    "",
                    fUser.uid,
                    null,
                    0,
                    null)

            //Note: "friends" and "cell_number" node not changed TODO: should profile pic be kept?
            val childUpdates = HashMap<String, Any>()
            childUpdates.put("users/${fUser.uid}/device_token", user.device_token)
            childUpdates.put("users/${fUser.uid}/device_type", user.device_type)
            childUpdates.put("users/${fUser.uid}/unseen_roosters", user.unseen_roosters)
            childUpdates.put("users/${fUser.uid}/profile_pic", user.profile_pic)
            childUpdates.put("users/${fUser.uid}/uid", user.uid)
            childUpdates.put("users/${fUser.uid}/user_name", user.user_name)

            //Add user as a friend of theirs
            childUpdates.put("users/${fUser.uid}/friends/${fUser.uid}", true)

            fDB.updateChildren(childUpdates)
        }
    }
}
