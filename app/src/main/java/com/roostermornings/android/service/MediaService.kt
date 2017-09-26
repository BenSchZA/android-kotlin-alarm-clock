/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service

import android.app.Notification
import android.app.NotificationManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import com.roostermornings.android.R
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaController
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.app.NotificationCompat
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
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.roostermornings.android.activity.MyAlarmsFragmentActivity
import com.roostermornings.android.channels.ChannelManager
import com.roostermornings.android.domain.ChannelRooster
import com.roostermornings.android.util.RoosterUtils


class MediaService : MediaBrowserServiceCompat(),
        AudioManager.OnAudioFocusChangeListener{

    companion object {
        @JvmField
        val ACTION_PLAY = "action_play"
        @JvmField
        val ACTION_PAUSE = "action_pause"
        @JvmField
        val ACTION_REWIND = "action_rewind"
        @JvmField
        val ACTION_FAST_FORWARD = "action_fast_foward"
        @JvmField
        val ACTION_NEXT = "action_next"
        @JvmField
        val ACTION_PREVIOUS = "action_previous"
        @JvmField
        val ACTION_STOP = "action_stop"
    }

    val mThis: Context by lazy { this }

    var mFocusRequest : AudioFocusRequest? = null

    val mSession: MediaSessionCompat by lazy { MediaSessionCompat(this, "MediaService") }
    val mSessionConnector: MediaSessionConnector by lazy { MediaSessionConnector(mSession, DefaultPlaybackController()) }
    val mTransportControls: MediaControllerCompat.TransportControls by lazy { mSession.controller.transportControls }

    val mTrackSelector: TrackSelector = DefaultTrackSelector()
    val mPlayer by lazy{ ExoPlayerFactory.newSimpleInstance(this, mTrackSelector) }

    var lastVolume : Float = 0f

    val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val channelManager: ChannelManager by lazy { ChannelManager(this, this) }
    var queueItems: ArrayList<ChannelRooster> = ArrayList()

    val mWifiLock by lazy { (this.getSystemService(Context.WIFI_SERVICE) as WifiManager).createWifiLock(WifiManager.WIFI_MODE_FULL, "RadiophonyLock")  }

    fun getQueueItemMediaDescription(windowIndex: Int) : MediaDescriptionCompat {
        val rooster: ChannelRooster = queueItems.get(windowIndex)

        val mMediaDescriptionCompatBuilder: MediaDescriptionCompat.Builder = MediaDescriptionCompat.Builder()
                .setDescription(rooster.description)
                .setMediaId(rooster.channel_uid)
                .setMediaUri(Uri.parse(rooster.audio_file_url))
                .setIconUri(Uri.parse(rooster.channel_photo))
                .setTitle(rooster.name)

        return mMediaDescriptionCompatBuilder.build()
    }

    class MediaErrorMessageProvider : MediaSessionConnector.ErrorMessageProvider {
        override fun getErrorMessage(playbackException: ExoPlaybackException?): Pair<Int, String> {
            return Pair(1, playbackException.toString())
        }
    }

    class MediaPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {
        override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getSupportedPrepareActions(): Long {
            return (PlaybackStateCompat.ACTION_PREPARE
                    or PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                    or PlaybackStateCompat.ACTION_PREPARE_FROM_URI
                    or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackStateCompat.ACTION_PLAY_FROM_URI);
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onPrepare() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    inner class ChannelQueueNavigator : TimelineQueueNavigator(mSession) {
        override fun getMediaDescription(windowIndex: Int): MediaDescriptionCompat {
            return getQueueItemMediaDescription(windowIndex)
        }
    }

    private fun setAttachedActivity() {
        val context = applicationContext
        val intent = Intent(context, MyAlarmsFragmentActivity::class.java)
        val pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mSession.setSessionActivity(pi)
    }

    private fun initNoisyReceiver() {
        // Handles headphones coming unplugged. Cannot be done through a manifest receiver.
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(mNoisyReceiver, filter)
    }

    private val mNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                mTransportControls.pause()
            }
        }
    }

    //https://medium.com/google-developers/how-to-properly-handle-audio-interruptions-3a13540d18fa
    override fun onAudioFocusChange(focusChange: Int) {
        val state = mSession.controller.playbackState.state
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (state == PlaybackStateCompat.STATE_PLAYING)
                    mTransportControls.stop()
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

    override fun onDestroy() {
        super.onDestroy()

        //Abandon audio focus
        if(RoosterUtils.hasO()) {
            audioManager.abandonAudioFocusRequest(mFocusRequest)
        } else {
            audioManager.abandonAudioFocus(this)
        }

        unregisterReceiver(mNoisyReceiver)
        mSession.release()
        if(mWifiLock.isHeld) mWifiLock.release()
        NotificationManagerCompat.from(this).cancel(1)
    }

    override fun onCreate() {
        super.onCreate()

        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mSession.setCallback(TransportControlsCallback())
        mSessionConnector.setQueueNavigator(ChannelQueueNavigator())
        mSessionConnector.setErrorMessageProvider(MediaErrorMessageProvider())
        mSessionConnector.setPlayer(mPlayer, MediaPlaybackPreparer())

        setAttachedActivity()

        initNoisyReceiver()
        retrieveAudioFocus()
        //Lock WiFi during service operation
        mWifiLock.acquire()

        mSession.isActive = true

        buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY))

        ChannelManager.onFlagChannelManagerDataListener = object : ChannelManager.Companion.OnFlagChannelManagerDataListener {
            override fun onChannelRoosterDataChanged(freshChannelRoosters: java.util.ArrayList<ChannelRooster>) {
                queueItems.clear()
                queueItems.addAll(freshChannelRoosters)
            }

            override fun onSyncFinished() {
                val mediaSources: ArrayList<MediaSource> = ArrayList(queueItems.size)
                for(item in queueItems) {
                    val dataSourceFactory = DefaultHttpDataSourceFactory("MediaService")
                    val extractor = DefaultExtractorsFactory()
                    val handler = Handler()

                    val mediaSource = ExtractorMediaSource(Uri.parse(item.audio_file_url),
                            dataSourceFactory,
                            extractor,
                            handler, null)

                    mediaSources.add(mediaSource)
                }

                mPlayer.playWhenReady = true
                mPlayer.prepare(ConcatenatingMediaSource(*mediaSources.toTypedArray()))
            }
        }

        channelManager.refreshChannelData(queueItems)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        handleIntent(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleIntent(intent: Intent) {
        when(intent.action) {
            ACTION_PLAY -> {
                mTransportControls.play()
            }
            ACTION_PAUSE -> {
                mTransportControls.pause()
            }
            ACTION_FAST_FORWARD -> {
                mTransportControls.fastForward()
            }
            ACTION_REWIND -> {
                mTransportControls.rewind()
            }
            ACTION_PREVIOUS -> {
                mTransportControls.skipToPrevious()
            }
            ACTION_NEXT -> {
                mTransportControls.skipToNext()
            }
            ACTION_STOP -> {
                mTransportControls.stop()
            }
        }
    }

    inner class TransportControlsCallback : MediaSessionCompat.Callback() {
        override fun onRewind() {
            super.onRewind()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
        }

        override fun onPrepare() {
            super.onPrepare()
        }

        override fun onFastForward() {
            super.onFastForward()
        }

        override fun onPlay() {
            if(retrieveAudioFocus()) {
                super.onPlay()
                buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
            }
        }

        override fun onStop() {
            super.onStop()
            NotificationManagerCompat.from(mThis).cancel(1)
            val intent = Intent(applicationContext, MediaService::class.java)
            stopService(intent)
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE))
        }

        override fun onPause() {
            super.onPause()
            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY))
        }
    }

    private fun generateAction(icon: Int, title: String, intentAction: String): android.support.v4.app.NotificationCompat.Action {
        val intent = Intent(applicationContext, MediaService::class.java)
        intent.action = intentAction
        val pendingIntent = PendingIntent.getService(applicationContext, 1, intent, 0);
        return android.support.v4.app.NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private fun buildNotification(action: android.support.v4.app.NotificationCompat.Action) {
        val style = NotificationCompat.MediaStyle()
                // Attach our MediaSession token
                .setMediaSession(mSession.sessionToken)

        //Pending intent to delete notification and kill service
        val intent = Intent(applicationContext, MediaService::class.java)
        intent.action = ACTION_STOP
        val pendingIntent = PendingIntent.getService(applicationContext, 1, intent, 0)

        val notificationBuilder =  android.support.v4.app.NotificationCompat.Builder(this, "MediaService")
                .setSmallIcon(R.drawable.logo_icon)
                .setContentTitle("Media Title")
                .setContentText("Media Artist")
                .setDeleteIntent(pendingIntent)
                .setStyle(style)

        notificationBuilder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS))
        notificationBuilder.addAction(generateAction(android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND))
        notificationBuilder.addAction(action);
        notificationBuilder.addAction(generateAction(android.R.drawable.ic_media_ff, "Fast Foward", ACTION_FAST_FORWARD))
        notificationBuilder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notificationBuilder.build())
    }

    //Not important for general audio service, required for class
    override fun onLoadChildren(parentId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null)
    }

    //Not important for general audio service, required for class
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
        return if (TextUtils.equals(clientPackageName, packageName)) {
            MediaBrowserServiceCompat.BrowserRoot(getString(R.string.app_name), null)
        } else null
    }
}

