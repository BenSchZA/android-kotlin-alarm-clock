/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.util.RoosterUtils;

import java.util.ArrayList;

public class MyAlarmsListAdapter extends RecyclerView.Adapter<MyAlarmsListAdapter.ViewHolder> {
    private ArrayList<Alarm> mDataset;
    private Activity mActivity;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView txtAlarmTime;
        public TextView txtAlarmDays;
        public TextView txtAlarmChannel;
        public ImageView imgDelete;

        public ViewHolder(View v) {
            super(v);
            txtAlarmTime = (TextView) v.findViewById(R.id.cardview_alarm_time_textview);
            txtAlarmDays = (TextView) v.findViewById(R.id.cardview_alarm_days_textview);
            txtAlarmChannel = (TextView) v.findViewById(R.id.cardview_alarm_channel_textview);
            imgDelete = (ImageView) v.findViewById(R.id.cardview_alarm_delete);
        }

    }

    public void delete(int position) { //removes the row
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    public void add(int position, Alarm item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAlarmsListAdapter(ArrayList<Alarm> myDataset, Activity activity) {
        mDataset = myDataset;
        mActivity = activity;
        setHasStableIds(true);
    }


    // Create new views (invoked by the layout manager)
    @Override
    public MyAlarmsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_my_alarms, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Alarm alarm = mDataset.get(position);

        try {
            holder.txtAlarmTime.setText(RoosterUtils.setAlarmTimeFromHourAndMinute(mDataset.get(position)));
            holder.txtAlarmDays.setText(RoosterUtils.getAlarmDays(mDataset.get(position)));
            holder.txtAlarmChannel.setText(mDataset.get(position).getChannel().getName());
        }catch(Exception e) {
            e.printStackTrace();
        }

        holder.imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity instanceof MyAlarmsFragmentActivity) {

                    View dialogMmpView = LayoutInflater.from(mActivity)
                            .inflate(R.layout.dialog_confirm_alarm_delete, null);
                    new MaterialDialog.Builder(mActivity)
                            .customView(dialogMmpView, false)
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .negativeColorRes(R.color.grey)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                    mDataset.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, mDataset.size());
                                    ((MyAlarmsFragmentActivity) mActivity).deleteAlarm(alarm.getSetId(), alarm.getUid());
                                    //TODO: Delete alarm set
                                }
                            })
                            .show();


                }
            }
        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}