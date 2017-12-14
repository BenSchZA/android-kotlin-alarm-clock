package com.roostermornings.android.onboarding

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.roostermornings.android.R
import kotlinx.android.synthetic.main.cardview_onboarding_channel_demo.view.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.STATE_DISMISSED
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt.STATE_FOCAL_PRESSED
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal


class ChannelDemoListAdapter(
        private val mDataset: ArrayList<ChannelDemoFragment.Companion.ChannelDemoItem>,
        private val mFragment: Fragment) :
        RecyclerView.Adapter<ChannelDemoListAdapter.ViewHolder>(),
        ShowcaseInterface {

    private var context: Context? = null

    private var showcaseChannelCardView: CardView? = null
    private var mShowcase: MaterialTapTargetPrompt? = null

    private var channelDemoInterface: ChannelDemoInterface? = null

    private var showCaseSeen: Boolean = false

    // Provide a reference to the views for each data item
    // Complex data items may need more than one activityContentView per item, and
    // you provide access to all the views for a data item in a activityContentView holder
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @BindView(R.id.cardView)
        lateinit internal var cardView: CardView
        @BindView(R.id.image)
        lateinit internal var image: ImageView
        @BindView(R.id.title)
        lateinit internal var title: TextView

        init {
            ButterKnife.bind(this, view)
        }
    }

    fun add(position: Int, item: ChannelDemoFragment.Companion.ChannelDemoItem) {
        mDataset.add(position, item)
        notifyItemInserted(position)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        context = parent.context
        // create a new activityContentView
        val v = LayoutInflater.from(parent.context).inflate(R.layout.cardview_onboarding_channel_demo, parent, false)

        if (mFragment is ChannelDemoFragment) {
            channelDemoInterface = mFragment
        }

        // set the activityContentView's size, margins, paddings and layout parameters
        return ViewHolder(v)
    }

    // Replace the contents of a activityContentView (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mDataset[position]
        holder.image.setImageResource(item.imageID)
        holder.image.tag = item.imageID
        holder.title.text = item.title

        if(item.imageID == R.drawable.onboarding_channel_demo_purple_breasted_roller) {
//            showcaseChannelCardView = holder.cardView
        }
        if(position == 1) showcaseChannelCardView = holder.cardView

        //TODO: descriptions
        holder.cardView.setOnClickListener {
            channelDemoInterface?.performChannelImageTransition(it.title.text.toString(), "Sample channel demo description", it.image.tag as Int, holder.image, R.raw.onboarding_the_shins)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mDataset.size
    }

    override fun startShowCase(handler: Handler, activity: Activity) {
//        Handler().postDelayed({
//            val firstMileManager = FirstMileManager()
//            firstMileManager.createShowcase(activity, ViewTarget(showcaseChannelCardView), 1)
//        }, 500)
        if(!showCaseSeen) {
            handler.postDelayed({
                mShowcase = MaterialTapTargetPrompt.Builder(activity)
                        .setTarget(showcaseChannelCardView)
                        .setPrimaryText("Test what works for you")
                        .setSecondaryText("Design your morning...")
                        .setPrimaryTextSize(R.dimen.text_xxlarge)
                        .setSecondaryTextSize(R.dimen.text_xxlarge)
                        .setIdleAnimationEnabled(false)
                        .setFocalRadius(300f)
                        .setPrimaryTextGravity(Gravity.BOTTOM)
                        .setSecondaryTextGravity(Gravity.BOTTOM)
                        .setPromptFocal(RectanglePromptFocal())
                        .setBackgroundColour(ResourcesCompat.getColor(activity.resources, R.color.black_overlay_a70, null))
                        .setPromptStateChangeListener({ _, state ->
                            when(state) {
                                STATE_DISMISSED, STATE_FOCAL_PRESSED -> {
                                    // User has pressed the prompt target
                                    showCaseSeen = true
                                }
                            }
                        })
                        .show()
            }, 500)
        }
    }

    override fun dismissShowcase() {
        mShowcase?.dismiss()
    }
}