//class MediaService : MediaBrowserServiceCompat(),
//        MediaPlayer.OnCompletionListener,
//        MediaPlayer.OnPreparedListener,
//        AudioManager.OnAudioFocusChangeListener {
//

//
//    private var mMediaPlayer: MediaPlayer? = null
//    private var mMediaSessionCompat: MediaSessionCompat? = null
//
//    companion object {
//        val COMMAND_EXAMPLE = "command_example"
//    }
//
//    init {
//
//    }
//
//    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent)
//        return super.onStartCommand(intent, flags, startId)
//    }
//

//
//    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
//
//        override fun onPlay() {
//            super.onPlay()
//            if (!successfullyRetrievedAudioFocus()) {
//                return
//            }
//
//            mMediaSessionCompat!!.isActive = true
//            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
//
//            showPlayingNotification()
//            mMediaPlayer!!.start()
//        }
//
//        override fun onPause() {
//            super.onPause()
//
//            if (mMediaPlayer!!.isPlaying) {
//                mMediaPlayer!!.pause()
//                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
//                showPausedNotification()
//            }
//        }
//
//        override fun onStop() {
//            super.onStop()
//        }
//
//        override fun onSkipToNext() {
//            super.onSkipToNext()
//        }
//
//        override fun onSkipToPrevious() {
//            super.onSkipToPrevious()
//        }
//
//        override fun onSkipToQueueItem(queueId: Long) {
////            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
////                // set the current index on queue from the music Id:
////                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId)
////                // play the music
////                handlePlayRequest()
////            }
//            super.onSkipToQueueItem(queueId)
//        }
//
//        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
//            super.onPlayFromMediaId(mediaId, extras)
//
//            try {
//                val afd = resources.openRawResourceFd(Integer.valueOf(mediaId)!!) ?: return
//
//                try {
//                    mMediaPlayer!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
//                } catch (e: IllegalStateException) {
//                    mMediaPlayer!!.release()
//                    initMediaPlayer()
//                    mMediaPlayer!!.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
//                }
//
//                afd.close()
//                initMediaSessionMetadata()
//
//            } catch (e: IOException) {
//                return
//            }
//
//            try {
//                mMediaPlayer!!.prepare()
//            } catch (e: IOException) {
//            }
//
//            //Work with extras here if you want
//        }
//
//        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
//            super.onCommand(command, extras, cb)
//            if (COMMAND_EXAMPLE.equals(command!!, ignoreCase = true)) {
//                //Custom command here
//            }
//        }
//
//        override fun onSeekTo(pos: Long) {
//            super.onSeekTo(pos)
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        initMediaPlayer()
//        initMediaSession()
//        initNoisyReceiver()
//
//        mMediaSessionCallback.onPlay()
//    }
//

