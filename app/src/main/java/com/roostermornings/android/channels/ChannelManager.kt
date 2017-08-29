/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.channels

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.*
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.activity.DiscoverFragmentActivity
import com.roostermornings.android.domain.Channel
import com.roostermornings.android.domain.ChannelRooster
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment2
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

class ChannelManager(private val context: Context, private val from: Any) {
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
        BaseApplication.getRoosterApplicationComponent().inject(this)
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
                            override fun onDataChange(geoHashChannels: List<GeoHashUtils.GeoHashChannel>) {

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

                                        //When finished, add temp data to adapter array
                                        if (persistedChannelRoosters != tempChannelRoosters) {
                                            onFlagChannelManagerDataListener?.onChannelRoosterDataChanged(tempChannelRoosters)
                                        }
                                        onFlagChannelManagerDataListener?.onSyncFinished()
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {

                                    }
                                })

                            }

                            override fun onCancelled() {

                            }

                            override fun onPostExecute(success: Boolean) {

                            }
                        })

                geoHashUtils.getGeoHashChannelsWithinInfluence()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                Toaster.makeToast(context, "Failed to load channel.", Toast.LENGTH_SHORT).checkTastyToast()
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

                        //Only place non-current iterations into the map, to ensure we don't play current iteration in discover
                        if (channelRooster.isActive && channelRooster.getRooster_cycle_iteration() != iteration) {
                            channelRoosterIterationMap.put(channelRooster.getRooster_cycle_iteration(), channelRooster)
                        }
                    }
                }
                channelRoosterIterationMap.isNotEmpty().run {
                    findNextValidChannelRooster(channelRoosterIterationMap, channel, iteration)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        channelRoosterUploadsReference.addListenerForSingleValueEvent(channelRoosterUploadsListener)
    }

    private fun findNextValidChannelRooster(channelRoosterIterationMap: TreeMap<Int, ChannelRooster>, channel: Channel, iteration: Int) {

        //Check head and tail of naturally sorted TreeMap for next valid channel content
        val tailMap = channelRoosterIterationMap.tailMap(iteration)
        val headMap = channelRoosterIterationMap.headMap(iteration)

        if(from is DiscoverFragmentActivity) {

            //        head               tail
            //  00000000000000[0]  x   00000000
            // We want to select [] where x is the current iteration, !NOT! included in the channelRoosterIterationMap - this ensures best content for discover
            // Or   x   0000000[0]  in the case of a story that is unstarted or a channel on first iteration
            if (headMap.isNotEmpty()) {
                //Retrieve channel audio
                channelRoosterIterationMap[headMap.lastKey()]?.let { channelRooster ->
                    channelRooster.setSelected(false)
                    //This method allows multiple objects per key
                    if(channelRoosterMap[channel.getPriority()]?.add(channelRooster) != true) {
                        val values = ArrayList<ChannelRooster>()
                        values.add(channelRooster)
                        channelRoosterMap.put(channel.getPriority(), values)
                    }
                }
            } else if (tailMap.isNotEmpty()) {
                //Retrieve channel audio
                channelRoosterIterationMap[tailMap.lastKey()]?.let { channelRooster ->
                    channelRooster.setSelected(false)
                    //This method allows multiple objects per key
                    if(channelRoosterMap[channel.getPriority()]?.add(channelRooster) != true) {
                        val values = ArrayList<ChannelRooster>()
                        values.add(channelRooster)
                        channelRoosterMap.put(channel.getPriority(), values)
                    }
                }
            }
        } else if(from is NewAlarmFragment2) {

            if (tailMap.isNotEmpty()) {
                //User is starting story at next valid entry
                //Set entry for iteration to current valid story iteration, to be incremented on play
                jsonPersistence.setStoryIteration(channel.getUid(), tailMap.firstKey())
                //Retrieve channel audio
                channelRoosterIterationMap[tailMap.firstKey()]?.let { channelRooster ->
                    channelRooster.setSelected(false)
                    //This method allows multiple objects per key
                    if(channelRoosterMap[channel.getPriority()]?.add(channelRooster) != true) {
                        val values = ArrayList<ChannelRooster>()
                        values.add(channelRooster)
                        channelRoosterMap.put(channel.getPriority(), values)
                    }
                }
            } else if (headMap.isNotEmpty()) {
                //User is starting story at next valid entry
                //Set entry for iteration to current valid story iteration, to be incremented on play
                jsonPersistence.setStoryIteration(channel.getUid(), headMap.firstKey())
                //Retrieve channel audio
                channelRoosterIterationMap[headMap.firstKey()]?.let { channelRooster ->
                    channelRooster.setSelected(false)
                    //This method allows multiple objects per key
                    if(channelRoosterMap[channel.getPriority()]?.add(channelRooster) != true) {
                        val values = ArrayList<ChannelRooster>()
                        values.add(channelRooster)
                        channelRoosterMap.put(channel.getPriority(), values)
                    }
                }
            }
        }

        //For each channel rooster fetched, refresh the display list
        refreshTempChannelRoosters()
    }

    private fun refreshTempChannelRoosters() {
        //TreeMap ensures unique and allows sorting by priority! How cool is that?
        tempChannelRoosters.clear()
        //This method allows us to have multiple objects per priority key
        val values = ArrayList<ChannelRooster>()
        for (channelRoosterList in channelRoosterMap.values) {
            values.addAll(channelRoosterList)
        }
        if (!values.isEmpty()) tempChannelRoosters.addAll(values)
    }

    companion object {
        //Listener channel manager data sync
        var onFlagChannelManagerDataListener: OnFlagChannelManagerDataListener? = null

        interface OnFlagChannelManagerDataListener {
            fun onChannelRoosterDataChanged(freshChannelRoosters: ArrayList<ChannelRooster>)
            fun onSyncFinished()
        }
    }
}