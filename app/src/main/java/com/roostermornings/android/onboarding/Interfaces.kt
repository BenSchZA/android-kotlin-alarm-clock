package com.roostermornings.android.onboarding

import android.app.Activity
import android.media.MediaPlayer
import android.os.Handler
import android.widget.ImageView

class InterfaceCommands {
    companion object {
        enum class Command {
            HIDE_FAB, SHOW_FAB, SCROLL_RIGHT
        }
    }
}

interface HostInterface {
    fun setOnboardingProgress(pageNumber: Int)
    fun scrollViewPager(direction: Int)
    fun customCommand(command: InterfaceCommands.Companion.Command)
}

interface ChannelDemoInterface {
    fun performChannelImageTransition(title: String, drawableID: Int, imageView: ImageView, media: Int)
}

interface FragmentInterface {
    fun fragmentVisible(position: Int)
    fun customCommand(command: InterfaceCommands.Companion.Command)
}

interface ShowcaseInterface {
    fun startShowCase(handler: Handler, activity: Activity)
}