/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.TextView

import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.adapter.DiscoverListAdapter
import com.roostermornings.android.firebase.FA
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.media.MediaNotificationHelper
import com.roostermornings.android.service.MediaService
import com.roostermornings.android.util.JSONPersistence
import com.roostermornings.android.util.RoosterUtils

import java.util.ArrayList

import javax.inject.Inject

import butterknife.BindView

class DiscoverFragmentActivity : BaseActivity(), DiscoverListAdapter.DiscoverAudioSampleInterface, MediaController.MediaPlayerControl {

    private val mThis = this

    @BindView(R.id.discoverListView)
    lateinit var mRecyclerView: RecyclerView
    @BindView(R.id.toolbar_title)
    lateinit var toolbarTitle: TextView
    @BindView(R.id.button_bar)
    lateinit var buttonBarLayout: LinearLayout
    @BindView(R.id.swiperefresh)
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    //Set up adapter for monitoring channelrooster media item objects
    private val mediaItems = ArrayList<MediaBrowserCompat.MediaItem>()
    private var mAdapter = DiscoverListAdapter(mediaItems, this@DiscoverFragmentActivity)

    private var mMediaController: MediaControllerCompat? = null
    private val mMediaBrowser by lazy { MediaBrowserCompat(this, ComponentName(this, MediaService::class.java), ConnectionCallback(), null) }

    private val mMediaControllerWidget: MediaController? = null

