package com.roostermornings.android.onboarding.channel_demo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import com.roostermornings.android.R
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.support.v4.content.res.ResourcesCompat
import android.transition.TransitionInflater
import android.widget.SeekBar
import butterknife.OnClick
import com.roostermornings.android.domain.local.OnboardingJourneyEvent
import com.roostermornings.android.firebase.UserMetrics
import com.roostermornings.android.onboarding.CustomCommandInterface
import com.roostermornings.android.onboarding.HostInterface
import com.roostermornings.android.onboarding.InterfaceCommands
import com.roostermornings.android.util.FileUtils
import com.roostermornings.android.util.RoosterUtils
import kotlinx.android.synthetic.main.onboarding_audio_demo.view.*
import java.io.File


class ChannelDemoOverlayFragment : Fragment() {
    private var mHostInterface: HostInterface? = null
    private var mCustomCommandInterface: CustomCommandInterface? = null

    private var mMediaPlayer: MediaPlayer = MediaPlayer()
    private val mHandler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    companion object {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */

        private val ARG_UID_STRING = "ARG_UID_STRING"
        private val ARG_TITLE_STRING = "ARG_TITLE_STRING"
        private val ARG_DESCRIPTION_STRING = "ARG_DESCRIPTION_STRING"
        private val ARG_DRAWABLE_ID = "ARG_DRAWABLE_ID"
        private val ARG_MEDIA = "ARG_MEDIA"

        fun newInstance(uid: String, title: String, description: String, imageID: Int, media: Int): Fragment {
            val fragment = ChannelDemoOverlayFragment()
            val args = Bundle()
            args.putString(ARG_UID_STRING, uid)
            args.putString(ARG_TITLE_STRING, title)
            args.putString(ARG_DESCRIPTION_STRING, description)
            args.putInt(ARG_DRAWABLE_ID, imageID)
            args.putInt(ARG_MEDIA, media)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mHostInterface = context as HostInterface
            mCustomCommandInterface = context as CustomCommandInterface
        } catch (e: ClassCastException) {
            TODO("Interface not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(RoosterUtils.hasLollipop()) {
            sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.onboarding_audio_demo, container, false)
        ButterKnife.bind(this, view)

        mCustomCommandInterface?.onCustomCommand(InterfaceCommands.Companion.Command.HIDE_FAB)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val media = arguments?.getInt(ARG_MEDIA)

        view.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.black_overlay_a40, null))

        val drawable = ResourcesCompat.getDrawable(resources, arguments?.getInt(ARG_DRAWABLE_ID)?:-1, null)
        val color = ColorDrawable(ResourcesCompat.getColor(resources, R.color.white, null))

        val ld = LayerDrawable(arrayOf(color, drawable))
        view.audioDemoImage.setImageDrawable(ld)

        view.audioDemoText.visibility = View.VISIBLE
        view.audioDemoTitle.text = arguments?.getString(ARG_TITLE_STRING)
        view.audioDemoDescription.text = arguments?.getString(ARG_DESCRIPTION_STRING)

        view.demoAudioSeekBar.visibility = View.VISIBLE
        view.demoAudioSeekBar.progressDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        view.demoAudioSeekBar.thumb.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        //Set the maximum value to the audio item length
        view.demoAudioSeekBar.max = 0
        view.demoAudioSeekBar.max = getMediaLength(media?:0)/1000
        //Listen for seekbar progress updates, and mediaPlayer.seekTo()
        view.demoAudioSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)

        //Update seekbar on UI thread
        runnable = Runnable {
            if (view.demoAudioSeekBar != null && mMediaPlayer.isPlaying) {
                val mCurrentPosition = mMediaPlayer.currentPosition / 1000
                view.demoAudioSeekBar.progress = mCurrentPosition

                if (view.demoAudioSeekBar.max >= mCurrentPosition) {
                    mHandler.removeCallbacks(runnable)
                    mHandler.postDelayed(runnable, 1000)
                }
            }
        }

        if(mMediaPlayer.isPlaying) mMediaPlayer.stop()
        mMediaPlayer = MediaPlayer.create(context, arguments?.getInt(ARG_MEDIA)?:-1)

        mMediaPlayer.setOnCompletionListener {
            activity?.supportFragmentManager?.popBackStackImmediate()
        }

        view.playPause?.isSelected = true

        val mediaOnClickListener = View.OnClickListener {
            view.playPause?.isSelected = !mMediaPlayer.isPlaying
            if(mMediaPlayer.isPlaying) {
                pauseMedia()
            } else {
                playMedia()
            }
        }
        view.playPause.setOnClickListener(mediaOnClickListener)
        view.audioDemoImage.setOnClickListener(mediaOnClickListener)
        playMedia()
    }

    private val onSeekBarChangeListener = object: SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {
            UserMetrics.logOnboardingEvent(
                    OnboardingJourneyEvent(
                            subject = "Channel Demo UI",
                            content_uid = arguments?.getString(ARG_UID_STRING),
                            target_time = mMediaPlayer.currentPosition / 1000)
                            .setType(OnboardingJourneyEvent.Companion.Event.SEEK_TRACK))
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                if(!mMediaPlayer.isPlaying) {
                    mHandler.removeCallbacks(runnable)
                    mHandler.postDelayed(runnable, 1000)
                }

                mMediaPlayer.seekTo(progress * 1000)
                mMediaPlayer.start()
                view?.playPause?.isSelected = true
            }
        }
    }

    private fun playMedia() {
        runnable?.let { mHandler.removeCallbacks(runnable) }
        mMediaPlayer.start()
        mHandler.postDelayed(runnable, 1000)
    }

    private fun pauseMedia() {
        runnable?.let { mHandler.removeCallbacks(runnable) }
        if(mMediaPlayer.isPlaying) mMediaPlayer.pause()
    }

    private fun stopMedia() {
        runnable?.let { mHandler.removeCallbacks(runnable) }
        if(mMediaPlayer.isPlaying) mMediaPlayer.stop()
    }

    private fun getMediaLength(media: Int): Int {
        val context = context ?: return -1

        val file = File(context.cacheDir, "channelDemo.mp3")
        FileUtils.copyFromStream(resources.openRawResource(media), file)

        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(file.path)
        val durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        return Integer.parseInt(durationStr)
    }

    @OnClick(R.id.onboardingAudioDemoLayout)
    fun onClickOverlay() {
        activity?.supportFragmentManager?.popBackStackImmediate()
    }

    override fun onPause() {
        super.onPause()
        val finishPosition = mMediaPlayer.currentPosition/1000
        // Listen event triggered in onPause, with length as current position
        UserMetrics.logOnboardingEvent(
                OnboardingJourneyEvent(
                        subject = "Channel Demo UI",
                        content_uid = arguments?.getString(ARG_UID_STRING),
                        length = finishPosition)
                        .setType(OnboardingJourneyEvent.Companion.Event.LISTEN))

        stopMedia()
        mCustomCommandInterface?.onCustomCommand(InterfaceCommands.Companion.Command.SHOW_FAB)
    }
}