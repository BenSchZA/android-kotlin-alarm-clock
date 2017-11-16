package com.roostermornings.android.util

import android.app.Activity
import android.os.Handler
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.view.View
import com.google.gson.annotations.Expose
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.realm.RealmManager_ScheduledSnackbar
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
    @Inject lateinit var realmManagerScheduledSnackbar: RealmManager_ScheduledSnackbar

    // Timer to manage automatic transition of queue elements
    private var timerTask: TimerTask
    private fun resetTimerTask() {
        timerTask = object: TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    if(snackbarQueue.isNotEmpty()) snackbarQueue.removeAt(0)
                    if (snackbarQueue.isNotEmpty()) {
                        initializeCurrentSnackBar(snackbarQueue[0])
                    } else {
                        this.cancel()
                        currentSnackBar.dismiss()
                    }
                }
            }
        }
    }

    private fun clearTimerTask() {
        timerTask.cancel()
        resetTimerTask()
    }

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
        timerTask = object: TimerTask(){ override fun run() {} }
        realmManagerScheduledSnackbar.getScheduledSnackbarsForActivity(activity).forEach {
            if(addSnackbarToQueue(it.snackbarQueueElement)) checkQueue()
        }
        realmManagerScheduledSnackbar.listenForScheduledSnackbars(activity) {
            realmManagerScheduledSnackbar.getScheduledSnackbarsForActivity(activity).forEach {
                if(addSnackbarToQueue(it.snackbarQueueElement)) checkQueue()
            }
        }
    }

    companion object {
        class SnackbarQueueElement(
                @Expose
                var text: String = "",
                @Expose
                var actionText: String = "OK",
                @Expose
                var action: View.OnClickListener? = null,
                @Expose
                var length: Int = Snackbar.LENGTH_LONG,
                @Expose
                var priority: Int = -1,
                @Expose
                var state: State = State.NONE)
    }

    fun closeRealm() {
        realmManagerScheduledSnackbar.closeRealm()
    }

    // Set the previous state for activity side logic
    var previousState = State.NONE
    enum class State {
        NONE, SYNCING, FINISHED, NO_INTERNET
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
        if(locked) clearTimerTask()
    }

    // Clear the queue
    fun clearQueue() {
        snackbarQueue.clear()
    }

    // Check whether the current snackbar inserted has the highest priority
    private fun hasPriority(priority: Int): Boolean {
        return snackbarQueue.isNotEmpty() && !snackbarQueue.any{ it.priority >= priority } || priority > -1
    }

    // Assign current snack bar from queue element data
    private fun initializeCurrentSnackBar(snackbarQueueElement: SnackbarQueueElement) {
        currentSnackBar.setText(snackbarQueueElement.text)
        currentSnackBar.setAction(snackbarQueueElement.actionText, snackbarQueueElement.action)
        previousState = snackbarQueueElement.state
    }

    private fun showSnackbar() {
        currentSnackBar.show()
    }

    private fun resetCurrentSnackbar() {
        currentSnackBar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
    }

    // Check if snackbar queue has elements, and if snackbar is currently shown
    private fun checkQueue() {
        if(snackbarQueue.isNotEmpty() && !currentSnackBar.isShownOrQueued) {
            resetCurrentSnackbar()
            initializeCurrentSnackBar(snackbarQueue[0])
            if(renew) {
                currentSnackBar.duration = snackbarQueue[0].length

                currentSnackBar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        when (event) {
                            BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION -> {
                                if(snackbarQueue.isNotEmpty()) {
                                    snackbarQueue.removeAt(0)
                                    checkQueue()
                                }
                                locked = false
                            }
                            BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT -> {
                                if(snackbarQueue.isNotEmpty()) {
                                    snackbarQueue.removeAt(0)
                                    checkQueue()
                                }
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

                currentSnackBar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        when (event) {
                            BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_MANUAL -> {
                                locked = false
                            }
                            BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION -> {
                                locked = false
                            }
                            BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT -> {
                                locked = false
                            }
                        }
                    }
                })

                showSnackbar()
                clearTimerTask()
                Timer().schedule(timerTask, 2500, 2500)
            }
        }
    }

    private fun addSnackbarToQueue(element: SnackbarQueueElement): Boolean {
        if(snackbarQueue.isEmpty() && !currentSnackBar.isShownOrQueued) setLocked(false)
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
        if(addSnackbarToQueue(SnackbarQueueElement("Please connect to the internet to finish downloading alarm content.", action = View.OnClickListener {  }, state = State.NO_INTERNET, length = Snackbar.LENGTH_INDEFINITE, priority = 1))) {
            checkQueue()
        }
    }
}