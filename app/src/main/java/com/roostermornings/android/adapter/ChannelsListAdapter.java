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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.roostermornings.android.R;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.domain.database.Alarm;
import com.roostermornings.android.domain.database.AlarmChannel;
import com.roostermornings.android.domain.database.ChannelRooster;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment2;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChannelsListAdapter extends RecyclerView.Adapter<ChannelsListAdapter.ViewHolder> implements NewAlarmFragment2.ChannelInterface {
    private ArrayList<ChannelRooster> mDataset;
    private Activity mActivity;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView txtChannelName;
        public TextView imgInfo;
        public ImageView imgChannelImage;
        public ImageView imgChannelSelected;
        public CardView cardViewChannel;
        public ProgressBar progressBar;

        public ViewHolder(View v) {
            super(v);
            cardViewChannel = v.findViewById(R.id.card_view_channels);
            txtChannelName = v.findViewById(R.id.cardview_channel_name);
            imgInfo = v.findViewById(R.id.cardview_channel_info);
            imgChannelImage = v.findViewById(R.id.card_view_channel_image);
            imgChannelSelected = v.findViewById(R.id.cardview_channel_selected);
            progressBar = v.findViewById(R.id.progressBar);
        }
    }

    public void add(int position, ChannelRooster item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ChannelsListAdapter(ArrayList<ChannelRooster> mDataset, Activity mActivity) {
        this.mDataset = mDataset;
        this.mActivity = mActivity;
    }

    public ChannelsListAdapter() {

    }

    // Create new views (invoked by the layout manager)
    @Override
    public ChannelsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        context = parent.getContext();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_channels, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        final ChannelRooster channelRooster = mDataset.get(position);
        // - replace the contents of the view with that element
        holder.cardViewChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleChannelSelection(channelRooster);

                if (channelRooster.isSelected()) {
                    setSelectedChannel(channelRooster);
                } else {
                    clearSelectedChannel();
                }

                holder.imgChannelSelected.setVisibility(View.VISIBLE);
                notifyDataSetChanged();
            }
        });

        holder.txtChannelName.setText(channelRooster.getName());
        if (mDataset.get(position).isSelected()) {
            holder.imgChannelSelected.setVisibility(View.VISIBLE);
        } else {
            holder.imgChannelSelected.setVisibility(View.INVISIBLE);
        }

        holder.imgInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new MaterialDialog.Builder(mActivity)
                        .title(channelRooster.getName())
                        .content(channelRooster.getChannel_description())
                        .positiveText(R.string.ok)
                        .negativeText("")
                        .show();

                FA.Log(FA.Event.channel_info_viewed.class, FA.Event.channel_info_viewed.Param.channel_title, channelRooster.getChannel_uid());

            }
        });

        try {
            Picasso.with(context).load(channelRooster.getChannel_photo())
                    .fit()
                    .centerCrop()
                    .into(holder.imgChannelImage, new Callback() {
                @Override
                public void onSuccess() {
                    holder.progressBar.setVisibility(View.GONE);
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

    @Override
    public void setSelectedChannel(ChannelRooster channelRooster) {
        IAlarmSetListener mListener = (IAlarmSetListener) mActivity;
        Alarm alarm = mListener.getAlarmDetails();
        alarm.setChannel(new AlarmChannel(channelRooster.getName(), channelRooster.getChannel_uid()));
        mListener.setAlarmDetails(alarm);
    }

    @Override
    public void clearSelectedChannel() {
        IAlarmSetListener mListener = (IAlarmSetListener) mActivity;
        Alarm alarm = mListener.getAlarmDetails();
        alarm.setChannel(new AlarmChannel());
        mListener.setAlarmDetails(alarm);
    }

    private void toggleChannelSelection(ChannelRooster channelSelection) {
        //Clear all channels except selected
        for (ChannelRooster channel : mDataset) {
            if (!(channel == channelSelection)) channel.setSelected(false);
        }
        //Toggle selected channel
        if (channelSelection.isSelected()) {
            channelSelection.setSelected(false);
        } else {
            channelSelection.setSelected(true);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}