/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.util.RoosterUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by bscholtz on 08/03/17.
 */

public class FriendsRequestListAdapter extends RecyclerView.Adapter<FriendsRequestListAdapter.ViewHolder> {
    private ArrayList<Friend> mDataset;
    private Context mContext;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public TextView txtInitials;
        public Button btnAdd;
        public ImageButton btnDelete;

        public ViewHolder(View v) {
            super(v);
            imgProfilePic = (ImageView) itemView.findViewById(R.id.my_friends_profile_pic);
            txtName = (TextView) itemView.findViewById(R.id.my_friends_profile_name);
            txtInitials = (TextView) itemView.findViewById(R.id.txtInitials);
            btnAdd = (Button) itemView.findViewById(R.id.friends_button1);
            btnDelete = (ImageButton) itemView.findViewById(R.id.friends_button2);
        }
    }

    public void add(int position, Friend item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(Friend item) {
        int position = mDataset.indexOf(item);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public FriendsRequestListAdapter(ArrayList<Friend> myDataset, Context context) {
        mDataset = myDataset;
        mContext = context;
    }



    // Create new views (invoked by the layout manager)
    @Override
    public FriendsRequestListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_friends_request, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new FriendsRequestListAdapter.ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final FriendsRequestListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Friend user = mDataset.get(position);
        user.setSelected(false);
        holder.txtName.setText(mDataset.get(position).getUser_name());
        holder.txtInitials.setText(RoosterUtils.getInitials(mDataset.get(position).getUser_name()));

        setProfilePic(user.getProfile_pic(), holder, position);

        //Add button listener to accept request
        holder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setSelected(!user.getSelected());
                setButtonBackground(holder.btnAdd, user.getSelected());

                ((FriendsFragmentActivity)mContext).acceptFriendRequest(user);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 200ms
                        remove(user);
                    }
                }, 200);
            }
        });

        //Add button listener to reject request
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ((FriendsFragmentActivity)mContext).rejectFriendRequest(user);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 200ms
                        remove(user);
                    }
                }, 200);
            }
        });
    }

    private void setProfilePic(String url, final FriendsRequestListAdapter.ViewHolder holder, final int position) {

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

    private void setButtonBackground(Button addButton, Boolean focused) {
        if (focused) addButton.setBackground(mContext.getResources().getDrawable(R.drawable.rooster_button_light_blue));
        else addButton.setBackground(mContext.getResources().getDrawable(R.drawable.rooster_button_semi_transparent));
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
