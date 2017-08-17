/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.StrUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MessageStatusSentListAdapter extends RecyclerView.Adapter<MessageStatusSentListAdapter.ViewHolder> implements Filterable {
    private ArrayList<SocialRooster> mDataset = new ArrayList<>();
    private Activity mActivity;
    private Context context;

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.message_status_profile_pic)
        ImageView imgProfilePic;

        @BindView(R.id.message_status_friend_profile_name)
        TextView txtName;

        @BindView(R.id.txtInitials)
        TextView txtInitials;

        @BindView(R.id.message_status)
        TextView txtStatus;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public void add(int position, SocialRooster item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(int position, SocialRooster item) {
        mDataset.remove(item);
        notifyItemRemoved(position);
    }

    public void refreshAll(ArrayList<SocialRooster> myDataset) {
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MessageStatusSentListAdapter(ArrayList<SocialRooster> myDataset, Activity activity) {
        mDataset = myDataset;
        mActivity = activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageStatusSentListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                      int viewType) {
        context = parent.getContext();
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_message_status_sent, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new MessageStatusSentListAdapter.ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MessageStatusSentListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final SocialRooster socialRooster = mDataset.get(position);
        holder.txtName.setText(socialRooster.getUser_name());
        //Check if image is null, else previous images reused
        if(StrUtils.notNullOrEmpty(socialRooster.getProfile_pic())) {
            holder.imgProfilePic.setImageDrawable(null);
            holder.imgProfilePic.setAlpha(1f);
            holder.txtInitials.setText("");
            setProfilePic(socialRooster.getProfile_pic(), holder, socialRooster);
        } else {
            holder.imgProfilePic.setImageDrawable(null);
            holder.imgProfilePic.setAlpha(0.3f);
            holder.txtInitials.setText(RoosterUtils.getInitials(socialRooster.getUser_name()));
        }
        switch(socialRooster.getStatus()) {
            case Constants.MESSAGE_STATUS_SENT:
                holder.txtStatus.setText(context.getResources().getText(R.string.status_sent));
                break;
            case Constants.MESSAGE_STATUS_DELIVERED:
                holder.txtStatus.setText(context.getResources().getText(R.string.status_delivered));
                break;
            case Constants.MESSAGE_STATUS_RECEIVED:
                holder.txtStatus.setText(context.getResources().getText(R.string.status_awake));
                break;
            default:
                break;
        }
    }

    private void setProfilePic(String url, final MessageStatusSentListAdapter.ViewHolder holder, final SocialRooster socialRooster) {
        try{
            Picasso.with(context).load(url)
                    .resize(50, 50)
                    .centerCrop()
                    .into(holder.imgProfilePic, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap imageBitmap = ((BitmapDrawable) holder.imgProfilePic.getDrawable()).getBitmap();
                            RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), imageBitmap);
                            imageDrawable.setCircular(true);
                            imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                            holder.txtInitials.setText("");
                            holder.imgProfilePic.setAlpha(1f);
                            holder.imgProfilePic.setImageDrawable(imageDrawable);
                        }

                        @Override
                        public void onError() {
                            holder.imgProfilePic.setAlpha(0.3f);
                            holder.txtInitials.setText(RoosterUtils.getInitials(socialRooster.getUser_name()));
                        }
                    });
        } catch(IllegalArgumentException e){
            e.printStackTrace();
            holder.txtInitials.setText(RoosterUtils.getInitials(socialRooster.getUser_name()));
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

                mDataset = (ArrayList<SocialRooster>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<SocialRooster> filteredContacts = new ArrayList<>();

                //Perform your search here using the search constraint string
                constraint = constraint.toString().toLowerCase();
                for (int i = 0; i < mDataset.size(); i++) {
                    String contactData = mDataset.get(i).getUser_name();
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
