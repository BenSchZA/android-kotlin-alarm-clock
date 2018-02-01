/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils

import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.firebase.FA
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.database.User
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.RoosterUtils
import com.roostermornings.android.util.Toaster

import java.io.File
import java.io.IOException
import java.util.ArrayList

import butterknife.BindView
import butterknife.OnClick

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.widget.*
import com.roostermornings.android.firebase.UserMetrics
import kotlinx.android.synthetic.main.content_new_audio.*

class NewAudioRecordActivity : BaseActivity() {

    private var startTime: Long = 0
    private var countDownTime: Long = 0
    //NB: this refresh rate has a direct influence on performance as well as tuning of record time/amplitude calculations
    private val REFRESH_RATE = 100
    private val MAX_RECORDING_TIME = 60000
    private val maxRecordingTime = "60"

    //Silence average: 125
    //Ambient music: 300
    private val AUDIO_MIN_AMPLITUDE = 250
    private val MIN_CUMULATIVE_TIME = 5000
    private var maxAmplitude = 0
    private var averageAmplitude = 0.0
    private var timeSinceAcceptableAmplitudeStart: Long = 0
    private var cumulativeAcceptableAmplitudeTime: Long = 0
    private var timeOfUnsuccessfulAmplitude: Long = 0
    private var timeOfSuccessfulAmplitude: Long = 0

    private val mHandler = Handler()

    private var mAudioSavePathInDevice = ""
    private var mediaRecorder: MediaRecorder? = MediaRecorder()
    private var mediaPlayer: MediaPlayer? = MediaPlayer()
    private var audioLength = 0

    private var randomAudioFileName = ""

    private var newAudioStatus = 0

    @BindView(R.id.new_audio_time)
    lateinit var txtAudioTime: TextView

    @BindView(R.id.new_audio_start_stop)
    lateinit var imgAudioStartStop: ImageView

    @BindView(R.id.new_audio_circle_outer)
    lateinit var layoutRecord: FrameLayout

    @BindView(R.id.new_audio_listen)
    lateinit var imgNewAudioListen: ImageButton

    @BindView(R.id.new_message)
    lateinit var txtMessage: TextView

    private val startTimer = object : Runnable {
        override fun run() {
            when (newAudioStatus) {
                NEW_AUDIO_READY_RECORD -> {
                    mHandler.removeCallbacks(this)
                    txtAudioTime.text = maxRecordingTime
                }
                NEW_AUDIO_RECORD_ERROR -> {
                    mHandler.removeCallbacks(this)
                    txtAudioTime.text = maxRecordingTime
                }
                NEW_AUDIO_PAUSED -> {
                }
                NEW_AUDIO_LISTENING -> {
                    countDownTime = startTime - System.currentTimeMillis()
                    updateTimer(countDownTime.toFloat())
                    mHandler.postDelayed(this, REFRESH_RATE.toLong())
                }
                NEW_AUDIO_RECORDING -> {
                    //This logic checks that the average recording amplitude is above a certain threshold for a cumulative amount of time
                    maxAmplitude = mediaRecorder?.maxAmplitude?:0
                    averageAmplitude = (maxAmplitude + averageAmplitude) / 2

                    //TODO: error here
                    cumulativeAcceptableAmplitudeTime += timeSinceAcceptableAmplitudeStart
                    timeSinceAcceptableAmplitudeStart = 0

                    if (averageAmplitude < AUDIO_MIN_AMPLITUDE && cumulativeAcceptableAmplitudeTime < MIN_CUMULATIVE_TIME) {
                        txtMessage.text = resources.getText(R.string.new_audio_time_amplitude_instructions)
                        timeOfUnsuccessfulAmplitude = System.currentTimeMillis()
                    } else if (cumulativeAcceptableAmplitudeTime > MIN_CUMULATIVE_TIME) {
                        txtMessage.text = resources.getText(R.string.new_audio_instructions)
                    } else {
                        txtMessage.text = resources.getText(R.string.new_audio_time_instructions)
                        timeSinceAcceptableAmplitudeStart = timeOfSuccessfulAmplitude - timeOfUnsuccessfulAmplitude
                        timeOfSuccessfulAmplitude = System.currentTimeMillis()
                    }
                    ////////////////////////////////////////////////////////////////////////////////

                    if (countDownTime < 0) stopRecording()

                    countDownTime = startTime - System.currentTimeMillis()
                    updateTimer(countDownTime.toFloat())
                    mHandler.postDelayed(this, REFRESH_RATE.toLong())
                }
                else -> {
                    mHandler.removeCallbacks(this)
                    txtAudioTime.text = maxRecordingTime
                }
            }
        }
    }

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(R.layout.activity_new_audio)
        BaseApplication.getRoosterApplicationComponent().inject(this)

