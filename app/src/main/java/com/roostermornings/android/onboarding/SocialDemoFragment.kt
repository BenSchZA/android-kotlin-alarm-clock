package com.roostermornings.android.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.roostermornings.android.R
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.fragment.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_onboarding_social_demo.view.*
import kotlinx.android.synthetic.main.onboarding_audio_demo.view.*
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import butterknife.OnClick
import com.roostermornings.android.activity.MyAlarmsFragmentActivity
import com.roostermornings.android.firebase.FA
import kotlinx.android.synthetic.main.fragment_onboarding_social_demo.*


/**
 * Created by bscholtz on 2017/12/07.
 */
class SocialDemoFragment: BaseFragment() {
    private var mHostInterface: HostInterface? = null

    private var mMediaPlayer = MediaPlayer()
    private var mPlaying  = false

    private val imageDrawables = intArrayOf(
            R.drawable.rooster_wakeup,
            R.drawable.rooster_channels,
            R.drawable.logo_icon)
    private val imageTitles = arrayOf(
            "Uncle Joe",
            "Friendly Friend",
            "Sassy Sister")
    private val imageDescriptions = arrayOf(
            "Telling Dad jokes",
            "Bit of banter",
            "Lots of love from Greece")

    companion object {
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */

        fun newInstance(): SocialDemoFragment {
            val fragment = SocialDemoFragment()
//            val args = Bundle()
//            args.putParcelable(ARG_WINDOW_SIZE, windowSize)
//            fragment.arguments = args
            return fragment
        }
    }

    override fun inject(component: RoosterApplicationComponent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mHostInterface = context as HostInterface
        } catch (e: ClassCastException) {
            TODO("HostInterface not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return initiate(inflater, R.layout.fragment_onboarding_social_demo, container, false)
    }

    @OnClick(R.id.button_proceed)
    fun onClickProceed() {
        val intent = Intent(context, MyAlarmsFragmentActivity::class.java)
        startActivity(intent)
        FA.Log(FA.Event.onboarding_first_entry::class.java, null, null)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view?.let {

            view.viewFlipper.isAutoStart = true
            view.viewFlipper.setFlipInterval(3000)

            imageDrawables.forEachIndexed { index, drawableID ->
                val drawablePerson = ResourcesCompat.getDrawable(resources, drawableID, null)
                val socialTitle = imageTitles[index]
                val socialDescription = imageDescriptions[index]

                val vfElement = View.inflate(context, R.layout.onboarding_audio_demo, null)

                val color = ColorDrawable(ResourcesCompat.getColor(resources, R.color.white, null))
                val ld = LayerDrawable(arrayOf(color, drawablePerson))
                vfElement.audioDemoImage.setImageDrawable(ld)
                vfElement.socialDemoPerson.text = socialTitle
                vfElement.socialDemoMessage.text = socialDescription
                vfElement.socialDemoText.visibility = View.VISIBLE

                view.viewFlipper.addView(vfElement)
            }
            view.viewFlipper.startFlipping()

            for(childIndex in 0 until view.viewFlipper.childCount) {
                view.viewFlipper.getChildAt(childIndex).setOnClickListener {
                    view.viewFlipper?.stopFlipping()
                    val childView = view.viewFlipper.getChildAt(childIndex)

                    if(mPlaying) {
                        mMediaPlayer.stop()
                        mPlaying = false
                        childView.playPause.isSelected = false
                        view.viewFlipper?.startFlipping()
                    } else {
                        mMediaPlayer = MediaPlayer.create(context, R.raw.onboarding_the_shins)
                        childView.playPause.isSelected = true
                        mMediaPlayer.start()
                        mPlaying = true
                    }

                    mMediaPlayer.setOnCompletionListener {
                        childView.playPause.isSelected = false
                        view.viewFlipper?.startFlipping()
                        mPlaying = false
                    }
                }
            }
        }
    }
}