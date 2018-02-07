/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter_data

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.*
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.domain.database.Channel
import com.roostermornings.android.domain.database.ChannelRooster
import com.roostermornings.android.domain.local.GeoHashChannel
import com.roostermornings.android.geolocation.GeoHashUtils
import com.roostermornings.android.util.JSONPersistence
import com.roostermornings.android.util.Toaster
import java.util.*
import javax.inject.Inject

/**
 * com.roostermornings.android.channels
 * Rooster Mornings Android
 *
 * Created by bscholtz on 28/08/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */

class ChannelManager(private val context: Context) {
    private val TAG = ChannelManager::class.java.simpleName

    private var mChannelsReference: DatabaseReference = FirebaseDatabase.getInstance().reference
            .child("channels")
    private var mChannelRoostersReference: DatabaseReference = FirebaseDatabase.getInstance().reference
            .child("channel_rooster_uploads")

    private val tempChannelRoosters = ArrayList<ChannelRooster>()
    private val channelRoosterMap = TreeMap<Int, MutableList<ChannelRooster>>(Collections.reverseOrder<Any>())
    private var channelIterationMap = HashMap<Channel, Int>()

    @Inject
    lateinit var jsonPersistence: JSONPersistence
    @Inject
    lateinit var geoHashUtils: GeoHashUtils

    init {
        BaseApplication.roosterApplicationComponent.inject(this)
        mChannelsReference.keepSynced(true)
        mChannelRoostersReference.keepSynced(true)
    }

    fun refreshChannelData(persistedChannelRoosters: ArrayList<ChannelRooster>) {

        //Clear old data before syncing
        //Must be called outside thread, or use mThis context to access
        tempChannelRoosters.clear()
        channelRoosterMap.clear()
        channelIterationMap.clear()

        val channelsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    postSnapshot.getValue(Channel::class.java)?.let {
                        channel -> if(channel.isActive) {
                        if (channel.isNew_alarms_start_at_first_iteration) {
                            var iteration = jsonPersistence.getStoryIteration(channel.getUid())
                            if (iteration <= 0) iteration = 1
                            channelIterationMap.put(channel, iteration)
                        } else {
                            var iteration = channel.getCurrent_rooster_cycle_iteration()
                            if (iteration <= 0) iteration = 1
                            channelIterationMap.put(channel, iteration)
                        }
                    }
                    }
                }

                GeoHashUtils.onFlagGeoHashChannelsDataListener = (
                        object : GeoHashUtils.Companion.OnFlagGeoHashChannelsDataListener {
                            override fun onDataChange(geoHashChannels: List<GeoHashChannel>) {

                                //Filter all geolocated channels
                                channelIterationMap = GeoHashUtils.filterGeoLocatedChannels(channelIterationMap, geoHashChannels)

                                //Attach listeners all at once, so that last listener indicates data sync complete
                                for ((key, value) in channelIterationMap) {
                                    getChannelRoosterData(key, value)
                                }

                                //https://stackoverflow.com/questions/34530566/find-out-if-child-event-listener-on-firebase-completely-load-all-data
                                //Value events are always triggered last and are guaranteed to contain updates from any other events which occurred before that snapshot was taken.
                                mChannelRoostersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {

                                        val dataFresh =
                                                persistedChannelRoosters.isNotEmpty() &&
                                                        persistedChannelRoosters.all {
                                            persisted -> tempChannelRoosters.any {
                                            it.audio_file_url == persisted.audio_file_url }
                                        }
                                        //When finished, add temp data to adapter array
                                        if (!dataFresh) {
                                            onFlagChannelManagerDataListener?.onChannelRoosterDataChanged(tempChannelRoosters)
                                        }
                                        onFlagChannelManagerDataListener?.onSyncFinished()
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        onFlagChannelManagerDataListener?.onSyncFinished()
                                    }
                                })

                            }

                            override fun onCancelled() {
                                onFlagChannelManagerDataListener?.onSyncFinished()
                            }

