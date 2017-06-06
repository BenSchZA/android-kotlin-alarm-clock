/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.StrUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by steven on 2017/02/22.
 */

public class NewAudioFriendsListAdapter extends RecyclerView.Adapter<NewAudioFriendsListAdapter.ViewHolder> {
    private ArrayList<User> mDataset;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public TextView txtInitials;
        public RadioButton btnSelect;
        public LinearLayout roosterFriendSelectLayout;

        public ViewHolder(View v) {
            super(v);
            imgProfilePic = (ImageView) itemView.findViewById(R.id.new_audio_friend_profile_pic);
            txtName = (TextView) itemView.findViewById(R.id.new_audio_friend_profile_name);
            txtInitials = (TextView) itemView.findViewById(R.id.txtInitials);
            btnSelect = (RadioButton) itemView.findViewById(R.id.new_audio_friend_select);
            roosterFriendSelectLayout = (LinearLayout) itemView.findViewById(R.id.roosterFriendSelectLayout);
        }
    }

    public void add(int position, User item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public NewAudioFriendsListAdapter(ArrayList<User> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public NewAudioFriendsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                    int viewType) {
        context = parent.getContext();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_new_audio_choose_friends, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final User user = mDataset.get(position);

        holder.txtName.setText(user.getUser_name());

        //Check if image is null, else previous images reused
        if(StrUtils.notNullOrEmpty(user.getProfile_pic())) {
            holder.imgProfilePic.setImageDrawable(null);
            holder.imgProfilePic.setAlpha(1f);
            holder.txtInitials.setText("");
            setProfilePic(user.getProfile_pic(), holder, user);
        } else {
            holder.imgProfilePic.setImageDrawable(null);
            holder.imgProfilePic.setAlpha(0.3f);
            holder.txtInitials.setText(RoosterUtils.getInitials(user.getUser_name()));
        }

        holder.btnSelect.setChecked(user.getSelected());
        holder.btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setSelected(!user.getSelected());
                holder.btnSelect.setChecked(user.getSelected());
            }
        });

        holder.roosterFriendSelectLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setSelected(!user.getSelected());
                holder.btnSelect.setChecked(user.getSelected());
            }
        });
    }

    private void setProfilePic(String url, final NewAudioFriendsListAdapter.ViewHolder holder, final User user) {

        try{
            Picasso.with(context).load(url)
                    .resize(50, 50)
                    .into(holder.imgProfilePic, new Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap imageBitmap = ((BitmapDrawable) holder.imgProfilePic.getDrawable()).getBitmap();
                            RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), imageBitmap);
                            imageDrawable.setCircular(true);
                            imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                            //holder.imgProfilePic.setImageAlpha(0);
                            holder.imgProfilePic.setImageDrawable(imageDrawable);
                        }

                        @Override
                        public void onError() {
                            holder.txtInitials.setText(RoosterUtils.getInitials(user.getUser_name()));
                        }
                    });
        } catch(IllegalArgumentException e){
            e.printStackTrace();
            holder.txtInitials.setText(RoosterUtils.getInitials(user.getUser_name()));
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