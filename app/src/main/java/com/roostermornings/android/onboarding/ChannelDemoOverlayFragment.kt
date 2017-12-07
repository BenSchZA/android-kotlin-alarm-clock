package com.roostermornings.android.onboarding

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import com.roostermornings.android.R
import kotlinx.android.synthetic.main.onboarding_channel_overlay.view.*
import android.graphics.PorterDuff
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.transition.TransitionInflater
import android.widget.RelativeLayout
import android.widget.SeekBar
import butterknife.BindView
import butterknife.OnClick
import com.roostermornings.android.util.FileUtils
import com.roostermornings.android.util.RoosterUtils
import java.io.File


class ChannelDemoOverlayFragment : Fragment() {
    private var mHostInterface: HostInterface? = null

    private var mMediaPlayer: MediaPlayer = MediaPlayer()
    private val mHandler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    companion object {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */

        private val ARG_DRAWABLE_ID = "ARG_DRAWABLE_ID"
        private val ARG_TITLE_STRING = "ARG_TITLE_STRING"
        private val ARG_MEDIA = "ARG_MEDIA"

        fun newInstance(title: String, imageID: Int, media: Int): Fragment {
            val fragment = ChannelDemoOverlayFragment()
            val args = Bundle()
            args.putString(ARG_TITLE_STRING, title)
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
        } catch (e: ClassCastException) {
            TODO("HostInterface not implemented") //To change body of created functions use File | Settings | File Templates.
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
        val view = inflater.inflate(R.layout.onboarding_channel_overlay, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val media = arguments.getInt(ARG_MEDIA)
        view?.let{
            view.image.setImageResource(arguments.getInt(ARG_DRAWABLE_ID))
            view.title.text = arguments.getString(ARG_TITLE_STRING)

            view.seekbar.progressDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            view.seekbar.thumb.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

            //Set the maximum value to the audio item length
            view.seekbar.max = 0
            view.seekbar.max = getMediaLength(media)/1000
            //Listen for seekbar progress updates, and mediaPlayer.seekTo()
            view.seekbar.setOnSeekBarChangeListener(onSeekBarChangeListener)

            //Update seekbar on UI thread
            runnable = Runnable {
                if (view.seekbar != null && mMediaPlayer.isPlaying) {
                    val mCurrentPosition = mMediaPlayer.currentPosition / 1000
                    view.seekbar.progress = mCurrentPosition

                    if (view.seekbar.max >= mCurrentPosition) {
                        mHandler.removeCallbacks(runnable)
                        mHandler.postDelayed(runnable, 1000)
                    }
                }
            }
        }

        if(mMediaPlayer.isPlaying) mMediaPlayer.stop()
        mMediaPlayer = MediaPlayer.create(context, arguments.getInt(ARG_MEDIA))
        mMediaPlayer.start()
        mHandler.removeCallbacks(runnable)
        mHandler.postDelayed(runnable, 1000)
    }

    private val onSeekBarChangeListener = object: SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                if(!mMediaPlayer.isPlaying) {
                    mHandler.removeCallbacks(runnable)
                    mHandler.postDelayed(runnable, 1000)
                }
                mMediaPlayer.seekTo(progress * 1000)
                mMediaPlayer.start()
            }
        }
    }

    private fun getMediaLength(media: Int): Int {
        val file = File(context.cacheDir, "channelDemo.mp3")
        FileUtils.copyFromStream(resources.openRawResource(media), file)

        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(file.path)
        val durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        return Integer.parseInt(durationStr)
    }

    @OnClick(R.id.onboardingChannelOverlay)
    fun onClickOverlay() {
        activity.supportFragmentManager.popBackStackImmediate()
    }

    override fun onPause() {
        super.onPause()
        if(mMediaPlayer.isPlaying) mMediaPlayer.stop()
        runnable?.let {
            mHandler.removeCallbacks(runnable)
        }
    }
}