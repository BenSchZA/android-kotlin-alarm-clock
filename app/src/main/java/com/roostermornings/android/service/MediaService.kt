/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.util.Pair
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.activity.DiscoverFragmentActivity
import com.roostermornings.android.activity.MyAlarmsFragmentActivity
import com.roostermornings.android.channels.ChannelManager
import com.roostermornings.android.domain.ChannelRooster
import com.roostermornings.android.media.MediaNotificationHelper
import com.roostermornings.android.util.RoosterUtils
import java.util.concurrent.TimeUnit


class MediaService : MediaBrowserServiceCompat(),
        AudioManager.OnAudioFocusChangeListener{

    companion object {
        val PLAY_FROM_MEDIA_ID_COMMAND = "PLAY_FROM_MEDIA_ID_COMMAND"
        val MEDIA_ID_EXTRA = "MEDIA_ID_EXTRA"
    }

    val mThis: Context by lazy { this }

    var mFocusRequest : AudioFocusRequest? = null

    val mSession: MediaSessionCompat by lazy { MediaSessionCompat(this, "MediaService") }
    val mSessionConnector: MediaSessionConnector by lazy { MediaSessionConnector(mSession, PlaybackController()) }
    val mTransportControls: MediaControllerCompat.TransportControls by lazy { mSession.controller.transportControls }

    val mPlaybackState: PlaybackStateCompat
        get() = mSession.controller.playbackState
    var mPlayerPreparing = false

    val mCurrentMediaDescription: MediaDescriptionCompat
        get() = mSession.controller.metadata.description

    val mTrackSelector: TrackSelector = DefaultTrackSelector()
    val mPlayer by lazy{ ExoPlayerFactory.newSimpleInstance(this, mTrackSelector) }

    var lastVolume : Float = 0f

    val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val channelManager: ChannelManager by lazy { ChannelManager(this, this) }
    var channelRoosters: ArrayList<ChannelRooster> = ArrayList()

    var mediaSources: ArrayList<MediaSource> = ArrayList()

    val mWifiLock by lazy { (this.getSystemService(Context.WIFI_SERVICE) as WifiManager).createWifiLock(WifiManager.WIFI_MODE_FULL, "RadiophonyLock")  }

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)

        //Repeat all in queue
        mPlayer.repeatMode = REPEAT_MODE_ALL
        mSession.setRepeatMode(REPEAT_MODE_ALL)
        mSession.isActive = true

        mSession.controller.registerCallback(mMediaControllerCallback)

        mSessionConnector.setQueueNavigator(ChannelQueueNavigator())
        mSessionConnector.setErrorMessageProvider(MediaErrorMessageProvider())
        mSessionConnector.setPlayer(mPlayer, MediaPlaybackPreparer())

        setAttachedActivity()

        initNoisyReceiver()
        retrieveAudioFocus()
        //Lock WiFi during service operation
        mWifiLock.acquire()

        super.setSessionToken(mSession.sessionToken)

        MediaNotificationHelper.createNotification(this, mSession)
    }

    override fun onDestroy() {
        super.onDestroy()

        abandonAudioFocus()

        unregisterReceiver(mNoisyReceiver)
        mSession.release()
        if(mWifiLock.isHeld) mWifiLock.release()
        NotificationManagerCompat.from(this).cancel(1)
    }

    private fun setAttachedActivity() {
        val context = applicationContext
        val intent = Intent(context, DiscoverFragmentActivity::class.java)
        val pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mSession.setSessionActivity(pi)
    }

    private fun retrieveAudioFocus(): Boolean {
        val result : Int
        if(RoosterUtils.hasO()) {
            val mPlaybackAttributes =  AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mPlaybackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build()

            result = audioManager.requestAudioFocus(mFocusRequest)
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }

        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    private fun abandonAudioFocus() {
        //Abandon audio focus
        if(RoosterUtils.hasO()) {
            audioManager.abandonAudioFocusRequest(mFocusRequest)
        } else {
            audioManager.abandonAudioFocus(this)
        }
    }

    private fun initNoisyReceiver() {
        // Handles headphones coming unplugged. Cannot be done through a manifest receiver.
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mNoisyReceiver, filter)
    }

    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mPlaybackState.state == PlaybackStateCompat.STATE_PLAYING) {
                mTransportControls.pause()
            }
        }
    }

    //https://medium.com/google-developers/how-to-properly-handle-audio-interruptions-3a13540d18fa
    override fun onAudioFocusChange(focusChange: Int) {
        val state = mPlaybackState.state
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (state == PlaybackStateCompat.STATE_PLAYING)
                    mTransportControls.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mTransportControls.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                lastVolume = mPlayer.volume
                mPlayer.volume = lastVolume*0.3f
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (state == PlaybackStateCompat.STATE_STOPPED)
                    mTransportControls.play()
                mPlayer.volume = lastVolume
            }
        }
    }

    class MediaErrorMessageProvider : MediaSessionConnector.ErrorMessageProvider {
        override fun getErrorMessage(playbackException: ExoPlaybackException?): Pair<Int, String> {
            return Pair(1, playbackException.toString())
        }
    }

    inner class MediaPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        override fun getSupportedPrepareActions(): Long {
            return (ACTION_PLAY_FROM_MEDIA_ID)
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) { TODO("not implemented") }
        override fun onPrepareFromSearch(query: String?, extras: Bundle?) { TODO("not implemented") }
        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) { TODO("not implemented") }
        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) { TODO("not implemented")  }
        override fun onPrepare() { TODO("not implemented") }
    }

    inner class ChannelQueueNavigator : TimelineQueueNavigator(mSession) {
        override fun getMediaDescription(windowIndex: Int): MediaDescriptionCompat {
            return getQueueItemMediaDescription(windowIndex)
        }

        override fun onSkipToPrevious(player: Player?) {
            super.onSkipToPrevious(player)
            mTransportControls.play()
        }

        override fun onSkipToNext(player: Player?) {
            super.onSkipToNext(player)
            mTransportControls.play()
        }

        override fun onSkipToQueueItem(player: Player?, id: Long) {
            if(this.getActiveQueueItemId(player) != id) {
                super.onSkipToQueueItem(player, id)
            }
            mTransportControls.play()
        }
    }

    fun getQueueItemMediaDescription(windowIndex: Int) : MediaDescriptionCompat {
        val rooster: ChannelRooster = channelRoosters[windowIndex]

        val mMediaDescriptionCompatBuilder: MediaDescriptionCompat.Builder = MediaDescriptionCompat.Builder()
                .setTitle(rooster.name)
                .setSubtitle("subtitle")
                .setDescription(rooster.description)
                .setMediaId(rooster.channel_uid)
                .setMediaUri(Uri.parse(rooster.audio_file_url))
                .setIconUri(Uri.parse(rooster.channel_photo))

        return mMediaDescriptionCompatBuilder.build()
    }

    inner class PlaybackController : DefaultPlaybackController() {
        override fun onRewind(player: Player) {
            //super.onRewind(player)
            mTransportControls.seekTo(player.currentPosition - TimeUnit.SECONDS.toMillis(10))
            mTransportControls.play()
        }

        override fun onSeekTo(player: Player, pos: Long) {
            super.onSeekTo(player, pos)
        }

        override fun getSupportedPlaybackActions(player: Player?): Long {
            return super.getSupportedPlaybackActions(player)
        }

        override fun onFastForward(player: Player) {
            //super.onFastForward(player)
            mTransportControls.seekTo(player.currentPosition + TimeUnit.SECONDS.toMillis(10))
            mTransportControls.play()
        }

        override fun onPlay(player: Player) {
            if(retrieveAudioFocus()) {
                super.onPlay(player)
                //Ensure service is started to avoid being killed on browser disconnect
                startService(Intent(mThis, MediaService::class.java))
            }

            if (!mSession.isActive) {
                mSession.isActive = true
            }
        }

        override fun onStop(player: Player) {
            super.onStop(player)
            NotificationManagerCompat.from(mThis).cancel(1)
            abandonAudioFocus()
            stopSelf()
        }

        override fun onPause(player: Player) {
            super.onPause(player)
        }
    }

    inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            super.onCommand(command, extras, cb)
        }
    }

    private val mMediaControllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            MediaNotificationHelper.createNotification(mThis, mSession)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            MediaNotificationHelper.createNotification(mThis, mSession)
        }
    }

    val MEDIA_ID_ROOT = "MEDIA_ID_ROOT"
    val MEDIA_ID_EMPTY_ROOT = "MEDIA_ID_EMPTY_ROOT"

    override fun onLoadChildren(parentId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
        // Use result.detach to allow calling result.sendResult from another thread
        result.detach()

        //Load queue async and send
        ChannelManager.onFlagChannelManagerDataListener = object : ChannelManager.Companion.OnFlagChannelManagerDataListener {
            override fun onChannelRoosterDataChanged(freshChannelRoosters: java.util.ArrayList<ChannelRooster>) {
                channelRoosters.clear()
                channelRoosters.addAll(freshChannelRoosters)
            }

            override fun onSyncFinished() {
                mediaSources = ArrayList(channelRoosters.size)

                for(item in channelRoosters) {
                    val dataSourceFactory = DefaultHttpDataSourceFactory("MediaService")
                    val extractor = DefaultExtractorsFactory()
                    val handler = Handler()

                    val mediaSource = ExtractorMediaSource(Uri.parse(item.audio_file_url),
                            dataSourceFactory,
                            extractor,
                            handler, null)

                    mediaSources.add(mediaSource)
                }

                val playerEventListener = object : Player.EventListener {

                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        when(playbackState) {
                            Player.STATE_READY -> {
                                if(mPlayerPreparing) {
                                    loadChildrenImpl(parentId, result)
                                    mPlayerPreparing = false
                                }
                            }
                            Player.STATE_IDLE -> {}
                            Player.STATE_BUFFERING -> {}
                            Player.STATE_ENDED -> {}
                        }
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
                    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}
                    override fun onPlayerError(error: ExoPlaybackException?) {}
                    override fun onLoadingChanged(isLoading: Boolean) {}
                    override fun onPositionDiscontinuity() {}
                    override fun onRepeatModeChanged(repeatMode: Int) {}
                    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {}
                }

                mPlayer.addListener(playerEventListener)
                mPlayerPreparing = true
                mPlayer.prepare(ConcatenatingMediaSource(*mediaSources.toTypedArray()))
            }
        }

        channelManager.refreshChannelData(channelRoosters)
    }

    private fun loadChildrenImpl(parentMediaId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
        var mediaItems : List<MediaBrowserCompat.MediaItem> = ArrayList()

        when(parentMediaId) {
            MEDIA_ID_ROOT -> {
                val tempMediaItems : MutableList<MediaBrowserCompat.MediaItem> = ArrayList(channelRoosters.size)

                 channelRoosters.forEachIndexed { index, _ ->
                     val mediaItem : MediaBrowserCompat.MediaItem = MediaBrowserCompat.MediaItem(getQueueItemMediaDescription(index),
                             MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                     tempMediaItems.add(mediaItem)
                 }

                mediaItems = tempMediaItems.toList()
            }
            // Since the client provided the empty root we'll just send back an
            // empty list
            MEDIA_ID_EMPTY_ROOT -> { return }
        }

        result.sendResult(mediaItems)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
        // Verify the client is authorized to browse media and return the root that
        // makes the most sense here. In this example we simply verify the package name
        // is the same as ours, but more complicated checks, and responses, are possible
        return if (clientPackageName != packageName) {
            // Allow the client to connect, but not browse, by returning an empty root
            MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_EMPTY_ROOT, null)
        } else MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_ROOT, null)
    }
}