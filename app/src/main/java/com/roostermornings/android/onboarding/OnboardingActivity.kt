package com.roostermornings.android.onboarding

import android.graphics.Point
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.*
import android.widget.ProgressBar
import butterknife.BindView
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.local.OnboardingJourneyEvent
import com.roostermornings.android.firebase.UserMetrics
import com.roostermornings.android.onboarding.channel_demo.ChannelDemoFragment
import com.roostermornings.android.onboarding.social_demo.SocialDemoFragment
import com.roostermornings.android.onboarding.social_demo.SocialHookFragment
import com.roostermornings.android.util.RoosterUtils

import kotlinx.android.synthetic.main.activity_onboarding.*

class OnboardingActivity: BaseActivity(), HostInterface, CustomCommandInterface {

    companion object {
        private val NUMBER_OF_ONBOARDING_PAGES = Page.values().size
        // -1 for first page
        private val NUMBER_OF_PROGRESS_SEGMENTS = NUMBER_OF_ONBOARDING_PAGES - 1

        enum class Page {
            INTRO, CHANNEL_DEMO, SIGN_IN, SOCIAL_HOOK, SOCIAL_DEMO
        }
    }

    private var hasPassedSignIn = false

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    private var mFragmentInterface: FragmentInterface? = null
    private var mCustomCommandInterface: CustomCommandInterface? = null

    @BindView(R.id.progressBar)
    lateinit var progressBar: ProgressBar

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BaseApplication.roosterApplicationComponent.inject(this)

        // Set display to fullscreen
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // Inflate activityContentView and bind children views
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
                mFragmentInterface?.fragmentVisible(position)

                // Log onboarding journey activityContentView event
                when(position) {
                    Page.INTRO.ordinal -> {
                        UserMetrics.logOnboardingEvent(
                                OnboardingJourneyEvent(subject = "Intro UI")
                                        .setType(OnboardingJourneyEvent.Companion.Event.VIEW))
                    }
                    Page.CHANNEL_DEMO.ordinal -> {
                        UserMetrics.logOnboardingEvent(
                                OnboardingJourneyEvent(subject = "Channel Demo UI")
                                        .setType(OnboardingJourneyEvent.Companion.Event.VIEW))
                        container.isRightScrollEnabled = true
                    }
                    Page.SIGN_IN.ordinal -> {
                        UserMetrics.logOnboardingEvent(
                                OnboardingJourneyEvent(subject = "Sign-In UI")
                                        .setType(OnboardingJourneyEvent.Companion.Event.VIEW))
                            container.isRightScrollEnabled = authManager.isUserSignedIn() || hasPassedSignIn
                    }
                    Page.SOCIAL_HOOK.ordinal -> {
                        UserMetrics.logOnboardingEvent(
                                OnboardingJourneyEvent(subject = "Social Hook UI")
                                        .setType(OnboardingJourneyEvent.Companion.Event.VIEW))
                        container.isRightScrollEnabled = true
                    }
                    Page.SOCIAL_DEMO.ordinal -> {
                        UserMetrics.logOnboardingEvent(
                                OnboardingJourneyEvent(subject = "Social Demo UI")
                                        .setType(OnboardingJourneyEvent.Companion.Event.VIEW))
                    }
                    else -> {}
                }
            }
        })
    }

    override fun setOnboardingProgress(pageNumber: Int) {
        val progress = (pageNumber/ NUMBER_OF_PROGRESS_SEGMENTS.toFloat() *100).toInt()
        if(RoosterUtils.hasNougat()) {
            progressBar.setProgress(progress, true)
        } else {
            progressBar.progress = progress
        }
    }

    override fun scrollViewPager(direction: Int) {
        container.arrowScroll(direction)
    }

    override fun onCustomCommand(command: InterfaceCommands.Companion.Command) {
        mCustomCommandInterface?.onCustomCommand(command)
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
                Page.INTRO.ordinal -> {
                    val point = Point()
                    windowManager?.defaultDisplay?.getSize(point)
                    IntroFragment.newInstance(point)
                }
                Page.CHANNEL_DEMO.ordinal -> {
                    val fragment = ChannelDemoFragment.newInstance()
                    mFragmentInterface = fragment
                    mCustomCommandInterface = fragment
                    fragment
                }
                Page.SIGN_IN.ordinal -> {
                    ProfileCreationFragment.newInstance(ProfileCreationFragment.Companion.Source.ONBOARDING)
                }
                Page.SOCIAL_HOOK.ordinal -> {
                    SocialHookFragment.newInstance()
                }
                Page.SOCIAL_DEMO.ordinal -> {
                    SocialDemoFragment.newInstance()
                }
                else -> {
                    val point = Point()
                    windowManager?.defaultDisplay?.getSize(point)
                    IntroFragment.newInstance(point)
                }
            }
        }

        override fun getCount(): Int {
            // Show X total pages.
            return NUMBER_OF_ONBOARDING_PAGES
        }
    }

//    override fun onWindowFocusChanged(hasFocus:Boolean) {
//        super.onWindowFocusChanged(hasFocus)
//        if (hasFocus) {
//            var uiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            or View.SYSTEM_UI_FLAG_FULLSCREEN)
//            if(RoosterUtils.hasKitKat()) {
//                uiVisibility = uiVisibility or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//            }
//            window.decorView.systemUiVisibility = uiVisibility
//        }
//    }
}

