/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter_data

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.domain.Alarm
import com.roostermornings.android.firebase.FirebaseNetwork
import com.roostermornings.android.sqlutil.DeviceAlarmController
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager
import com.roostermornings.android.util.StrUtils
import com.roostermornings.android.util.Toaster
import javax.inject.Inject
import com.google.firebase.auth.FirebaseUser
import com.roostermornings.android.activity.MyAlarmsFragmentActivity
import java.util.*

/**
 * com.roostermornings.android.`adapter-data`
 * Rooster Mornings Android
 *
 * Created by bscholtz on 12/10/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */

class RoosterAlarmManager(val context: Context) {

    private val TAG = RoosterAlarmManager::class.java.simpleName

    private val mTempAlarms = ArrayList<Alarm>()

    @Inject lateinit var deviceAlarmController: DeviceAlarmController
    @Inject lateinit var deviceAlarmTableManager: DeviceAlarmTableManager
    var firebaseUser: FirebaseUser? = null

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    @Inject
    fun RoosterAlarmManager(firebaseUser: FirebaseUser?) {
        this.firebaseUser = firebaseUser
    }

    fun fetchAlarms(persistedAlarms: ArrayList<Alarm>) {

        //Clear old content
        mTempAlarms.clear()

        val mMyAlarmsReference = FirebaseDatabase.getInstance().reference
                .child("alarms").child(firebaseUser!!.uid)
        //Keep local and Firebase alarm dbs synced
        mMyAlarmsReference.keepSynced(true)

        val alarmsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                for (postSnapshot in dataSnapshot.children) {
                    val alarm = postSnapshot.getValue(Alarm::class.java) ?: Alarm()

                    //Register alarm sets on login
                    //Extract data from Alarm "alarm" and create new alarm set DeviceAlarm
                    val alarmChannel = alarm.getChannel()
                    val alarmChannelUID = alarmChannel.getId() ?: ""

                    //Check for a valid Firebase entry, if invalid delete entry, else continue
                    if (alarm.getHour() < 0 || alarm.getMinute() < 0 || alarm.days.isEmpty() || !StrUtils.notNullOrEmpty(alarm.getUid())) {
                        FirebaseNetwork.removeFirebaseAlarm(postSnapshot.key)
                    } else {
                        var successfulProcessing = false
                        //If alarm from firebase does not exist locally, create it
                        if (StrUtils.notNullOrEmpty(alarm.getUid()) && (!deviceAlarmTableManager.isSetInDB(alarm.getUid()))) {
                            //Try to insert alarm into SQL db - if successful, configure new alarm set element and set successfulProcessing flag = true
                            if (deviceAlarmController.registerAlarmSet(alarm.isEnabled, alarm.getUid(), alarm.getHour(), alarm.getMinute(),
                                    alarm.days, alarm.isRecurring, alarmChannelUID, alarm.isAllow_friend_audio_files)) {
                                configureAlarmElement(alarm)
                                successfulProcessing = true
                            }
                        } else if (StrUtils.notNullOrEmpty(alarm.getUid()) && deviceAlarmTableManager.isSetInDB(alarm.getUid())!!) {
                            configureAlarmElement(alarm)
                            successfulProcessing = true
                        }//If alarm exists locally, AND in Firebase, just configure alarm UI element

                        //If alarm exists in Firebase and couldn't be created locally, or is corrupt, delete Firebase entry
                        if (!successfulProcessing) {
                            FirebaseNetwork.removeFirebaseAlarm(postSnapshot.key)
                        }
                    }
                }

                if(persistedAlarms.sortedWith(compareBy({it.getUid()})) != mTempAlarms.sortedWith(compareBy({it.getUid()})))
                    onFlagAlarmManagerDataListener?.onAlarmDataChanged(mTempAlarms)
                onFlagAlarmManagerDataListener?.onSyncFinished()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                Toaster.makeToast(context, "Failed to load alarms.",
                        Toast.LENGTH_SHORT).checkTastyToast()
            }
        }
        mMyAlarmsReference.addListenerForSingleValueEvent(alarmsListener)
    }

    private fun configureAlarmElement(alarm: Alarm) {
        //Check SQL db to see if all alarms in set have fired
        alarm.isEnabled = deviceAlarmTableManager.isSetEnabled(alarm.getUid())
        //Set set enabled flag and don't notify user
        deviceAlarmController.setSetEnabled(alarm.getUid(), alarm.isEnabled, false)

        //Set alarm element millis to allow sorting
        val alarmSetPendingMillis = deviceAlarmTableManager.getMillisOfNextPendingAlarmInSet(alarm.getUid())
        if (alarmSetPendingMillis != null) {
            alarm.millis = alarmSetPendingMillis
        }

        //Add alarm to adapter display arraylist and notify adapter of change
        mTempAlarms.add(alarm)
    }

    companion object {
        // Listener for alarm manager data sync
        var onFlagAlarmManagerDataListener: OnFlagAlarmManagerDataListener? = null

        interface OnFlagAlarmManagerDataListener {
            fun onAlarmDataChanged(freshAlarms: ArrayList<Alarm>)
            fun onSyncFinished()
        }
    }
}