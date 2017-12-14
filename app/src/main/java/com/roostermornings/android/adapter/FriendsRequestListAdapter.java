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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.Toaster;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.roostermornings.android.BaseApplication.mCurrentUser;

/**
 * Created by bscholtz on 08/03/17.
 */

public class FriendsRequestListAdapter extends RecyclerView.Adapter<FriendsRequestListAdapter.ViewHolder> implements Filterable, FriendsFragmentActivity.FriendsRequestListAdapterInterface {
    private ArrayList<Friend> mDataset;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one activityContentView per item, and
    // you provide access to all the views for a data item in a activityContentView holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public TextView txtInitials;
        public Button btnAdd;
        public ImageButton btnDelete;

        public ViewHolder(View v) {
            super(v);
            imgProfilePic = itemView.findViewById(R.id.my_friends_profile_pic);
            txtName = itemView.findViewById(R.id.my_friends_profile_name);
            txtInitials = itemView.findViewById(R.id.txtInitials);
            btnAdd = itemView.findViewById(R.id.friends_button1);
            btnDelete = itemView.findViewById(R.id.friends_button2);
        }
    }

    public void add(int position, Friend item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void refreshAll(ArrayList<Friend> myDataset) {
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    public void remove(int position, Friend item) {
        mDataset.remove(item);
        notifyItemRemoved(position);
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public FriendsRequestListAdapter(ArrayList<Friend> myDataset) {
        mDataset = myDataset;
    }



    // Create new views (invoked by the layout manager)
    @Override
    public FriendsRequestListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
        context = parent.getContext();
        // create a new activityContentView
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_friends_request, parent, false);
        // set the activityContentView's size, margins, paddings and layout parameters
        return new FriendsRequestListAdapter.ViewHolder(v);
    }

    // Replace the contents of a activityContentView (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final FriendsRequestListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the activityContentView with that element
        final Friend user = mDataset.get(position);
        user.setSelected(false);
        holder.txtName.setText(user.getUser_name());
        holder.txtInitials.setText(RoosterUtils.getInitials(user.getUser_name()));

        setProfilePic(user.getProfile_pic(), holder, user);

        //Add button listener to accept request
        holder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setSelected(!user.getSelected());
                holder.btnAdd.setSelected(user.getSelected());

                //Manually refresh friends list when new friend added, use catch in case detached
                try {
                    ((FriendsFragmentActivity) context).manualSwipeRefreshFriends();
                } catch(Exception e) {
                    e.printStackTrace();
                }

                acceptFriendRequest(user);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 200ms
                        remove(holder.getAdapterPosition(), user);
                    }
                }, 200);
            }
        });

        //Add button listener to reject request
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                rejectFriendRequest(user);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 200ms
                        remove(holder.getAdapterPosition(), user);
                    }
                }, 200);
            }
        });
    }

    private void setProfilePic(String url, final FriendsRequestListAdapter.ViewHolder holder, final Friend user) {

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

                mDataset = (ArrayList<Friend>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<Friend> filteredContacts = new ArrayList<>();

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

    //Accept friend request and update Firebase DB
    public void acceptFriendRequest(Friend acceptFriend) {

        String currentUserUrl = String.format("users/%s/friends", mCurrentUser.getUid());
        String friendUserUrl = String.format("users/%s/friends", acceptFriend.getUid());

        //Create friend object from current signed in user
        Friend currentUserFriend = new Friend(mCurrentUser.getUid(), mCurrentUser.getUser_name(), mCurrentUser.getProfile_pic(), mCurrentUser.getCell_number());

        //Update current user and friend entry as: uid:boolean
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(acceptFriend.getUid(), true);
        BaseApplication.getFbDbRef().getDatabase().getReference(currentUserUrl).updateChildren(childUpdates);
        childUpdates.clear();

        childUpdates.put(currentUserFriend.getUid(), true);
        BaseApplication.getFbDbRef().getDatabase().getReference(friendUserUrl).updateChildren(childUpdates);
        childUpdates.clear();

        String receivedUrl = String.format("friend_requests_received/%s/%s", mCurrentUser.getUid(), acceptFriend.getUid());
        String sentUrl = String.format("friend_requests_sent/%s/%s", acceptFriend.getUid(), mCurrentUser.getUid());

        //Clear received and sent request list
        BaseApplication.getFbDbRef().getDatabase().getReference(receivedUrl).setValue(null);
        BaseApplication.getFbDbRef().getDatabase().getReference(sentUrl).setValue(null);

        //Notify user that friend request accepted
        Toaster.makeToast(context, acceptFriend.getUser_name() + "'s friend request accepted!", Toast.LENGTH_LONG).checkTastyToast();
    }

    public void rejectFriendRequest(Friend rejectFriend) {

        String receivedUrl = String.format("friend_requests_received/%s/%s", mCurrentUser.getUid(), rejectFriend.getUid());
        String sentUrl = String.format("friend_requests_sent/%s/%s", rejectFriend.getUid(), mCurrentUser.getUid());

        //Clear received and sent request list
        BaseApplication.getFbDbRef().getDatabase().getReference(receivedUrl).setValue(null);
        BaseApplication.getFbDbRef().getDatabase().getReference(sentUrl).setValue(null);

        //Notify user that friend request accepted
        Toaster.makeToast(context, rejectFriend.getUser_name() + "'s friend request rejected!", Toast.LENGTH_LONG).checkTastyToast();
    }
}