//
//    override fun onDestroy() {
//        super.onDestroy()
//        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        audioManager.abandonAudioFocus(this)
//        unregisterReceiver(mNoisyReceiver)
//        mMediaSessionCompat!!.release()
//        NotificationManagerCompat.from(this).cancel(1)
//    }
//
//    private fun initMediaPlayer() {
//        mMediaPlayer = RoosterMediaPlayer(this)
//        //TODO: create class for managing queue
//        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//        mMediaPlayer!!.setDataSource(this, notification)
//
//        mMediaPlayer!!.prepareAsync();
//    }
//
//    private fun showPlayingNotification() {
//        val builder = MediaStyleHelper.from(this@MediaService, mMediaSessionCompat) ?: return
//
//        builder.addAction(android.support.v4.app.NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
//        builder.setStyle(NotificationCompat.MediaStyle().setShowActionsInCompactView(0).setMediaSession(mMediaSessionCompat!!.sessionToken))
//        builder.setSmallIcon(R.mipmap.ic_launcher)
//        NotificationManagerCompat.from(this@MediaService).notify(1, builder.build())
//    }
//
//    private fun showPausedNotification() {
//        val builder = MediaStyleHelper.from(this, mMediaSessionCompat) ?: return
//
//        builder.addAction(android.support.v4.app.NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
//        builder.setStyle(NotificationCompat.MediaStyle().setShowActionsInCompactView(0).setMediaSession(mMediaSessionCompat!!.sessionToken))
//        builder.setSmallIcon(R.mipmap.ic_launcher)
//        NotificationManagerCompat.from(this).notify(1, builder.build())
//    }
//
//    private fun initMediaSession() {
//        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
//        mMediaSessionCompat = MediaSessionCompat(applicationContext, "Tag", mediaButtonReceiver, null)
//
//        mMediaSessionCompat!!.setCallback(mMediaSessionCallback)
//        mMediaSessionCompat!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
//
//        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
//        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0)
//        mMediaSessionCompat!!.setMediaButtonReceiver(pendingIntent)
//
//        sessionToken = mMediaSessionCompat!!.sessionToken
//    }
//
//    private fun setMediaPlaybackState(state: Int) {
//        val playbackstateBuilder = PlaybackStateCompat.Builder()
//
//        if (state == PlaybackStateCompat.STATE_PLAYING) {
//            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE)
//        } else {
//            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY)
//        }
//        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
//
//        mMediaSessionCompat!!.setPlaybackState(playbackstateBuilder.build())
//    }
//
//    private fun initMediaSessionMetadata() {
//        val metadataBuilder = MediaMetadataCompat.Builder()
//        //Notification icon in card
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
//
//        //lock screen icon for pre lollipop
//        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
//        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Display Title")
//        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Display Subtitle")
//        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 1)
//        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 1)
//
//        mMediaSessionCompat!!.setMetadata(metadataBuilder.build())
//    }
//
//    private fun successfullyRetrievedAudioFocus(): Boolean {
//        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//
//        val result = audioManager.requestAudioFocus(this,
//                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
//
//        return result == AudioManager.AUDIOFOCUS_GAIN
//    }
//
//
//    //Not important for general audio service, required for class
//    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
//        return if (TextUtils.equals(clientPackageName, packageName)) {
//            MediaBrowserServiceCompat.BrowserRoot(getString(R.string.app_name), null)
//        } else null
//    }
//
//    //Not important for general audio service, required for class
//    override fun onLoadChildren(parentId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
//        result.sendResult(null)
//    }
//
//    override fun onAudioFocusChange(focusChange: Int) {
//        when (focusChange) {
//            AudioManager.AUDIOFOCUS_LOSS -> {
//                if (mMediaPlayer!!.isPlaying)
//                    mMediaPlayer!!.stop()
//            }
//            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
//                mMediaPlayer!!.pause()
//            }
//            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
//                if (mMediaPlayer != null)
//                    mMediaPlayer!!.setVolume(0.3f, 0.3f)
//            }
//            AudioManager.AUDIOFOCUS_GAIN -> {
//                if (mMediaPlayer != null) {
//                    if (!mMediaPlayer!!.isPlaying)
//                        mMediaPlayer!!.start()
//                    mMediaPlayer!!.setVolume(1.0f, 1.0f)
//                }
//            }
//        }
//    }
//
//    override fun onCompletion(mediaPlayer: MediaPlayer) {
//        mMediaPlayer?.release()
//    }
//
//    override fun onPrepared(p0: MediaPlayer?) {
//        mMediaPlayer?.start()
//    }