        setDayNightTheme()
        setButtonBarSelection()

        setNewAudioStatus(NEW_AUDIO_READY_RECORD)

        //Finish this activity when audio process complete - this ensures recreated on backstack
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action == Constants.FINISH_AUDIO_RECORD_ACTIVITY) {
                    finish()
                }
            }
        }, IntentFilter(Constants.FINISH_AUDIO_RECORD_ACTIVITY))
    }

    override fun onStart() {
        super.onStart()
        //Display notifications
        updateRoosterNotification()
        updateRequestNotification()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_new_audio, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        return if (id == R.id.action_send) true
        else super.onOptionsItemSelected(item)
    }

    public override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaRecorder?.release()
    }

    @OnClick(R.id.upload_audio)
    fun uploadAudio() {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "audio/*"
            startActivityForResult(Intent.createChooser(intent, "Select audio file"), 0)
        } catch (e: Exception) {
            //App not found
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val audioFileUri: Uri? = data?.data

            val uploadAudioIntent = Intent(Intent.ACTION_SEND)
            uploadAudioIntent.setDataAndType(audioFileUri, "audio/*")
            uploadAudioIntent.putExtra(Intent.EXTRA_STREAM, audioFileUri)
            uploadAudioIntent.`package` = "com.roostermornings.android"
            startActivity(Intent.createChooser(uploadAudioIntent, "Share audio"))
        }
    }

    @OnClick(R.id.home_friends)
    override fun manageFriends() {
        startActivity(Intent(this@NewAudioRecordActivity, FriendsFragmentActivity::class.java))
    }

    @OnClick(R.id.home_my_alarms)
    fun manageAlarms() {
        startHomeActivity()
    }

    @OnClick(R.id.home_my_uploads)
    override fun manageUploads() {
        startActivity(Intent(this@NewAudioRecordActivity, MessageStatusFragmentActivity::class.java))
    }

    @OnClick(R.id.home_record_audio)
    override fun recordNewAudio() {
        if (!checkInternetConnection()) return
        startActivity(Intent(this@NewAudioRecordActivity, NewAudioRecordActivity::class.java))
    }

    private fun setListenLayoutVisibility(visibility: Int) {
        new_audio_delete.visibility = visibility
        new_audio_save.visibility = visibility
        new_audio_listen_circle_outer.visibility = visibility
    }

    private fun setNewAudioStatus(status: Int) {
        newAudioStatus = status
        when (status) {
            NEW_AUDIO_RECORD_ERROR -> {
                FA.Log(FA.Event.social_rooster_recording_error::class.java, null, null)

                layoutRecord.animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
                mHandler.removeCallbacks(startTimer)
                setListenLayoutVisibility(View.INVISIBLE)
                layoutRecord.visibility = View.VISIBLE
                //Clear red recording focus
                imgAudioStartStop.isSelected = false
                txtMessage.text = resources.getText(R.string.new_audio_quiet_instructions)
                txtAudioTime.text = maxRecordingTime
                txtAudioTime.visibility = View.VISIBLE
            }
            NEW_AUDIO_READY_RECORD -> {
                layoutRecord.animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
                mHandler.removeCallbacks(startTimer)
                setListenLayoutVisibility(View.INVISIBLE)
                layoutRecord.visibility = View.VISIBLE
                //Clear red recording focus
                imgAudioStartStop.isSelected = false
                if (txtMessage.text != resources.getString(R.string.new_audio_post_delete_instructions))
                    txtMessage.text = resources.getText(R.string.new_audio_instructions)
                txtAudioTime.text = maxRecordingTime
                txtAudioTime.visibility = View.VISIBLE
            }
            NEW_AUDIO_READY_LISTEN -> {
                FA.Log(FA.Event.social_rooster_recorded::class.java, null, null)

                layoutRecord.clearAnimation()
                mHandler.removeCallbacks(startTimer)
                layoutRecord.visibility = View.INVISIBLE
                //Clear red recording focus
                imgAudioStartStop.isSelected = false
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_audio_play_button)
                setListenLayoutVisibility(View.VISIBLE)
                txtMessage.text = resources.getText(R.string.new_audio_continue_instructions)

                //Don't display countdown for playback - uncomment to display
                txtAudioTime.visibility = View.GONE

                try {
                    val uri = Uri.parse(mAudioSavePathInDevice)
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(this, uri)
                    val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    audioLength = Integer.parseInt(duration)
                    updateTimer(audioLength.toFloat())
                } catch (e: Resources.NotFoundException) {
                    e.printStackTrace()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }

            }
            NEW_AUDIO_RECORDING -> {
                layoutRecord.animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
                //Reset message from amplitude instructions to default
                txtMessage.text = resources.getText(R.string.new_audio_instructions)
                imgAudioStartStop.isSelected = true

                startTime = System.currentTimeMillis() + MAX_RECORDING_TIME
            }
            NEW_AUDIO_PAUSED -> {
                layoutRecord.clearAnimation()
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_audio_play_button)
            }
            NEW_AUDIO_LISTENING -> {
                layoutRecord.clearAnimation()
                imgNewAudioListen.setBackgroundResource(R.drawable.rooster_new_audio_pause_button)

                startTime = if (mediaPlayer != null && mediaPlayer!!.currentPosition > 0) {
                    System.currentTimeMillis() + audioLength - mediaPlayer!!.currentPosition
                } else {
                    System.currentTimeMillis() + audioLength
                }
            }
            else -> {
            }
        }
    }

    @OnClick(R.id.new_audio_start_stop)
    fun startStopAudioRecording() {
        if (newAudioStatus != NEW_AUDIO_RECORDING) {
            setNewAudioStatus(NEW_AUDIO_RECORDING)

            //Reset acceptable amplitude timer
            timeSinceAcceptableAmplitudeStart = 0
            timeOfUnsuccessfulAmplitude = System.currentTimeMillis()
            timeOfSuccessfulAmplitude = System.currentTimeMillis()
            cumulativeAcceptableAmplitudeTime = 0
            averageAmplitude = 0.0

            randomAudioFileName = RoosterUtils.createRandomUID(5) + Constants.FILENAME_PREFIX_ROOSTER_TEMP_RECORDING + ".3gp"

            if (checkPermission()) {

                mAudioSavePathInDevice = filesDir.absolutePath + "/" +
                        randomAudioFileName

                mediaRecorderReady()

                try {
                    mediaRecorder?.prepare()
                    mediaRecorder?.start()
                    mHandler.removeCallbacks(startTimer)
                    mHandler.postDelayed(startTimer, 0)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            } else {
                requestPermission()
            }

        } else {
            stopRecording()
        }
    }

    private fun stopRecording() {
        try {
            mHandler.removeCallbacks(startTimer)
            mediaRecorder!!.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            deleteAudio()
            setNewAudioStatus(NEW_AUDIO_READY_RECORD)
        } catch (e: RuntimeException) {
            e.printStackTrace()
            deleteAudio()
            setNewAudioStatus(NEW_AUDIO_READY_RECORD)
        }

        //Check if cumulative amplitude condition has not been met, else continue
        if (cumulativeAcceptableAmplitudeTime < MIN_CUMULATIVE_TIME) {
            deleteAudio()
            setNewAudioStatus(NEW_AUDIO_RECORD_ERROR)
        } else {
            setNewAudioStatus(NEW_AUDIO_READY_LISTEN)
        }
    }

    @OnClick(R.id.new_audio_delete)
    fun onDeleteAudioClick() {
        FA.Log(FA.Event.social_rooster_recording_deleted::class.java, null, null)

        setNewAudioStatus(NEW_AUDIO_READY_RECORD)
        txtMessage.text = resources.getString(R.string.new_audio_post_delete_instructions)
        deleteAudio()
    }

    private fun deleteAudio(): Boolean {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaRecorderReady()
        }
        return if (!mAudioSavePathInDevice.isEmpty()) {
            File(mAudioSavePathInDevice).delete()
        } else {
            false
        }
    }

    @OnClick(R.id.new_audio_listen)
    fun onAudioListenClick() {

        try {
            when (newAudioStatus) {
                NEW_AUDIO_READY_LISTEN -> {
                    mediaPlayer = MediaPlayer()
                    try {
                        mediaPlayer?.setDataSource(mAudioSavePathInDevice)
                        mediaPlayer?.prepare()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    mediaPlayer?.start()

                    mHandler.removeCallbacks(startTimer)
                    mHandler.postDelayed(startTimer, 0)
                    mediaPlayer?.setOnCompletionListener { mediaPlayer ->
                        try {
                            setNewAudioStatus(NEW_AUDIO_READY_LISTEN)
                            mediaPlayer.stop()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    setNewAudioStatus(NEW_AUDIO_LISTENING)
                }
                NEW_AUDIO_LISTENING -> {
                    setNewAudioStatus(NEW_AUDIO_PAUSED)
                    mediaPlayer?.pause()
                    mHandler.removeCallbacks(startTimer)
                }
                NEW_AUDIO_PAUSED -> {
                    setNewAudioStatus(NEW_AUDIO_LISTENING)
                    mediaPlayer?.start()
                    mHandler.postDelayed(startTimer, 0)
                }
                else -> {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @OnClick(R.id.new_audio_save)
    fun onSaveAudioFileClick() {
        if (!checkInternetConnection()) return

        //Stop audio if currently playing
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            setNewAudioStatus(NEW_AUDIO_READY_LISTEN)
        }

        //Manage whether audio file is being sent direct to a user or a list of users needs to be shown
        val intent = Intent(this@NewAudioRecordActivity, NewAudioFriendsActivity::class.java)
        val bun = Bundle()
        bun.putString(Constants.EXTRA_LOCAL_FILE_STRING, mAudioSavePathInDevice)

        //If this fails (shouldn't) then user can select from list of friends rather than direct message,
        //no harm done
        try {
            if (intent.extras?.containsKey(Constants.EXTRA_FRIENDS_LIST) == true) {
                val mFriends = getIntent().getSerializableExtra(Constants.EXTRA_FRIENDS_LIST) as ArrayList<User>
                bun.putSerializable(Constants.EXTRA_FRIENDS_LIST, mFriends)
            }
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }

        intent.putExtras(bun)
        startActivity(intent)
    }

    private fun updateTimer(time: Float) {
        val displaySeconds: String

        var seconds = (time / 1000).toLong()

        /* Convert the seconds to String
         * and format to ensure it has
		 * a leading zero when required
		 */
        seconds %= 60
        displaySeconds = seconds.toString()

        /* Setting the timer text to the elapsed time */
        txtAudioTime.text = displaySeconds
    }

    private fun mediaRecorderReady() {
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setAudioChannels(2)
        //Calculated for a 60 second audio clip file size of just over 500kb
        mediaRecorder?.setAudioEncodingBitRate(70000)
        mediaRecorder?.setAudioSamplingRate(48000)
        mediaRecorder?.setOutputFile(mAudioSavePathInDevice)
    }


    private fun requestPermission() {
        ActivityCompat.requestPermissions(this@NewAudioRecordActivity, arrayOf(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO), Constants.MY_PERMISSIONS_REQUEST_AUDIO_RECORD)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.MY_PERMISSIONS_REQUEST_AUDIO_RECORD -> if (grantResults.isNotEmpty()) {
                val storagePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val recordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED

                UserMetrics.setPermission(
                        UserMetrics.Permission.PERMISSION_MIC,
                        recordPermission)
                UserMetrics.setPermission(
                        UserMetrics.Permission.PERMISSION_STORAGE,
                        storagePermission)

                if (storagePermission && recordPermission) {
                    setNewAudioStatus(NEW_AUDIO_READY_RECORD)
                    startStopAudioRecording()
                } else {
                    Toaster.makeToast(this@NewAudioRecordActivity, "Permission denied. Please reconsider?", Toast.LENGTH_LONG).checkTastyToast()
                    setNewAudioStatus(NEW_AUDIO_READY_RECORD)
                }
            }
            else -> {}
        }
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext,
                WRITE_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext,
                RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val NEW_AUDIO_RECORDING = 1
        private val NEW_AUDIO_LISTENING = 2
        private val NEW_AUDIO_PAUSED = 3
        private val NEW_AUDIO_READY_RECORD = 4
        private val NEW_AUDIO_READY_LISTEN = 5
        private val NEW_AUDIO_RECORD_ERROR = 6
    }
}