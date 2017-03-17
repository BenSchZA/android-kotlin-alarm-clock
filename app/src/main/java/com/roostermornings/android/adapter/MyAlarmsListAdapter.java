/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.util.RoosterUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyAlarmsListAdapter extends RecyclerView.Adapter<MyAlarmsListAdapter.ViewHolder> {
    private ArrayList<Alarm> mDataset;
    private Activity mActivity;
    private Application mApplication;
    private BroadcastReceiver receiver;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        @BindView(R.id.cardview_alarm_time_textview)
        TextView txtAlarmTime;
        @BindView(R.id.cardview_alarm_days_textview)
        TextView txtAlarmDays;
        @BindView(R.id.cardview_alarm_channel_textview)
        TextView txtAlarmChannel;
        @BindView(R.id.cardview_alarm_delete)
        ImageView imgDelete;
        @BindView(R.id.rooster_notification)
        ImageView roosterNotification;

        public ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
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
    public MyAlarmsListAdapter(ArrayList<Alarm> myDataset, Activity activity, Application application) {
        mDataset = myDataset;
        mActivity = activity;
        mApplication = application;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        updateRoosterNotification(holder);

        holder.txtAlarmTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MyAlarmsFragmentActivity) mActivity).editAlarm(alarm.getUid());
            }
        });

        holder.txtAlarmDays.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MyAlarmsFragmentActivity) mActivity).editAlarm(alarm.getUid());
            }
        });

        holder.txtAlarmChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MyAlarmsFragmentActivity) mActivity).editAlarm(alarm.getUid());
            }
        });


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
                                    mActivity.unregisterReceiver(receiver);
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

    private void updateRoosterNotification(final ViewHolder holder) {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //If notifications waiting, display new Rooster notification
        if (((BaseApplication) mApplication).getNotificationFlag("roosterCount") > 0) {
            setRoosterNotification(holder, true);
        }

        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction("rooster.update.ROOSTER_NOTIFICATION");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                if(((BaseApplication) mApplication).getNotificationFlag("roosterCount") > 0){
                    setRoosterNotification(holder, true);
                }
            }
        };
        mActivity.registerReceiver(receiver, firebaseListenerServiceFilter);
    }

    private void setRoosterNotification(final ViewHolder holder, boolean notification) {
        holder.roosterNotification.setVisibility(View.VISIBLE);
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