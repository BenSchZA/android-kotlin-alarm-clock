/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter.add_invite_contacts;

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
import com.roostermornings.android.domain.Contact;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.fragment.friends.FriendsInviteFragment3;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;

import static com.roostermornings.android.BaseApplication.mCurrentUser;

/**
 * Created by bscholtz on 06/03/17.
 */

public class InviteContactsListAdapter extends RecyclerView.Adapter<InviteContactsListAdapter.ViewHolder> implements Filterable {
    private ArrayList<Contact> mDataset;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public TextView txtInitials;
        public Button btnAdd;

        public ViewHolder(View v) {
            super(v);
            imgProfilePic = (ImageView) itemView.findViewById(R.id.my_friends_profile_pic);
            txtName = (TextView) itemView.findViewById(R.id.my_friends_profile_name);
            txtInitials = (TextView) itemView.findViewById(R.id.txtInitials);
            btnAdd = (Button) itemView.findViewById(R.id.friends_add_button);
        }
    }

    public void add(int position, Contact item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void refreshAll(ArrayList<Contact> myDataset) {
        mDataset = myDataset;
        notifyDataSetChanged();
    }

    public void remove(int position, Contact item) {
        mDataset.remove(item);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public InviteContactsListAdapter(ArrayList<Contact> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public InviteContactsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {
        context = parent.getContext();
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_friends_invite, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new InviteContactsListAdapter.ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final InviteContactsListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Contact contact = mDataset.get(position);
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

                mDataset = (ArrayList<Contact>) results.values;
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                ArrayList<Contact> filteredContacts = new ArrayList<>();

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
