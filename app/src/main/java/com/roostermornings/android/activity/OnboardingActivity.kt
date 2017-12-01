package com.roostermornings.android.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Point
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ProgressBar
import butterknife.BindView
import butterknife.OnClick
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.fragment.base.BaseFragment
import com.roostermornings.android.util.RoosterUtils

import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_onboarding_intro.*
import kotlinx.android.synthetic.main.fragment_onboarding_intro.view.*
import java.util.*
//BaseActivity()
class OnboardingActivity: BaseActivity(), HostInterface {

    companion object {
        private val NUMBER_OF_ONBOARDING_PAGES = 5.0
    }

    private var mWindowManager: WindowManager? = null

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    @BindView(R.id.progressBar)
    lateinit var progressBar: ProgressBar

    override fun inject(component: RoosterApplicationComponent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get window manager to determine screen size
        mWindowManager = windowManager

        // Set display to fullscreen
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // Inflate view and bind children views
        initialize(R.layout.activity_onboarding)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter

        // On page selected, refresh progress bar
        container.addOnPageChangeListener(object: ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setOnboardingProgress(position)
            }
        })
    }

    override fun setOnboardingProgress(pageNumber: Int) {
        val progress = (pageNumber/NUMBER_OF_ONBOARDING_PAGES*100).toInt()
        if(RoosterUtils.hasNougat()) {
            progressBar.setProgress(progress, true)
        } else {
            progressBar.progress = progress
        }
    }

    override fun scrollViewPager(direction: Int) {
        container.arrowScroll(direction)
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a OnboardingIntroFragment (defined as a static inner class below).
            return when(position) {
                1 -> {
                    val point = Point()
                    mWindowManager?.defaultDisplay?.getSize(point)
                    OnboardingIntroFragment.newInstance(point)
                }
                else -> {
                    val point = Point()
                    mWindowManager?.defaultDisplay?.getSize(point)
                    OnboardingIntroFragment.newInstance(point)
                }
            }
        }

        override fun getCount(): Int {
            // Show X total pages.
            return 3
        }
    }
}

interface HostInterface {
    fun setOnboardingProgress(pageNumber: Int)
    fun scrollViewPager(direction: Int)
}

class OnboardingIntroFragment : BaseFragment() {

    companion object {
        private val CLOUD_MAX_WIDTH = 300
        private val CLOUD_DURATION = 30000L
        private val LOGO_DURATION = 2000L
        private val HOOK_TEXT_DURATION = 500L

        private val MIN_RANDOM = -2000L
        private val MAX_RANDOM = 2000L

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        private val ARG_WINDOW_SIZE = "ARG_WINDOW_SIZE"

        fun newInstance(windowSize: Point): OnboardingIntroFragment {
            val fragment = OnboardingIntroFragment()
            val args = Bundle()
            args.putParcelable(ARG_WINDOW_SIZE, windowSize)
            fragment.arguments = args
            return fragment
        }
    }

    private val randomOffset = fun(): Long {
        return (MIN_RANDOM + (Random().nextDouble()*(MAX_RANDOM - MIN_RANDOM))).toLong()
    }

    @OnClick(R.id.button)
    fun onClickContinueButton() {
        mHostInterface?.scrollViewPager(View.FOCUS_RIGHT)
    }

    private var mHostInterface: HostInterface? = null

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
        return initiate(inflater, R.layout.fragment_onboarding_intro, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view?.let {

            val windowSize = arguments.getParcelable(ARG_WINDOW_SIZE) as Point
            val windowWidth = windowSize.x.toFloat()
            val windowHeight = windowSize.y.toFloat()

            val logoLocation = IntArray(2)
            view.logo.getLocationInWindow(logoLocation)

            val logoAnimation = TranslateAnimation(Animation.RELATIVE_TO_SELF, 0F, Animation.RELATIVE_TO_SELF, 0F, Animation.RELATIVE_TO_PARENT, 1F, Animation.RELATIVE_TO_SELF, 0F)
            logoAnimation.duration = LOGO_DURATION
            logoAnimation.fillAfter = true

            val logoAnimationListener = object: Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) {
                    val hookTextAnimation = ObjectAnimator.ofFloat(view.hookText, "alpha", 0f, 1f)
                    hookTextAnimation.duration = HOOK_TEXT_DURATION
                    hookTextAnimation.start()
                }
                override fun onAnimationStart(p0: Animation?) {}
            }
            logoAnimation.setAnimationListener(logoAnimationListener)

            view.logo.animation = logoAnimation

            val animationCloud1 = TranslateAnimation(-(windowWidth/2 + CLOUD_MAX_WIDTH/2F), (windowWidth/2 + CLOUD_MAX_WIDTH/2F), 0F, 0F)
            animationCloud1.duration = CLOUD_DURATION + randomOffset()*8
            animationCloud1.fillAfter = true
            animationCloud1.repeatCount = -1
            animationCloud1.repeatMode = Animation.REVERSE

            val animationCloud2 = TranslateAnimation((windowWidth/2 + CLOUD_MAX_WIDTH/2F), -(windowWidth/2 + CLOUD_MAX_WIDTH/2F), 0F, 0F)
            animationCloud2.duration = CLOUD_DURATION + randomOffset()*8
            animationCloud2.fillAfter = true
            animationCloud2.repeatCount = -1
            animationCloud2.repeatMode = Animation.REVERSE
            animationCloud2.startOffset = 2000

            val animationCloud3 = TranslateAnimation(-(windowWidth/2 + CLOUD_MAX_WIDTH/2F), (windowWidth/2 + CLOUD_MAX_WIDTH/2F), 0F, 0F)
            animationCloud3.duration = CLOUD_DURATION + randomOffset()*8
            animationCloud3.fillAfter = true
            animationCloud3.repeatCount = -1
            animationCloud3.repeatMode = Animation.REVERSE
            animationCloud3.startOffset = 1000

            val animationCloud4 = TranslateAnimation((windowWidth/2 + CLOUD_MAX_WIDTH/2F), -(windowWidth/2 + CLOUD_MAX_WIDTH/2F), 0F, 0F)
            animationCloud4.duration = CLOUD_DURATION + randomOffset()*8
            animationCloud4.fillAfter = true
            animationCloud4.repeatCount = -1
            animationCloud4.repeatMode = Animation.REVERSE

            view.cloud1.animation = animationCloud1
            view.cloud2.animation = animationCloud2
            view.cloud3.animation = animationCloud3
            view.cloud4.animation = animationCloud4

            val cloudAnimationListener = object: Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {
                    p0?.startOffset = randomOffset()
                    p0?.duration = CLOUD_DURATION + randomOffset()*8
                }
                override fun onAnimationEnd(p0: Animation?) {}
                override fun onAnimationStart(p0: Animation?) {}
            }

            animationCloud1.setAnimationListener(cloudAnimationListener)
            animationCloud2.setAnimationListener(cloudAnimationListener)
            animationCloud3.setAnimationListener(cloudAnimationListener)
            animationCloud4.setAnimationListener(cloudAnimationListener)
        }
    }
}
