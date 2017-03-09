package com.roostermornings.android.adapter;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.domain.NodeUser;
import com.roostermornings.android.util.RoosterUtils;

import java.util.ArrayList;

/**
 * Created by bscholtz on 06/03/17.
 */

public class FriendsInviteListAdapter extends RecyclerView.Adapter<FriendsInviteListAdapter.ViewHolder> {
    private ArrayList<NodeUser> mDataset;
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

        public ViewHolder(View v) {
            super(v);
            imgProfilePic = (ImageView) itemView.findViewById(R.id.my_friends_profile_pic);
            txtName = (TextView) itemView.findViewById(R.id.my_friends_profile_name);
            txtInitials = (TextView) itemView.findViewById(R.id.txtInitials);
            btnAdd = (Button) itemView.findViewById(R.id.my_friends_add);
        }
    }

    public void add(int position, NodeUser item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(NodeUser item) {
        int position = mDataset.indexOf(item);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public FriendsInviteListAdapter(ArrayList<NodeUser> myDataset, Context context) {
        mDataset = myDataset;
        mContext = context;
    }



    // Create new views (invoked by the layout manager)
    @Override
    public FriendsInviteListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout_my_friends, parent, false);
        // set the view's size, margins, paddings and layout parameters
        FriendsInviteListAdapter.ViewHolder vh = new FriendsInviteListAdapter.ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final FriendsInviteListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final NodeUser user = mDataset.get(position);
        user.setSelected(false);
        holder.txtName.setText(mDataset.get(position).getUser_name());
        holder.txtInitials.setText(RoosterUtils.getInitials(mDataset.get(position).getUser_name()));
        holder.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user.setSelected(!user.getSelected());
                setButtonBackground(holder.btnAdd, user.getSelected());

                ((FriendsFragmentActivity)mContext).inviteUser(user);

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
