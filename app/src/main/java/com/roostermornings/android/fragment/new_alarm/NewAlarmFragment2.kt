/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.new_alarm

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.adapter.ChannelsListAdapter
import com.roostermornings.android.adapter_data.ChannelManager
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.database.Alarm
import com.roostermornings.android.domain.database.AlarmChannel
import com.roostermornings.android.domain.database.ChannelRooster
import com.roostermornings.android.fragment.IAlarmSetListener
import com.roostermornings.android.fragment.base.BaseFragment
import com.roostermornings.android.util.FileUtils
import com.roostermornings.android.util.JSONPersistence
import com.roostermornings.android.util.RoosterUtils
import com.roostermornings.android.util.Toaster

import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.ArrayList
import java.util.Date

import javax.inject.Inject

import butterknife.BindView

import android.app.Activity.RESULT_OK

class NewAlarmFragment2 : BaseFragment() {
    private var mUserUidParam: String? = null
    private var mListener: IAlarmSetListener? = null

    private val channelRoosters = ArrayList<ChannelRooster>()

    @BindView(R.id.main_content)
    lateinit var mRecyclerView: RecyclerView

    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    @Inject lateinit var jsonPersistence: JSONPersistence
    @Inject lateinit var channelManager: ChannelManager

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    interface ChannelInterface {
        fun setSelectedChannel(channelRooster: ChannelRooster)
        fun clearSelectedChannel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(BaseApplication.roosterApplicationComponent)

        mUserUidParam = arguments?.getString(ARG_USER_UID_PARAM)

        //        Intent musicPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        //        musicPickerIntent.setType("audio/*");
        //        startActivityForResult(musicPickerIntent, AUDIO_GET_REQUEST);
    }

    override fun onPause() {
        super.onPause()

        // Persist channels for seamless loading
        jsonPersistence.newAlarmChannelRoosters = channelRoosters
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = initiate(inflater, R.layout.fragment_new_alarm_step2, container, false)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true)
        mAdapter = ChannelsListAdapter(channelRoosters, activity)

        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(AppContext)
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.adapter = mAdapter

        if (jsonPersistence.newAlarmChannelRoosters?.isEmpty() == false) {
            channelRoosters.addAll(jsonPersistence.newAlarmChannelRoosters)
            mAdapter?.notifyDataSetChanged()
        }

        ChannelManager.onFlagChannelManagerDataListener = object : ChannelManager.Companion.OnFlagChannelManagerDataListener {
            override fun onChannelRoosterDataChanged(freshChannelRoosters: ArrayList<ChannelRooster>) {
                channelRoosters.clear()
                channelRoosters.addAll(freshChannelRoosters)
            }

            override fun onSyncFinished() {
                mListener?.configureAlarmDetails() //this is only relevant for alarms being edited
                mAdapter?.notifyDataSetChanged()
            }
        }
        channelManager.refreshChannelData(channelRoosters)

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val mediaPlayer = MediaPlayer()
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                AUDIO_GET_REQUEST -> if (data != null) {
                    val audioUri = data.data
                    if (audioUri != null) {

                        val customFilePath = File(context!!.filesDir, "Media")
                        val timeStamp = DateFormat.getDateTimeInstance().format(Date())
                        val customFileName = "Rooster_Custom_Audio_" + RoosterUtils.createRandomUID(5) + "_" + timeStamp + ".3gp"
                        val customFile = File(customFilePath, customFileName)

                        if (customFile.parentFile.exists() || customFile.parentFile.mkdirs()) {

                            try {
                                //Copy file contents to new public file
                                FileUtils.copyFromStream(context!!.contentResolver.openInputStream(audioUri), customFile)

                                //If file is valid, open a share dialog
                                //                                  && shareFile.length() < Constants.MAX_ROOSTER_FILE_SIZE
                                if (customFile.isFile && validMimeType(customFile)) {
                                    //Uri shareFileUri = FileProvider.getUriForFile(this, "com.roostermornings.android.fileprovider", shareFile);
                                    //Send audio file to friends selection activity
                                    //                                        Intent intent = new Intent(SplashActivity.this, NewAudioFriendsActivity.class);
                                    //                                        Bundle bun = new Bundle();
                                    //                                        bun.putString(Constants.EXTRA_LOCAL_FILE_STRING, shareFile.getPath());
                                    //                                        intent.putExtras(bun);
                                    //                                        startActivity(intent);

                                    try {
                                        mediaPlayer.setDataSource(customFile.absolutePath)
                                        mediaPlayer.setOnPreparedListener { mediaPlayer -> mediaPlayer.start() }
                                        mediaPlayer.prepareAsync()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }

                                    return
                                } else {
                                    Toaster.makeToast(context, "File is greater than 8 MB, or is corrupt.", Toast.LENGTH_LONG)
                                    return
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                                Toaster.makeToast(context, "Error loading file.", Toast.LENGTH_LONG)
                                return
                            }

                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun validMimeType(audioFile: File?): Boolean {
        if (audioFile != null) {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(audioFile.path)
            val mimeTypeStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (mimeTypeStr != null && (mimeTypeStr.contains("audio/mpeg")
                    || mimeTypeStr.contains("audio/mp4")
                    || mimeTypeStr.contains("audio/3gpp"))) {
                return true
            } else if (mimeTypeStr != null) {
                Toaster.makeToast(context, "Invalid audio mime-type " + mimeTypeStr, Toast.LENGTH_SHORT)
            }
        }
        return false
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        inject(BaseApplication.roosterApplicationComponent)

        if (context is IAlarmSetListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement IAlarmSetListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun setMenuVisibility(visible: Boolean) {
        super.setMenuVisibility(visible)
        if (visible) {
            mListener?.setNextButtonCaption(getString(R.string.save))
        }
    }

    fun selectAlarmChannel() {
        val alarm = mListener?.alarmDetails ?: Alarm()
        val alarmChannel: AlarmChannel? = alarm.channel

        // If editing alarm, choose original channel, else choose selected channel from persistence
        channelRoosters.firstOrNull { alarmChannel?.id == it.channel_uid }?.let {
            // Clear all selected channels
            channelRoosters.forEach { it.isSelected = false }
            it.isSelected = true

            alarm.channel.id = it.channel_uid
            alarm.channel.name = it.name

            return
        } ?: channelRoosters.firstOrNull { it.isSelected }?.let {
            // Clear all selected channels
            channelRoosters.forEach { it.isSelected = false }
            it.isSelected = true

            alarm.channel.id = it.channel_uid
            alarm.channel.name = it.name

            return
        }
    }

    companion object {

        private val AUDIO_GET_REQUEST = 10110

        private val ARG_USER_UID_PARAM = "user_uid_param"
        val TAG = NewAlarmFragment2::class.java.simpleName

        fun newInstance(param1: String?): NewAlarmFragment2 {
            val fragment = NewAlarmFragment2()
            val args = Bundle()
            args.putString(ARG_USER_UID_PARAM, param1)
            fragment.arguments = args
            return fragment
        }
    }
}
