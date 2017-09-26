/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.PowerManager
import android.support.v4.media.AudioAttributesCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.widget.MediaController

/**
 * com.roostermornings.android.service
 * Rooster Mornings Android
 *
 * Created by bscholtz on 22/09/17.
 * Copyright © 2017 Roosta Media. All rights reserved.
 */
class RoosterMediaPlayer(context: Context) : MediaPlayer(), MediaController.MediaPlayerControl,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener {

    init {

        // Make sure the media player will acquire a wake-lock while
        // playing. If we don't do that, the CPU might go to sleep while the
        // song is playing, causing playback to stop.
        this.setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

        // We want the media player to notify us when it's ready preparing,
        // and when it's done playing:
        this.setOnPreparedListener(this)
        this.setOnCompletionListener(this)
        this.setOnErrorListener(this)
        this.setOnSeekCompleteListener(this)

        //Configure audio stream
        this.setAudioStreamType(AudioManager.STREAM_MUSIC)

        //        val builder: AudioAttributesCompat.Builder  = AudioAttributesCompat.Builder()
        //            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
        //            .setFlags(AudioAttributesCompat.FLAG_AUDIBILITY_ENFORCED)
        //            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
        //
        //        this.setAudioAttributes(builder.build())
        //
        //        if()
        //        streamMediaPlayer.setAudioAttributes();
    }

    /**
     * Called when media player is done preparing.
     */
    override fun onPrepared(player: MediaPlayer) {
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.

    }

    override fun onCompletion(p0: MediaPlayer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSeekComplete(p0: MediaPlayer?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setNextMediaPlayer(p0: MediaPlayer?) {
        super.setNextMediaPlayer(p0)
    }

    override fun canPause(): Boolean {
        return true
    }

    override fun getBufferPercentage(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }
}