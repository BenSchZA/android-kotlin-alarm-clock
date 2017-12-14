package com.roostermornings.android.onboarding

import android.app.Activity
import android.os.Handler
import android.widget.ImageView

class InterfaceCommands {
    companion object {
        enum class Command {
            HIDE_FAB, SHOW_FAB, PROCEED
        }
    }
}

interface HostInterface {
    fun setOnboardingProgress(pageNumber: Int)
    fun scrollViewPager(direction: Int)
}

interface CustomCommandInterface {
    fun onCustomCommand(command: InterfaceCommands.Companion.Command)
}

interface ChannelDemoInterface {
    fun performChannelImageTransition(title: String, description: String, drawableID: Int, imageView: ImageView, media: Int)
}

interface FragmentInterface {
    fun fragmentVisible(position: Int)
}

interface ShowcaseInterface {
    fun startShowCase(handler: Handler, activity: Activity)
    fun dismissShowcase()
}