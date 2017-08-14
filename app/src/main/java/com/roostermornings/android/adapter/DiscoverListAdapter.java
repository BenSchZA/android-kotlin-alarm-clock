/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.DiscoverFragmentActivity;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DiscoverListAdapter extends RecyclerView.Adapter<DiscoverListAdapter.ViewHolder> implements Filterable {
    private ArrayList<ChannelRooster> mDataset;
    private Activity mActivity;
    private Context context;

    private DiscoverAudioSampleInterface discoverAudioSampleInterface;
    public interface DiscoverAudioSampleInterface {
        void streamChannelRooster(final ChannelRooster channelRooster);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.cardview_channel_name)
        TextView txtChannelName;
        @BindView(R.id.card_view_channel_image)
        ImageView imgChannelImage;
        @BindView(R.id.card_view_discover)
        CardView cardViewChannel;
        @BindView(R.id.imageProgressBar)
        ProgressBar imageProgressBar;
        @BindView(R.id.audioProgressBar)
        ProgressBar audioProgressBar;
        @BindView(R.id.audio_listen)
        ImageButton listenImageButton;
        @BindView(R.id.cardview_channel_info)
        TextView channelInfo;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public void add(int position, ChannelRooster item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void refreshAll(ArrayList<ChannelRooster> myDataset) {
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public DiscoverListAdapter(ArrayList<ChannelRooster> mDataset, Activity mActivity) {
        this.mDataset = mDataset;
        this.mActivity = mActivity;
        if(mActivity instanceof DiscoverFragmentActivity) {
            discoverAudioSampleInterface = (DiscoverAudioSampleInterface) mActivity;
        }
    }

    public DiscoverListAdapter() {

    }

    // Create new views (invoked by the layout manager)
    @Override
    public DiscoverListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        context = parent.getContext();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_discover, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        final ChannelRooster channelRooster = mDataset.get(position);
        // - replace the contents of the view with that element
        if (channelRooster.isPlaying()) {
            holder.listenImageButton.setBackgroundResource(R.drawable.rooster_new_audio_pause_button);
        } else {
            holder.listenImageButton.setBackgroundResource(R.drawable.rooster_audio_play_button);
        }

        if(channelRooster.isDownloading()) {
            holder.audioProgressBar.setVisibility(View.VISIBLE);
        } else {
            holder.audioProgressBar.setVisibility(View.GONE);
        }

        holder.cardViewChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverAudioSampleInterface.streamChannelRooster(channelRooster);
                FA.Log(FA.Event.explore_channel_rooster_played.class, FA.Event.explore_channel_rooster_played.Param.channel_title, channelRooster.getChannel_uid());
            }
        });

        holder.listenImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverAudioSampleInterface.streamChannelRooster(channelRooster);
                FA.Log(FA.Event.explore_channel_rooster_played.class, FA.Event.explore_channel_rooster_played.Param.channel_title, channelRooster.getChannel_uid());
            }
        });

        holder.txtChannelName.setText(channelRooster.getName());

        holder.channelInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new MaterialDialog.Builder(mActivity)
                        .title(channelRooster.getName())
                        .content(channelRooster.getDescription())
                        .positiveText(R.string.ok)
                        .negativeText("")
                        .show();

                FA.Log(FA.Event.channel_info_viewed.class, FA.Event.channel_info_viewed.Param.channel_title, channelRooster.getChannel_uid());
            }
        });

        try {
            Picasso.with(context).load(channelRooster.getPhoto())
                    .fit()
                    .centerCrop()
                    .into(holder.imgChannelImage, new Callback() {
                @Override
                public void onSuccess() {
                    holder.imageProgressBar.setVisibility(View.GONE);
                    holder.imgChannelImage.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError() {

                }
            });
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public Filter getFilter() {

        final Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                mDataset = (ArrayList<ChannelRooster>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<ChannelRooster> filteredContacts = new ArrayList<>();

                //Perform your search here using the search constraint string
                constraint = constraint.toString().toLowerCase();
                for (int i = 0; i < mDataset.size(); i++) {
                    String contactData = mDataset.get(i).getName();
                    if (contactData.toLowerCase().contains(constraint.toString()))  {
                        filteredContacts.add(mDataset.get(i));
                    }
                }

                results.count = filteredContacts.size();
                results.values = filteredContacts;

                return results;
            }
        };

        return filter;
    }

}