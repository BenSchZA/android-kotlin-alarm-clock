/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.domain.local.Contact;
import com.roostermornings.android.domain.local.Friend;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;

import static com.roostermornings.android.BaseApplication.mCurrentUser;
import static com.roostermornings.android.util.Constants.VIEW_TYPE_ADD;
import static com.roostermornings.android.util.Constants.VIEW_TYPE_HEADER;
import static com.roostermornings.android.util.Constants.VIEW_TYPE_INVITE;
import static com.roostermornings.android.util.Constants.VIEW_TYPE_UNKNOWN;

/**
 * Created by bscholtz on 06/03/17.
 */

public class FriendsInviteListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable, FriendsFragmentActivity.FriendsInviteListAdapterInterface {
    private ArrayList<Object> mDataset;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one activityContentView per item, and
    // you provide access to all the views for a data item in a activityContentView holder
    public class AddViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public TextView txtInitials;
        public Button btnAdd;

        public AddViewHolder(View v) {
            super(v);
            imgProfilePic = itemView.findViewById(R.id.my_friends_profile_pic);
            txtName = itemView.findViewById(R.id.my_friends_profile_name);
            txtInitials = itemView.findViewById(R.id.txtInitials);
            btnAdd = itemView.findViewById(R.id.friends_add_button);
        }
    }

    public class InviteViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public TextView txtInitials;
        public Button btnAdd;

        public InviteViewHolder(View v) {
            super(v);
            imgProfilePic = itemView.findViewById(R.id.my_friends_profile_pic);
            txtName = itemView.findViewById(R.id.my_friends_profile_name);
            txtInitials = itemView.findViewById(R.id.txtInitials);
            btnAdd = itemView.findViewById(R.id.friends_add_button);
        }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView txtHeader;

        public HeaderViewHolder(View v) {
            super(v);
            txtHeader = itemView.findViewById(R.id.list_header);
        }
    }

    @Override
    public int getItemViewType(int position) {

        Object object = mDataset.get(position);

        if(object instanceof Friend) {
            return VIEW_TYPE_ADD;
        } else if(object instanceof Contact) {
            return VIEW_TYPE_INVITE;
        } else if(object instanceof String) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_UNKNOWN;
        }
    }

    public void add(int position, Object item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void refreshAll(ArrayList<Object> myDataset) {
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    public void remove(int position, Object item) {
        mDataset.remove(item);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public FriendsInviteListAdapter(ArrayList<Object> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {
        context = parent.getContext();
        View v;

        switch (viewType) {
            case VIEW_TYPE_ADD:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_friends_invite, parent, false);
                return new FriendsInviteListAdapter.AddViewHolder(v);
            case VIEW_TYPE_INVITE:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_friends_invite, parent, false);
                return new FriendsInviteListAdapter.InviteViewHolder(v);
            case VIEW_TYPE_HEADER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header, parent, false);
                return new FriendsInviteListAdapter.HeaderViewHolder(v);
            default:
                return null;
        }
    }

    // Replace the contents of a activityContentView (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder objectHolder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the activityContentView with that element

        if(objectHolder instanceof AddViewHolder) {
            final FriendsInviteListAdapter.AddViewHolder holder = (AddViewHolder)objectHolder;

            final Friend user = (Friend) mDataset.get(position);
            user.setSelected(false);
            holder.txtName.setText(user.getUser_name());
            holder.txtInitials.setText(RoosterUtils.getInitials(user.getUser_name()));
            holder.btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    user.setSelected(!user.getSelected());
                    holder.btnAdd.setSelected(user.getSelected());

                    addUser(user);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 200ms
                            remove(holder.getAdapterPosition(), user);
                            notifyDataSetChanged();
                        }
                    }, 200);
                }
            });
        } else if(objectHolder instanceof InviteViewHolder) {
            final FriendsInviteListAdapter.InviteViewHolder holder = (InviteViewHolder)objectHolder;

            final Contact contact = (Contact) mDataset.get(position);
            contact.setSelected(false);
            holder.txtName.setText(contact.getName());
            holder.txtInitials.setText(RoosterUtils.getInitials(contact.getName()));
            holder.btnAdd.setText(R.string.text_friends_button_invite);
            holder.btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    contact.setSelected(!contact.getSelected());
                    holder.btnAdd.setSelected(contact.getSelected());

                    //Invite contact via SMS
                    if(context instanceof FriendsFragmentActivity) {
                        ((FriendsFragmentActivity) context).inviteContact(contact);
                    }

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 200ms
                            remove(holder.getAdapterPosition(), contact);
                            notifyDataSetChanged();
                        }
                    }, 200);
                }
            });

        } else if(objectHolder instanceof HeaderViewHolder) {
            final FriendsInviteListAdapter.HeaderViewHolder holder = (HeaderViewHolder)objectHolder;

            final String title = (String) mDataset.get(position);
            holder.txtHeader.setText(title);
            holder.txtHeader.setVisibility(View.VISIBLE);
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

                mDataset = (ArrayList<Object>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<Object> filteredContacts = new ArrayList<>();

                //Perform your search here using the search constraint string
                constraint = constraint.toString().toLowerCase();
                for (int i = 0; i < mDataset.size(); i++) {

                    String contactData;
                    Object tempObject = mDataset.get(i);
                    if(tempObject instanceof Friend) {
                        contactData = ((Friend)tempObject).getUser_name();
                    } else if(tempObject instanceof Contact) {
                        contactData = ((Contact)tempObject).getName();
                    } else {
                        contactData = "";
                    }

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

    @Override
    public void addUser(Friend friend) {

        String inviteUrl = String.format("friend_requests_received/%s/%s", friend.getUid(), mCurrentUser.getUid());
        String currentUserUrl = String.format("friend_requests_sent/%s/%s", mCurrentUser.getUid(), friend.getUid());

        //Create friend object from current signed in user
        Friend currentUserFriend = new Friend(mCurrentUser.getUid(), mCurrentUser.getUser_name(), mCurrentUser.getProfile_pic(), mCurrentUser.getCell_number());

        //Append to received and sent request list
        BaseApplication.getFbDbRef().getDatabase().getReference(inviteUrl).setValue(currentUserFriend);
        BaseApplication.getFbDbRef().getDatabase().getReference(currentUserUrl).setValue(friend);

        Toaster.makeToast(context, friend.getUser_name() + " invited!", Toast.LENGTH_LONG).checkTastyToast();
    }
}
