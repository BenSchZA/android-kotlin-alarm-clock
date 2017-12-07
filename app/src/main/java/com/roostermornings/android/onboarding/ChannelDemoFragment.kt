package com.roostermornings.android.onboarding

import android.annotation.TargetApi
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import butterknife.OnClick
import com.roostermornings.android.R
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.fragment.base.BaseFragment
import com.roostermornings.android.util.RoosterUtils
import kotlinx.android.synthetic.main.fragment_onboarding_channel_demo.view.*
import java.text.FieldPosition
import android.support.v7.widget.GridLayoutManager



class ChannelDemoFragment : BaseFragment(), ChannelDemoInterface, FragmentInterface {
    private var mHostInterface: HostInterface? = null
    private var mShowcaseInterface: ShowcaseInterface? = null
    private var mShowcaseHandler: Handler = Handler()

    private val imageIDs = intArrayOf(
            R.drawable.onboarding_channel_demo_inspiration,
            R.drawable.onboarding_channel_demo_martin_luther,
            R.drawable.onboarding_channel_demo_cross,
            R.drawable.onboarding_channel_demo_meditation,
            R.drawable.onboarding_channel_demo_purple_breasted_roller,
            R.drawable.onboarding_channel_demo_sports,
            R.drawable.onboarding_channel_demo_nature_sounds,
            R.drawable.onboarding_channel_demo_tanabbah,
            R.drawable.onboarding_channel_demo_trevor_noah,
            R.drawable.onboarding_channel_demo_tdih)

    private val imageTitles = arrayOf(
            "Inspiration",
            "Famous Speeches",
            "Christian",
            "Meditation",
            "Bird Calls",
            "Sport News",
            "Nature Sounds",
            "Tanabbah",
            "Comedy",
            "This Day in History")

    private var mDataSet = ArrayList<ChannelDemoItem>()
    private var mAdapter = ChannelDemoListAdapter(mDataSet, this@ChannelDemoFragment)

    companion object {

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */

        fun newInstance(): ChannelDemoFragment {
            val fragment = ChannelDemoFragment()
//            val args = Bundle()
//            args.putParcelable(ARG_WINDOW_SIZE, windowSize)
//            fragment.arguments = args
            return fragment
        }
    }

    init {
        imageIDs.forEachIndexed { index, image ->
            mDataSet.add(index, ChannelDemoItem(image, imageTitles[index]))
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
        return initiate(inflater, R.layout.fragment_onboarding_channel_demo, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view?.let {
            val recyclerView = view.gridRecylerView as RecyclerView
            val layoutManager = GridLayoutManager(context, 2)

            // Create a custom SpanSizeLookup where the first item spans both columns
            layoutManager.spanSizeLookup = (object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when(position) {
                                0 -> 2
                                3 -> 2
                                6 -> 2
                                9 -> 2
                                else -> 1
                            }
                        }
                    })

            //layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = mAdapter

            try {
                mShowcaseInterface = mAdapter
            } catch (e: ClassCastException) {
                TODO("HostInterface not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    @OnClick(R.id.navigationFAB)
    fun onClickFAB() {
        mHostInterface?.scrollViewPager(View.FOCUS_RIGHT)
    }

    private var previousOverlayFragment: Fragment? = null

    override fun performChannelImageTransition(title: String, drawableID: Int, imageView: ImageView, media: Int) {
        val overlayFragment = ChannelDemoOverlayFragment.newInstance(title, drawableID, media)

        // Note that we need the API version check here because the actual transition classes (e.g. Fade)
        // are not in the support library and are only available in API 21+. The methods we are calling on the Fragment
        // ARE available in the support library (though they don't do anything on API < 21)
        if (RoosterUtils.hasLollipop()) {
            //overlayFragment.sharedElementEnterTransition = DetailsTransition()
            //overlayFragment.enterTransition = Fade()
            //exitTransition = Fade()
            //overlayFragment.sharedElementReturnTransition = DetailsTransition()
        }

        if(previousOverlayFragment != null) activity.supportFragmentManager.popBackStack()

        activity.supportFragmentManager
                .beginTransaction()
                .addSharedElement(imageView, getString(R.string.onboarding_image_transition))
                .replace(R.id.fragmentOnboardingChannelDemo, overlayFragment)
                .addToBackStack(null)
                .commit()

        previousOverlayFragment = overlayFragment
    }

    override fun fragmentVisible(position: Int) {
        when(position) {
            1 -> {
                mShowcaseInterface?.startShowCase(mShowcaseHandler, activity)
            }
            else -> {
                mShowcaseHandler.removeCallbacksAndMessages(null)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mShowcaseHandler.removeCallbacksAndMessages(null)
    }
}

@TargetApi(21)
class DetailsTransition : android.transition.TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(android.transition.ChangeBounds()).
                addTransition(android.transition.ChangeTransform()).
                addTransition(android.transition.ChangeImageTransform())
    }
}

class ChannelDemoItem(var imageID: Int = -1, var title: String = "")