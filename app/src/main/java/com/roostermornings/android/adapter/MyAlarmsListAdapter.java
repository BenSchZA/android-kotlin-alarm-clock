package com.roostermornings.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.util.RoosterUtils;

import java.util.ArrayList;

public class MyAlarmsListAdapter extends RecyclerView.Adapter<MyAlarmsListAdapter.ViewHolder> {
    private ArrayList<Alarm> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView txtAlarmTime;
        public TextView txtAlarmDays;
        public TextView txtAlarmChannel;

        public ViewHolder(View v) {
            super(v);
            txtAlarmTime = (TextView) v.findViewById(R.id.cardview_alarm_time_textview);
            txtAlarmDays = (TextView) v.findViewById(R.id.cardview_alarm_days_textview);
            txtAlarmChannel = (TextView) v.findViewById(R.id.cardview_alarm_channel_textview);
        }
    }

    public void add(int position, Alarm item) {
        mDataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(String item) {
        int position = mDataset.indexOf(item);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAlarmsListAdapter(ArrayList<Alarm> myDataset) {
        mDataset = myDataset;
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
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Alarm alarm = mDataset.get(position);

        holder.txtAlarmTime.setText(RoosterUtils.getAlarmTimeFromHourAndMinute(mDataset.get(position)));
        holder.txtAlarmDays.setText(RoosterUtils.getAlarmDays(mDataset.get(position)));
        holder.txtAlarmChannel.setText(mDataset.get(position).getChannel());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

}