    @Inject lateinit var AppContext: BaseApplication
    @Inject lateinit var jsonPersistence: JSONPersistence
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            mediaItems.clear()
            mediaItems.addAll(children)
            mAdapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        }

        override fun onError(parentId: String) {
            super.onError(parentId)
            swipeRefreshLayout.isRefreshing = false
        }

        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>, options: Bundle) {
            super.onChildrenLoaded(parentId, children, options)
            mediaItems.clear()
            mediaItems.addAll(children)
            mAdapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        }

        override fun onError(parentId: String, options: Bundle) {
            super.onError(parentId, options)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    inner class ConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mMediaBrowser.subscribe(mMediaBrowser.root, subscriptionCallback)
            try {
                // Create a MediaControllerCompat
                mMediaController = MediaControllerCompat(mThis, mMediaBrowser.sessionToken)
                mMediaController?.registerCallback(mediaControllerCallback)
                // Save the controller
                MediaControllerCompat.setMediaController(this@DiscoverFragmentActivity, mMediaController)

                //                mMediaControllerWidget.setAnchorView(findViewById(R.id.anchorText));
                //                mMediaControllerWidget.setMediaPlayer();
                //                mMediaControllerWidget.setEnabled(true);
                //mMediaControllerWidget.show(0);
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            val mediaController = MediaControllerCompat
                    .getMediaController(mThis)
            if (mediaController != null) {
                mediaController.unregisterCallback(mediaControllerCallback)
                MediaControllerCompat.setMediaController(mThis, null)
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
        }
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            mAdapter.setPlaybackState(state)
            mAdapter.notifyDataSetChanged()
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            if (metadata != null) {
                mAdapter.setCurrentMediaMetadata(metadata)
            }
        }
    }

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(R.layout.activity_discover)

        inject(BaseApplication.getRoosterApplicationComponent())

        //Notify user of no internet connection
        checkInternetConnection()

        //Final context to be used in threads
        val context = this

        //UI setup thread
        object : Thread() {
            override fun run() {
                //Setup day/night theme selection (based on settings, and time)
                setDayNightTheme()
                //Set highlighting of button bar
                setButtonBarSelection()
                //Set toolbar title
                val toolbar = setupToolbar(toolbarTitle, getString(R.string.discover))
                toolbar?.setNavigationIcon(R.drawable.md_nav_back)
                toolbar?.setNavigationOnClickListener { startHomeActivity() }

                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                mRecyclerView.setHasFixedSize(true)
                //Use a linear layout manager
                val mLayoutManager = LinearLayoutManager(context)
                mRecyclerView.layoutManager = mLayoutManager
                mRecyclerView.adapter = mAdapter

                //Check if uses explore user prop has been set, and if not then create shared pref with default no
                val sharedPrefUsesExplore = sharedPreferences.getString(
                        FA.UserProp.uses_explore.shared_pref_uses_explore, "null")
                if (!sharedPrefUsesExplore.contains(FA.UserProp.uses_explore.no)
                        && !sharedPrefUsesExplore.contains(FA.UserProp.uses_explore.started_explore_playback)
                        && !sharedPrefUsesExplore.contains(FA.UserProp.uses_explore.completed_explore_playback)) {
                    sharedPreferences.edit().putString(FA.UserProp.uses_explore.shared_pref_uses_explore,
                            FA.UserProp.uses_explore.no).apply()
                    FA.SetUserProp(FA.UserProp.uses_explore::class.java, FA.UserProp.uses_explore.no)
                }
            }
        }.run()

        if (!jsonPersistence.mediaItems.isEmpty()) {
            mediaItems.clear()
            mediaItems.addAll(jsonPersistence.mediaItems)
            mAdapter.notifyDataSetChanged()
            swipeRefreshLayout.isRefreshing = false
        } else if (checkInternetConnection()) {
            if (!swipeRefreshLayout.isRefreshing) swipeRefreshLayout.isRefreshing = true
        }

        /*
        * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
        * performs a swipe-to-refresh gesture.
        */
        swipeRefreshLayout.setOnRefreshListener {
            // This method performs the actual data-refresh operation.
            // The method calls setRefreshing(false) when it's finished.
            //Reload adapter data and set message status, set listener for new data
        }

        //Set volume rocker to alarm stream
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStart() {
        super.onStart()
        //Display notifications
        updateRoosterNotification()
        updateRequestNotification()

        if (!mMediaBrowser.isConnected)
            mMediaBrowser.connect()
    }

    public override fun onPause() {
        super.onPause()

        //Persist channel roosters for seamless loading
        if (!mediaItems.isEmpty()) jsonPersistence.mediaItems = ArrayList<MediaBrowserCompat.MediaItem>(mediaItems)

        //Unsubscribe and unregister MediaControllerCompat callbacks
        MediaControllerCompat.getMediaController(this@DiscoverFragmentActivity)?.unregisterCallback(mediaControllerCallback)
        if (mMediaBrowser.isConnected) {
            mMediaBrowser.unsubscribe(mMediaBrowser.root, subscriptionCallback)
            mMediaBrowser.disconnect()
        }
    }

    override fun onNewIntent(intent: Intent) {
        handleIntent(intent)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (RoosterUtils.hasLollipop()) {
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                mMediaController?.dispatchMediaButtonEvent(event)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            //use the query to search your data somehow
            handleSearch(query)
        }
    }

    private fun handleSearch(query: String) {
        mAdapter.refreshAll(mediaItems)
        mAdapter.filter?.filter(query)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_explore, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.search_explore).actionView as SearchView
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(componentName))

        //When searchView is closed, refresh data
        searchView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {

            override fun onViewDetachedFromWindow(arg0: View) {
                // search was detached/closed
                mAdapter.refreshAll(mediaItems)
            }

            override fun onViewAttachedToWindow(arg0: View) {
                // search was opened
            }
        })

        return true
    }

    override fun onMediaItemSelected(item: MediaBrowserCompat.MediaItem, isPlaying: Boolean) {
        val controller = MediaControllerCompat.getMediaController(this)
        val controls = controller.transportControls
        if (isPlaying) {
            controls.pause()
        } else {
            controls.pause()
            mediaItems.indexOfFirst { it.mediaId == item.mediaId }.takeIf { it > -1 }?.let {
                controls.skipToQueueItem(it.toLong())
            }
        }
    }

    override fun start() {

    }

    override fun pause() {

    }

    override fun getDuration(): Int {
        return 0
    }

    override fun getCurrentPosition(): Int {
        return 0
    }

    override fun seekTo(i: Int) {

    }

    override fun isPlaying(): Boolean {
        return false
    }

    override fun getBufferPercentage(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        return false
    }

    override fun canSeekBackward(): Boolean {
        return false
    }

    override fun canSeekForward(): Boolean {
        return false
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    companion object {

        val TAG = DiscoverFragmentActivity::class.java.simpleName
    }
}
