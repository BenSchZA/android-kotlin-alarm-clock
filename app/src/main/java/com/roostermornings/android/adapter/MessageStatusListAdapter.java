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
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.RoosterUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MessageStatusListAdapter extends RecyclerView.Adapter<MessageStatusListAdapter.ViewHolder> {
    private ArrayList<SocialRooster> mDataset = new ArrayList<>();
    private Context mContext;
    private Activity mActivity;

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.message_status_friend_profile_pic)
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

    public void remove(String item) {
        int position = mDataset.indexOf(item);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MessageStatusListAdapter(ArrayList<SocialRooster> myDataset, Activity activity, Context context) {
        mDataset = myDataset;
        mContext = context;
        mActivity = activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MessageStatusListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                    int viewType) {
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_message_status, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new MessageStatusListAdapter.ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MessageStatusListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final SocialRooster socialRooster = mDataset.get(position);
        holder.txtName.setText(mDataset.get(position).getUser_name());
        setProfilePic(socialRooster.getProfile_pic(), holder, position);
        switch(socialRooster.getStatus()) {
            case Constants.MESSAGE_STATUS_SENT:
                holder.txtStatus.setText(mContext.getResources().getText(R.string.status_sent));
                break;
            case Constants.MESSAGE_STATUS_DELIVERED:
                holder.txtStatus.setText(mContext.getResources().getText(R.string.status_delivered));
                break;
            case Constants.MESSAGE_STATUS_RECEIVED:
                holder.txtStatus.setText(mContext.getResources().getText(R.string.status_awake));
                break;
            default:
                break;
        }
    }

    private void setProfilePic(String url, final MessageStatusListAdapter.ViewHolder holder, final int position) {

        try{
            Picasso.with(mContext).load(url)
                    .resize(50, 50)
                    .into(holder.imgProfilePic, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap imageBitmap = ((BitmapDrawable) holder.imgProfilePic.getDrawable()).getBitmap();
                            RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(mContext.getResources(), imageBitmap);
                            imageDrawable.setCircular(true);
                            imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                            //holder.imgProfilePic.setImageAlpha(0);
                            holder.imgProfilePic.setImageDrawable(imageDrawable);
                        }

                        @Override
                        public void onError() {
                            holder.txtInitials.setText(RoosterUtils.getInitials(mDataset.get(position).getUser_name()));
                        }
                    });
        } catch(IllegalArgumentException e){
            e.printStackTrace();
            holder.txtInitials.setText(RoosterUtils.getInitials(mDataset.get(position).getUser_name()));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void updateList(){
        notifyDataSetChanged();
    }
}
