package com.roostermornings.android.util

import android.app.Activity
import android.os.Handler
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.view.View
import io.realm.Realm
import io.realm.RealmObject
import java.util.*
import javax.inject.Inject

/**
 * Created by bscholtz on 2017/11/13.
 */
class SnackbarManager(val activity: Activity, val view: View) {

    private var currentSnackBar: Snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
    private var snackbarQueue = ArrayList<SnackbarQueueElement>()

    @Inject lateinit var realm: Realm

    init {

    }

    companion object {
        class SnackbarQueueElement(
                var text: String = "",
                var actionText: String = "OK",
                var action: View.OnClickListener? = null,
                var length: Int = Snackbar.LENGTH_LONG,
                var priority: Int = -1,
                var state: State = State.NONE)
    }

    // Set the previous state for activity side logic
    var previousState = State.NONE
    enum class State {
        NONE, SYNCING, FINISHED, NO_INTERNET
    }

    // Timer to manage automatic transition of queue elements
    private val timerTask = object: TimerTask() {
        override fun run() {
            activity.runOnUiThread {
                if (snackbarQueue.isNotEmpty()) {
                    initializeCurrentSnackBar(snackbarQueue[0])
                } else {
                    this.cancel()
                    currentSnackBar.dismiss()
                }
            }
        }
    }

    // Set whether to create new snackbar on addition, or add to queue with timer
    private var renew: Boolean = false
    fun setRenew(shouldRenew: Boolean = false) {
        renew = shouldRenew
    }

    // For snackbar with high priority, lock queue
    private var locked: Boolean = false
    fun setLocked(lock: Boolean = false) {
        locked = lock
        if(locked) timerTask.cancel()
    }

    // Clear the queue
    fun clearQueue() {
        snackbarQueue.clear()
    }

    // Check whether the current snackbar inserted has the highest priority
    private fun hasPriority(priority: Int): Boolean {
        return snackbarQueue.isNotEmpty() && !snackbarQueue.any{ it.priority >= priority }
    }

    // Assign current snack bar from queue element data
    private fun initializeCurrentSnackBar(snackbarQueueElement: SnackbarQueueElement) {
        currentSnackBar.setText(snackbarQueueElement.text)
        currentSnackBar.setAction(snackbarQueueElement.actionText, snackbarQueueElement.action)
        previousState = snackbarQueueElement.state
        snackbarQueue.removeAt(0)
    }

    private fun showSnackbar() {
        currentSnackBar.show()
    }

    // Check if snackbar queue has elements, and if snackbar is currently shown
    private fun checkQueue() {
        if(snackbarQueue.isNotEmpty() && !currentSnackBar.isShownOrQueued) {
            if(renew) {
                val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
                currentSnackBar = snackbar
                currentSnackBar.duration = snackbarQueue[0].length
                initializeCurrentSnackBar(snackbarQueue[0])

                currentSnackBar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        when (event) {
                            BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION -> {
                                if(snackbarQueue.isNotEmpty()) checkQueue()
                                locked = false
                            }
                            BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT -> {
                                if(snackbarQueue.isNotEmpty()) checkQueue()
                                locked = false
                            }
                        }
                    }
                })

                showSnackbar()
            } else {
                /*If renew == false, set snackbar length to indefinite
                * and start timer to transition between snackbars*/
                currentSnackBar.duration = Snackbar.LENGTH_INDEFINITE
                initializeCurrentSnackBar(snackbarQueue[0])
                showSnackbar()
                Timer().schedule(timerTask, 2500, 2500)
            }
        }
    }

    private fun addSnackbarToQueue(element: SnackbarQueueElement): Boolean {
        // If snackbar has priority: lock queue, renew snackbar, and dismiss current snackbar
        return if(hasPriority(element.priority)) {
            setLocked(true)
            renew = true
            clearQueue()
            if(currentSnackBar.isShownOrQueued) {
                currentSnackBar.dismiss()
                currentSnackBar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
            }
            snackbarQueue.add(element)
        } else {
            !locked && snackbarQueue.add(element)
        }
    }

    fun generateSyncing() {
        if(addSnackbarToQueue(SnackbarQueueElement("Alarm content is downloading...", action = View.OnClickListener { }, state = State.SYNCING))) {
            checkQueue()
        }
    }

    fun generateFinished() {
        if(addSnackbarToQueue(SnackbarQueueElement("Alarm content has finished downloading.", action = View.OnClickListener {  }, state = State.FINISHED))) {
            checkQueue()
        }
    }

    fun generateNoInternetConnection() {
        if(addSnackbarToQueue(SnackbarQueueElement("Please connect to the internet to finish downloading alarm content.", action = View.OnClickListener {  }, state = State.NO_INTERNET))) {
            checkQueue()
        }
    }
}