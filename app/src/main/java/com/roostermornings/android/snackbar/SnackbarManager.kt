package com.roostermornings.android.snackbar

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.flipboard.bottomsheet.BottomSheetLayout
import com.google.gson.annotations.Expose
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.realm.RealmManager_ScheduledSnackbar
import io.realm.Realm
import java.util.*
import javax.inject.Inject

/**
 * Created by bscholtz on 2017/11/13.
 */
class SnackbarManager(val activity: Activity, val view: View) {

    private var currentSnackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
    private var currentSnackbarQueueElement = SnackbarQueueElement()
    private var snackbarQueue = ArrayList<SnackbarQueueElement>()

    @Inject lateinit var realm: Realm
    @Inject lateinit var realmManagerScheduledSnackbar: RealmManager_ScheduledSnackbar

    /** Timer to manage refreshing of current snackbar content.*/
    private var timerTask: TimerTask = object: TimerTask(){ override fun run() {} }
    private fun resetTimerTask() {
        timerTask = object: TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    if (snackbarQueue.isNotEmpty()) {
                        initializeCurrentSnackBar(snackbarQueue[0])
                    } else {
                        timerTask.cancel()
                        currentSnackbar.dismiss()
                    }
                }
            }
        }
    }

    /** Recreate timer task to ensure only a single timer is running at any given time.*/
    private fun clearTimerTask() {
        timerTask.cancel()
        resetTimerTask()
    }

    private val bottomSheet: BottomSheetLayout
    private val activityContentView: CoordinatorLayout

    /** Snackbar callback used for snackbar renewal display method.*/
    private val callback1 = object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
        override fun onShown(transientBottomBar: Snackbar?) {
            super.onShown(transientBottomBar)
        }

        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            when (event) {
                BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_ACTION -> {
                    if(snackbarQueue.isNotEmpty()) {
                        checkQueue()
                    }
                    locked = false
                }
                BaseTransientBottomBar.BaseCallback.DISMISS_EVENT_TIMEOUT -> {
                    if(snackbarQueue.isNotEmpty()) {
                        checkQueue()
                    }
                    locked = false
                }
            }
        }
    }

    /** Snackbar callback used for snackbar timer refresh display method.*/
    private val callback2 = object : BaseTransientBottomBar.BaseCallback<Snackbar>() {

        override fun onShown(transientBottomBar: Snackbar?) {
            super.onShown(transientBottomBar)
        }

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
    }

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)

        /** On initialization, fetch all relevant snackbars for current activity.*/
        realmManagerScheduledSnackbar.getScheduledSnackbarsForActivity(activity).forEach {
            if(addSnackbarToQueue(it.snackbarQueueElement)) checkQueue()
        }
        /** Listen for scheduled snackbars added to realm database while activity is running.*/
        realmManagerScheduledSnackbar.listenForScheduledSnackbars(activity) {
            realmManagerScheduledSnackbar.getScheduledSnackbarsForActivity(activity).forEach {
                if(addSnackbarToQueue(it.snackbarQueueElement)) checkQueue()
            }
        }

        /** Initialize bottom sheet (snackbar dialog) and coordinator layout view references.*/
        bottomSheet = activity.findViewById(R.id.snackbarBottomsheet)
        activityContentView = activity.findViewById(R.id.my_alarms_coordinator_layout)
    }

    /** Element inserted into realm database and used for keeping track of pending snackbars
     * in snackbar queue.*/
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
                var state: State = State.NONE,
                @Expose
                var dialog: Boolean = false,
                @Expose
                var dialogTitle: String = "",
                @Expose
                var dialogText: String = "")
    }

    /** Ensure this method is called, else instances of this class will be kept and called from
     * the RealmResults listener.*/
    fun destroy() {
        realmManagerScheduledSnackbar.removeListeners()
        realmManagerScheduledSnackbar.closeRealm()
    }

    /** Set the previous state for activity side logic*/
    var previousState = State.NONE
    enum class State {
        NONE, SYNCING, FINISHED, NO_INTERNET
    }

    /** Set whether to create new snackbar on addition, or add to queue with timer*/
    private var renew: Boolean = false
    fun setRenew(shouldRenew: Boolean = false) {
        renew = shouldRenew
    }

    /** For snackbar with high priority, lock queue. No more elements can be added.*/
    private var locked: Boolean = false
    fun setLocked(lock: Boolean = false) {
        locked = lock
        if(locked) clearTimerTask()
    }

    fun clearQueue() {
        snackbarQueue.clear()
    }

    /** Check whether the current snackbar inserted has the highest priority*/
    private fun hasPriority(priority: Int): Boolean {
        return snackbarQueue.isNotEmpty() && !snackbarQueue.any{ it.priority >= priority } || priority > -1
    }

    /** Configure current snackbar from queue element data, then remove element from queue.*/
    private fun initializeCurrentSnackBar(snackbarQueueElement: SnackbarQueueElement) {
        currentSnackbar.setText(snackbarQueueElement.text)
        currentSnackbar.setAction(
                snackbarQueueElement.actionText,
                snackbarQueueElement.action ?: View.OnClickListener {  })

        if(snackbarQueueElement.dialog) {
            currentSnackbar.setAction(snackbarQueueElement.actionText, View.OnClickListener {
                val view = LayoutInflater.from(activity.applicationContext).inflate(R.layout.snackbar_dialog, bottomSheet, false)

                val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)
                dialogTitle.text = snackbarQueueElement.dialogTitle
                val dialogText = view.findViewById<TextView>(R.id.dialogText)
                dialogText.text = snackbarQueueElement.dialogText

                val yesButton = view.findViewById<Button>(R.id.snackbarDialogButtonYes)
                val noButton = view.findViewById<Button>(R.id.snackbarDialogButtonNo)

                // For now, on button click, dismiss sheet
                yesButton.setOnClickListener {
                    bottomSheet.dismissSheet()
                }
                noButton.setOnClickListener {
                    bottomSheet.dismissSheet()
                }

                bottomSheet.showWithSheetView(view)
                bottomSheet.peekSheetTranslation = activityContentView.height*0.3f
                bottomSheet.peekSheet()
            })
        }
        previousState = snackbarQueueElement.state

        currentSnackbarQueueElement = snackbarQueueElement
        snackbarQueue.removeAt(0)
    }

    private fun showSnackbar() {
        currentSnackbar.show()
    }

    /** Allow current snackbar to be dismissed from SnackbarManager instance.*/
    fun dismissSnackbar() {
        currentSnackbar.dismiss()
    }

    private fun resetCurrentSnackbar() {
        currentSnackbar.dismiss()
        currentSnackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
    }

    /**Check if snackbar queue has elements, and if snackbar is currently shown.
     * Two snackbar display methods exist:
     *  1) Dismiss and recreate current snackbar, or
     *  2) Add snackbar to a queue and use timer to refresh snackbar content*/
    private fun checkQueue() {
        if(snackbarQueue.isNotEmpty() && !currentSnackbar.isShownOrQueued) {
            resetCurrentSnackbar()
            initializeCurrentSnackBar(snackbarQueue[0])
            if(renew) {
                currentSnackbar.duration = currentSnackbarQueueElement.length

                currentSnackbar.removeCallback(callback1)
                currentSnackbar.addCallback(callback1)

                showSnackbar()
            } else {
                /*If renew == false, set snackbar length to indefinite
                * and start timer to transition between snackbars*/
                currentSnackbar.duration = Snackbar.LENGTH_INDEFINITE

                currentSnackbar.removeCallback(callback2)
                currentSnackbar.addCallback(callback2)

                showSnackbar()
                clearTimerTask()
                Timer().schedule(timerTask, 2500, 2500)
            }
        }
    }

    /** Add snackbar to a queue, checking if the queue is empty and no snackbar showing, then
     * unlock the queue.
     * If the snackbar has priority, do the following:
     *  1) Lock the queue
     *  2) Set the snackbar display method to renew
     *  3) Clear the queue
     *  4) Reset the snackbar if necessary
     *  5) Add snackbar to queue, and return that addition was successful
     *
     * If the snackbar doesn't have priority, only add it to the queue if the
     * queue is not locked.*/
    private fun addSnackbarToQueue(element: SnackbarQueueElement): Boolean {
        if(snackbarQueue.isEmpty() && !currentSnackbar.isShownOrQueued) setLocked(false)
        // If snackbar has priority: lock queue, renew snackbar, and dismiss current snackbar
        return if(hasPriority(element.priority)) {
            setLocked(true)
            renew = true
            clearQueue()
            if(currentSnackbar.isShownOrQueued) {
                resetCurrentSnackbar()
            }
            snackbarQueue.add(element)
        } else {
            !locked && snackbarQueue.add(element)
        }
    }

    /** Generate the alarm content sync status snackbars: syncing, finished, and no-internet.*/
    fun generateSyncing() {
        if(addSnackbarToQueue(SnackbarQueueElement("Alarm content is downloading...", action = View.OnClickListener { }, state = State.SYNCING, priority = 1))) {
            checkQueue()
        }
    }

    fun generateFinished() {
        if(addSnackbarToQueue(SnackbarQueueElement("Alarm content has finished downloading.", action = View.OnClickListener { }, state = State.FINISHED, priority = 1))) {
            checkQueue()
        }
    }

    fun generateNoInternetConnection() {
        if(addSnackbarToQueue(SnackbarQueueElement("Please connect to the internet to finish downloading alarm content.", action = View.OnClickListener { }, state = State.NO_INTERNET, length = Snackbar.LENGTH_INDEFINITE, priority = 1))) {
            checkQueue()
        }
    }
}