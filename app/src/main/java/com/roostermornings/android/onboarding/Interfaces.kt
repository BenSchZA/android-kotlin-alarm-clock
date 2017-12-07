package com.roostermornings.android.onboarding

import android.app.Activity
import android.media.MediaPlayer
import android.os.Handler
import android.widget.ImageView

interface HostInterface {
    fun setOnboardingProgress(pageNumber: Int)
    fun scrollViewPager(direction: Int)
}

interface ChannelDemoInterface {
    fun performChannelImageTransition(title: String, drawableID: Int, imageView: ImageView, media: Int)
}

interface FragmentInterface {
    fun fragmentVisible(position: Int)
}

interface ShowcaseInterface {
    fun startShowCase(handler: Handler, activity: Activity)
}