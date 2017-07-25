/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.StrUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyAlarmsListAdapter extends RecyclerView.Adapter<MyAlarmsListAdapter.ViewHolder> {
    private ArrayList<Alarm> mDataset;
    private Activity mActivity;
    private BroadcastReceiver receiver;
    private Context context;

    boolean computingLayout;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        @BindView(R.id.card_view_alarms)
        CardView cardView;
        @BindView(R.id.cardview_alarm_time_textview)
        TextView txtAlarmTime;
        @BindView(R.id.cardview_alarm_days_textview)
        TextView txtAlarmDays;
        @BindView(R.id.cardview_alarm_channel_textview)
        TextView txtAlarmChannel;
        @BindView(R.id.cardview_alarm_person)
        ImageView roosterNotificationPerson;
        @BindView(R.id.rooster_notification_text)
        TextView roosterNotificationText;
        @BindView(R.id.rooster_notification_parent)
        RelativeLayout roosterNotificationParent;
        @BindView(R.id.switch_enable)
        SwitchCompat switchEnable;

        public ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }

    }

    public void delete(int position, Alarm item) { //removes the row
        mDataset.remove(item);
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

    public MyAlarmsListAdapter() {

    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAlarmsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        context = parent.getContext();

        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_my_alarms, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        //Set flag to show layout is being refreshed
        computingLayout = true;

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Alarm alarm = mDataset.get(position);

        try {
            holder.txtAlarmTime.setText(RoosterUtils.setAlarmTimeFromHourAndMinute(alarm));
            holder.txtAlarmDays.setText(RoosterUtils.getAlarmDays(alarm));
            if(alarm.getChannel() != null) {
                holder.txtAlarmChannel.setText(alarm.getChannel().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(alarm.isAllow_friend_audio_files()) {
            holder.roosterNotificationPerson.setVisibility(View.VISIBLE);
            //Show notification of number of waiting Roosters for next pending alarm
            if(alarm.getUnseen_roosters() != null && alarm.getUnseen_roosters() > 0) {
                holder.roosterNotificationParent.setVisibility(View.VISIBLE);
                holder.roosterNotificationText.setText(String.valueOf(alarm.getUnseen_roosters()));
                holder.roosterNotificationPerson.setAnimation(AnimationUtils.loadAnimation(context, R.anim.pulse));
                holder.roosterNotificationParent.setAnimation(AnimationUtils.loadAnimation(context, R.anim.pulse));
            } else{
                holder.roosterNotificationParent.setVisibility(View.INVISIBLE);
                holder.roosterNotificationPerson.clearAnimation();
                holder.roosterNotificationParent.clearAnimation();
            }
        }

        holder.switchEnable.setOnCheckedChangeListener(new SwitchCompat.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                //Ensure that layout is not being computed: "IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling"
                //If check changes during computation, crash occurs
                if(!computingLayout) ((MyAlarmsFragmentActivity) mActivity).toggleAlarmSetEnable(alarm, isChecked);
            }
        });

        holder.switchEnable.setChecked(alarm.isEnabled());

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MyAlarmsFragmentActivity) mActivity).editAlarm(alarm.getUid());
            }
        });

        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                if (mActivity instanceof MyAlarmsFragmentActivity) {

                    new MaterialDialog.Builder(mActivity)
                            .theme(Theme.LIGHT)
                            .content(R.string.dialog_confirm_alarm_delete)
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .negativeColorRes(R.color.grey)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    mDataset.remove(alarm);
                                    notifyItemRemoved(mDataset.indexOf(alarm));
                                    notifyItemRangeChanged(mDataset.indexOf(alarm), mDataset.size());
                                    ((MyAlarmsFragmentActivity) mActivity).deleteAlarm(alarm.getUid());
                                    if(StrUtils.notNullOrEmpty(alarm.getChannel().getName())) {
                                        FA.Log(FA.Event.alarm_deleted.class, FA.Event.alarm_deleted.Param.channel_title, alarm.getChannel().getName());
                                    } else {
                                        FA.Log(FA.Event.alarm_deleted.class, null, null);
                                    }
                                }
                            })
                            .show();
                }
                return true;
            }
        });

        holder.roosterNotificationParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity instanceof MyAlarmsFragmentActivity) {

                    String dialogText = "Social roosters are voice notes from your friends that wake you up.";
                    switch (alarm.getUnseen_roosters()){
                        case 0:
                            dialogText = "Social roosters are voice notes from your friends that wake you up.";
                            break;
                        case 1:
                            dialogText = "You have received " + String.valueOf(alarm.getUnseen_roosters()) + " rooster from a friend to wake you up.";
                            break;
                        default:
                            dialogText = "You have received " + String.valueOf(alarm.getUnseen_roosters()) + " roosters from friends to wake you up.";
                            break;
                    }

                    new MaterialDialog.Builder(mActivity)
                            .theme(Theme.LIGHT)
                            .content(dialogText)
                            .show();
                }
            }
        });

        holder.roosterNotificationPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity instanceof MyAlarmsFragmentActivity) {

                    String dialogText = "Social roosters are voice notes from your friends that wake you up.";
                    switch (alarm.getUnseen_roosters()){
                        case 0:
                            dialogText = "Social roosters are voice notes from your friends that wake you up.";
                            break;
                        case 1:
                            dialogText = "You have received " + String.valueOf(alarm.getUnseen_roosters()) + " rooster from a friend to wake you up.";
                            break;
                        default:
                            dialogText = "You have received " + String.valueOf(alarm.getUnseen_roosters()) + " roosters from friends to wake you up.";
                            break;
                    }

                    new MaterialDialog.Builder(mActivity)
                            .theme(Theme.LIGHT)
                            .content(dialogText)
                            .show();
                }
            }
        });

        //Clear flag to show layout is being refreshed
        computingLayout = false;
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