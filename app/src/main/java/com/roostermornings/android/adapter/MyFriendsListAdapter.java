package com.roostermornings.android.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.User;

import java.util.ArrayList;

/**
 * Created by bscholtz on 06/03/17.
 */

public class MyFriendsListAdapter extends RecyclerView.Adapter<com.roostermornings.android.adapter.MyFriendsListAdapter.ViewHolder> {
    private ArrayList<User> mDataset;
    private Context mContext;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgProfilePic;
        public TextView txtName;
        public Button btnAdd;

        public ViewHolder(View v) {
            super(v);
            imgProfilePic = (ImageView) itemView.findViewById(R.id.new_audio_friend_profile_pic);
            txtName = (TextView) itemView.findViewById(R.id.new_audio_friend_profile_name);
            btnAdd = (Button) itemView.findViewById(R.id.new_audio_friend_add);
        }
    }

    public void add(int position, User item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(String item) {
        int position = mDataset.indexOf(item);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public MyFriendsListAdapter(ArrayList<User> myDataset, Context context) {
        mDataset = myDataset;
        mContext = context;
    }



    // Create new views (invoked by the layout manager)
    @Override
    public com.roostermornings.android.adapter.MyFriendsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                                                        int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_new_audio_choose_friends, parent, false);
        // set the view's size, margins, paddings and layout parameters
        com.roostermornings.android.adapter.MyFriendsListAdapter.ViewHolder vh = new com.roostermornings.android.adapter.MyFriendsListAdapter.ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final com.roostermornings.android.adapter.MyFriendsListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final User user = mDataset.get(position);
        user.setSelected(false);
        holder.txtName.setText(mDataset.get(position).getUser_name());
        holder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setSelected(!user.getSelected());
                setButtonBackground(holder.btnAdd, user.getSelected());
            }
        });
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
