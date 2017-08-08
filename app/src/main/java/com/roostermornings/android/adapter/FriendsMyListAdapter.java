/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.activity.NewAudioRecordActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.StrUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Created by bscholtz on 08/03/17.
 */

public class FriendsMyListAdapter extends RecyclerView.Adapter<FriendsMyListAdapter.ViewHolder> implements Filterable {
    private ArrayList<User> mDataset;
    private Activity mActivity;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public TextView txtInitials;
        public ImageButton btnSend;
        public LinearLayout linearLayout;

        public ViewHolder(View v) {
            super(v);
            imgProfilePic = (ImageView) itemView.findViewById(R.id.my_friends_profile_pic);
            txtName = (TextView) itemView.findViewById(R.id.my_friends_profile_name);
            txtInitials = (TextView) itemView.findViewById(R.id.txtInitials);
            btnSend = (ImageButton) itemView.findViewById(R.id.friends_send_button);
            linearLayout = (LinearLayout) itemView.findViewById(R.id.friendLayout);
        }
    }

    public void add(int position, User item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void refreshAll(ArrayList<User> myDataset) {
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    public void remove(int position, User item) {
        mDataset.remove(item);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public FriendsMyListAdapter(ArrayList<User> myDataset, Activity activity) {
        mDataset = myDataset;
        mActivity = activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FriendsMyListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
        context = parent.getContext();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_friends_my, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new FriendsMyListAdapter.ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final FriendsMyListAdapter.ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final User user = mDataset.get(position);
        user.setSelected(false);
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

        holder.linearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                    new MaterialDialog.Builder(mActivity)
                            .theme(Theme.LIGHT)
                            .content(R.string.dialog_confirm_friend_delete)
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .negativeColorRes(R.color.grey)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    ((FriendsFragmentActivity)mActivity).deleteFriend(user);
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            //Do something after 200ms
                                            remove(holder.getAdapterPosition(), user);
                                        }
                                    }, 200);
                                }
                            })
                            .show();
                return true;
            }
        });

        holder.btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!((BaseActivity)mActivity).checkInternetConnection()) return;
                ArrayList<User> friendToSend = new ArrayList<>();
                friendToSend.add(user);
                Intent intent = new Intent(mActivity, NewAudioRecordActivity.class);
                Bundle bun = new Bundle();
                bun.putSerializable(Constants.EXTRA_FRIENDS_LIST, friendToSend);
                intent.putExtras(bun);
                mActivity.startActivity(intent);
            }
        });
    }

    private void setProfilePic(String url, final FriendsMyListAdapter.ViewHolder holder, final User user) {

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

    @Override
    public Filter getFilter() {

        final Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                mDataset = (ArrayList<User>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<User> filteredContacts = new ArrayList<>();

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

