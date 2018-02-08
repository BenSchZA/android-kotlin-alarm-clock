/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter

import android.app.Activity
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView

import com.afollestad.materialdialogs.MaterialDialog
import com.roostermornings.android.R
import com.roostermornings.android.activity.DiscoverFragmentActivity
import com.roostermornings.android.firebase.FA
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

import java.util.ArrayList

import butterknife.BindView
import butterknife.ButterKnife

class DiscoverListAdapter(mDataset: MutableList<MediaBrowserCompat.MediaItem>, private val mActivity: Activity) : RecyclerView.Adapter<DiscoverListAdapter.ViewHolder>(), Filterable {

    private var mDataset: MutableList<MediaBrowserCompat.MediaItem>? = mDataset
    private var context: Context? = null

    private var mCurrentMediaId: String? = null
    private var mPlaybackState: PlaybackStateCompat? = null

    private val playingMediaId: String?
        get() {
            val isPlaying = mPlaybackState?.state == PlaybackStateCompat.STATE_PLAYING
            return if (isPlaying) mCurrentMediaId else null
        }

    private var discoverAudioSampleInterface: DiscoverAudioSampleInterface? = null

    private fun isPlaying(mediaItem: MediaBrowserCompat.MediaItem): Boolean {
        val mediaId = mediaItem.mediaId
        return mediaId != null && mediaId == playingMediaId
    }

    fun setCurrentMediaMetadata(mediaMetadata: MediaMetadataCompat?) {
        mCurrentMediaId = mediaMetadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    }

    fun setPlaybackState(playbackState: PlaybackStateCompat?) {
        mPlaybackState = playbackState
    }

    interface DiscoverAudioSampleInterface {
        fun onMediaItemSelected(item: MediaBrowserCompat.MediaItem, isPlaying: Boolean)
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one activityContentView per item, and
    // you provide access to all the views for a data item in a activityContentView holder
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @BindView(R.id.cardview_channel_name)
        lateinit var txtChannelName: TextView
        @BindView(R.id.card_view_channel_image)
        lateinit var imgChannelImage: ImageView
        @BindView(R.id.card_view_discover)
        lateinit var cardViewChannel: CardView
        @BindView(R.id.imageProgressBar)
        lateinit var imageProgressBar: ProgressBar
        @BindView(R.id.audioProgressBar)
        lateinit var audioProgressBar: ProgressBar
        @BindView(R.id.audio_listen)
        lateinit var listenImageButton: ImageButton
        @BindView(R.id.cardview_channel_info)
        lateinit var channelInfo: TextView

        init {
            ButterKnife.bind(this, view)
        }
    }

    fun add(position: Int, item: MediaBrowserCompat.MediaItem) {
        mDataset?.add(position, item)
        notifyItemInserted(position)
    }

    fun refreshAll(myDataset: MutableList<MediaBrowserCompat.MediaItem>) {
        mDataset = myDataset
        notifyDataSetChanged()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): DiscoverListAdapter.ViewHolder {
        context = parent.context
        // create a new activityContentView
        val v = LayoutInflater.from(parent.context).inflate(R.layout.cardview_discover, parent, false)
        // set the activityContentView's size, margins, paddings and layout parameters
        return ViewHolder(v)
    }

    // Replace the contents of a activityContentView (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        val mediaItem = mDataset?.get(position) ?: return

        // - replace the contents of the activityContentView with that element
        if (isPlaying(mediaItem)) {
            holder.listenImageButton.setBackgroundResource(R.drawable.rooster_new_audio_pause_button)
        } else {
            holder.listenImageButton.setBackgroundResource(R.drawable.rooster_audio_play_button)
        }

        if(mCurrentMediaId == mediaItem.mediaId) {
            when (mPlaybackState?.state) {
                PlaybackStateCompat.STATE_CONNECTING -> {
                    holder.audioProgressBar.visibility = View.VISIBLE
                }
                PlaybackStateCompat.STATE_BUFFERING -> {
                    holder.audioProgressBar.visibility = View.VISIBLE
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    holder.audioProgressBar.visibility = View.GONE
                }
                else -> {
                    holder.audioProgressBar.visibility = View.GONE
                }
            }
        } else {
            holder.audioProgressBar.visibility = View.GONE
        }

        holder.cardViewChannel.setOnClickListener {
            discoverAudioSampleInterface?.onMediaItemSelected(mediaItem, isPlaying(mediaItem))
        }

        holder.listenImageButton.setOnClickListener {
            discoverAudioSampleInterface?.onMediaItemSelected(mediaItem, isPlaying(mediaItem))
        }

        holder.txtChannelName.text = mediaItem.description.title

        holder.channelInfo.setOnClickListener {
            MaterialDialog.Builder(mActivity)
                    .title(mediaItem.description.title!!)
                    .content(mediaItem.description.description!!)
                    .positiveText(R.string.ok)
                    .negativeText("")
                    .show()

            FA.Log(FA.Event.channel_info_viewed::class.java, FA.Event.channel_info_viewed.Param.channel_title, mediaItem.mediaId)
        }

        try {
            Picasso.with(context).load(mediaItem.description.iconUri)
                    .fit()
                    .centerCrop()
                    .into(holder.imgChannelImage, object : Callback {
                        override fun onSuccess() {
                            holder.imageProgressBar.visibility = View.GONE
                            holder.imgChannelImage.visibility = View.VISIBLE
                        }

                        override fun onError() {}
                    })
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mDataset?.size ?: -1
    }

    override fun getFilter(): Filter {

        return object : Filter() {

            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                mDataset = results.values as ArrayList<MediaBrowserCompat.MediaItem>
                notifyDataSetChanged()
            }

            override fun performFiltering(constraint: CharSequence): FilterResults {
                val results = FilterResults()
                val filteredContacts = ArrayList<MediaBrowserCompat.MediaItem>()

                // Perform your search here using the search constraint string
                val mConstraint = constraint.toString().toLowerCase()
                filteredContacts.addAll(
                        mDataset?.filter {
                            it.description.title?.toString()?.toLowerCase()?.contains(mConstraint) == true
                        } as ArrayList<MediaBrowserCompat.MediaItem>)

                results.count = filteredContacts.size
                results.values = filteredContacts

                return results
            }
        }
    }

    init {
        if (mActivity is DiscoverFragmentActivity) {
            discoverAudioSampleInterface = mActivity
        }
    }
}