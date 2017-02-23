package com.roostermornings.android.adapter;

import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.fragment.NewAlarmFragment2;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChannelsListAdapter extends RecyclerView.Adapter<ChannelsListAdapter.ViewHolder> {
    private ArrayList<Channel> mDataset;
    private Fragment mFragment;

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

        public ViewHolder(View v) {
            super(v);
            cardViewChannel = (CardView) v.findViewById(R.id.card_view_alarms);
            txtChannelName = (TextView) v.findViewById(R.id.cardview_channel_name);
            imgInfo = (TextView) v.findViewById(R.id.cardview_channel_info);
            imgChannelImage = (ImageView) v.findViewById(R.id.card_view_channel_image);
            imgChannelSelected = (ImageView) v.findViewById(R.id.cardview_channel_selected);
        }
    }

    public void add(int position, Channel item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(String item) {
        int position = mDataset.indexOf(item);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ChannelsListAdapter(ArrayList<Channel> myDataset, Fragment fragment) {
        mDataset = myDataset;
        mFragment = fragment;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ChannelsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_channels, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.cardViewChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (Channel channel : mDataset) {
                    channel.setSelected(false);
                }
                mDataset.get(position).setSelected(true);

                if (mFragment instanceof NewAlarmFragment2) {
                    ((NewAlarmFragment2) mFragment).setSelectedChannel(mDataset.get(position));
                }

                holder.imgChannelSelected.setVisibility(View.VISIBLE);
                notifyDataSetChanged();
            }
        });

        holder.txtChannelName.setText(mDataset.get(position).getName());
        if (mDataset.get(position).isSelected()) {
            holder.imgChannelSelected.setVisibility(View.VISIBLE);
        } else {
            holder.imgChannelSelected.setVisibility(View.INVISIBLE);
        }

        Picasso.with(mFragment.getContext()).load(mDataset.get(position).getPhoto()).
                fit().into(holder.imgChannelImage);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}