                            override fun onPostExecute(success: Boolean) {

                            }
                        })

                geoHashUtils.getGeoHashChannelsWithinInfluence()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                Toaster.makeToast(context, "Failed to load channel.", Toast.LENGTH_SHORT).checkTastyToast()
                onFlagChannelManagerDataListener?.onSyncFinished()
            }
        }
        mChannelsReference.addListenerForSingleValueEvent(channelsListener)
    }

    private fun getChannelRoosterData(channel: Channel, iteration: Int) {
        val channelRoosterUploadsReference = mChannelRoostersReference.child(channel.getUid())
        //Ensure latest data is pulled
        channelRoosterUploadsReference.keepSynced(true)

        val channelRoosterUploadsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val channelRoosterIterationMap = TreeMap<Int, ChannelRooster>()
                //Check if node has children i.e. channelId content exists
                if (dataSnapshot.childrenCount == 0L) return
                //Iterate over all content children
                for (postSnapshot in dataSnapshot.children) {
                    postSnapshot.getValue(ChannelRooster::class.java)?.let { channelRooster ->
                        //Set channelrooster display picture to banner image & description to channel description
                        //In future channelrooster specific details will be shown in discover page
                        channelRooster.channel_photo = channel.getPhoto()
                        channelRooster.channel_description = channel.getDescription()

                        //Only place active channel content into map
                        if (channelRooster.isActive) {
                            channelRoosterIterationMap.put(channelRooster.getRooster_cycle_iteration(), channelRooster)
                        }
                    }
                }
                if(channelRoosterIterationMap.isNotEmpty()) {
                    findNextValidChannelRooster(channelRoosterIterationMap, channel, iteration)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        channelRoosterUploadsReference.addListenerForSingleValueEvent(channelRoosterUploadsListener)
    }

    fun findNextValidChannelRooster(channelRoosterIterationMap: TreeMap<Int, ChannelRooster>, channel: Channel, iteration: Int): ChannelRooster? {

        var validChannelRooster: ChannelRooster? = null

        //Check head and tail of naturally sorted TreeMap for next valid channel content
        val tailMap = channelRoosterIterationMap.tailMap(iteration)
        val headMap = channelRoosterIterationMap.headMap(iteration)

        // If the channel rooster iteration map contains the current iteration key, proceed, else find nextValidEntry
        // If date locked, use head/tail logic to ensure we don't select current iteration
        if(channelRoosterIterationMap.containsKey(iteration) and !isChannelStoryDateLocked(channel.uid)) {
            validChannelRooster = processValidChannelRooster(channel, channelRoosterIterationMap, iteration)
        } else {
            // If story, and not date locked:
            //        head               tail
            //  00000000000000[b]  x  [a]00000000
            // where x is current invalid iteration
            // try a first, then b
            if (channel.isNew_alarms_start_at_first_iteration and !isChannelStoryDateLocked(channel.uid)) {
                if (tailMap.isNotEmpty()) {
                    val nextValidEntry = tailMap.firstKey()
                    validChannelRooster = processValidChannelRooster(channel, channelRoosterIterationMap, nextValidEntry)
                } else if (headMap.isNotEmpty()) {
                    val nextValidEntry = headMap.lastKey()
                    validChannelRooster = processValidChannelRooster(channel, channelRoosterIterationMap, nextValidEntry)
                }
            }
            // If daily, or date locked:
            //        head               tail
            //  00000000000000[a]  x  [b]00000000
            // where x is current invalid iteration
            // try a first, then b
            else {
                if (headMap.isNotEmpty()) {
                    val nextValidEntry = headMap.lastKey()
                    validChannelRooster = processValidChannelRooster(channel, channelRoosterIterationMap, nextValidEntry)
                } else if (tailMap.isNotEmpty()) {
                    val nextValidEntry = tailMap.firstKey()
                    validChannelRooster = processValidChannelRooster(channel, channelRoosterIterationMap, nextValidEntry)
                }
            }
        }

        //For each channel rooster fetched, refresh the display list
        refreshTempChannelRoosters()
        return validChannelRooster
    }

    private fun processValidChannelRooster(channel: Channel, channelRoosterIterationMap: TreeMap<Int, ChannelRooster>, nextValidEntry: Int): ChannelRooster? {
        //Retrieve channel audio
        channelRoosterIterationMap[nextValidEntry]?.let { channelRooster ->
            // User is starting at next valid entry
            // Set entry for iteration to current valid story iteration, to be incremented on play
            if(!isChannelStoryDateLocked(channel.uid))
                jsonPersistence.setStoryIteration(channel.getUid(), nextValidEntry)

            channelRooster.isSelected = false
            // This method allows multiple objects per key
            // Try get priority mutable list, add to it, if unsuccessful (i.e. first entry) then create list
            if(channelRoosterMap[channel.getPriority()]?.add(channelRooster) != true) {
                val values = ArrayList<ChannelRooster>()
                values.add(channelRooster)
                channelRoosterMap.put(channel.getPriority(), values)
            }
            return channelRooster
        }
        return null
    }

    fun incrementChannelStoryIteration(channelRoosterUID: String) {
        //Attempt to increment channel story iteration, if not day locked
        val currentTime = Calendar.getInstance()

        if(!isChannelStoryDateLocked(channelRoosterUID)) {
            // Story iteration not locked
            jsonPersistence.setStoryIteration(channelRoosterUID, jsonPersistence.getStoryIteration(channelRoosterUID) + 1)
            jsonPersistence.setDateLock(channelRoosterUID, currentTime.timeInMillis)
        }
    }

    fun isChannelStoryDateLocked(channelRoosterUID: String): Boolean {
        //Attempt to increment channel story iteration, if not day locked
        val dateLockTimeInMillis = jsonPersistence.getDateLock(channelRoosterUID)
        val dateLockTime = Calendar.getInstance()
        dateLockTime.timeInMillis = dateLockTimeInMillis
        val currentTime = Calendar.getInstance()

        if(dateLockTime.get(Calendar.DATE) == currentTime.get(Calendar.DATE)) {
            // Story iteration locked
            return true
        }
        return false
    }

    private fun refreshTempChannelRoosters() {
        // TreeMap ensures unique and allows sorting by priority! How cool is that?
        tempChannelRoosters.clear()
        // This method allows us to have multiple objects per priority key
        val values = ArrayList<ChannelRooster>()
        for (channelRoosterList in channelRoosterMap.values) {
            values.addAll(channelRoosterList)
        }
        if (!values.isEmpty()) tempChannelRoosters.addAll(values)
    }

    companion object {
        // Listener for channel manager data sync
        var onFlagChannelManagerDataListener: OnFlagChannelManagerDataListener? = null

        interface OnFlagChannelManagerDataListener {
            fun onChannelRoosterDataChanged(freshChannelRoosters: ArrayList<ChannelRooster>)
            fun onSyncFinished()
        }
    }
}