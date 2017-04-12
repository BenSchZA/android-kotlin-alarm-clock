/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.new_alarm;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.R;
import com.roostermornings.android.adapter.ChannelsListAdapter;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;

import java.util.ArrayList;

import butterknife.BindView;


public class NewAlarmFragment2 extends BaseFragment {

    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = NewAlarmFragment2.class.getSimpleName();
    private String mUserUidParam;
    private IAlarmSetListener mListener;

    private DatabaseReference mChannelsReference;
    private DatabaseReference mChannelRoostersReference;
    private ArrayList<ChannelRooster> channelRoosters = new ArrayList<ChannelRooster>();

    @BindView(R.id.channelsListView)
    RecyclerView mRecyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public NewAlarmFragment2() {
        // Required empty public constructor
    }

    public static NewAlarmFragment2 newInstance(String param1) {
        NewAlarmFragment2 fragment = new NewAlarmFragment2();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID_PARAM, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserUidParam = getArguments().getString(ARG_USER_UID_PARAM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = initiate(inflater, R.layout.fragment_new_alarm_step2, container, false);

        mChannelsReference = FirebaseDatabase.getInstance().getReference()
                .child("channels");
        mChannelsReference.keepSynced(true);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ChannelsListAdapter(channelRoosters, NewAlarmFragment2.this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        ValueEventListener channelsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    final Channel channel = postSnapshot.getValue(Channel.class);

                    if (channel.isActive()) {
                        if(channel.isNew_alarms_start_at_first_iteration()) {
                            DeviceAlarmTableManager deviceAlarmTableManager = new DeviceAlarmTableManager(getContext());
                            Integer iteration = deviceAlarmTableManager.getChannelStoryIteration(channel.getUid());
                            if(iteration != null && iteration > 0) {
                                mChannelRoostersReference = FirebaseDatabase.getInstance().getReference()
                                        .child("channel_rooster_uploads").child(channel.getUid()).child(String.valueOf(iteration));
                            }
                            else {
                                //TODO: what if doesn't exist? Let's hope Node functions shifts them
                                mChannelRoostersReference = FirebaseDatabase.getInstance().getReference()
                                        .child("channel_rooster_uploads").child(channel.getUid()).child("1");
                            }
                        } else {
                            mChannelRoostersReference = FirebaseDatabase.getInstance().getReference()
                                    .child("channel_rooster_uploads").child(channel.getUid()).child(String.valueOf(channel.getCurrent_rooster_cycle_iteration()));
                        }
                        mChannelRoostersReference.keepSynced(true);

                        ValueEventListener channelRoostersListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                    ChannelRooster channelRooster = dataSnapshot.getValue(ChannelRooster.class);
                                if(channelRooster != null && channelRooster.isActive()) {
                                    channelRooster.setSelected(false);
                                    channelRoosters.add(channelRooster);
                                    mAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                                showToast(getContext(), "Failed to load channel.", Toast.LENGTH_SHORT);
                            }
                        };
                        mChannelRoostersReference.addValueEventListener(channelRoostersListener);

                        //TODO: check this solves bug
                        if (mListener != null)
                            mListener.retrieveAlarmDetailsFromSQL(); //this is only relevant for alarms being edited
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                showToast(getContext(), "Failed to load channel.", Toast.LENGTH_SHORT);
            }
        };
        mChannelsReference.addValueEventListener(channelsListener);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof IAlarmSetListener) {
            mListener = (IAlarmSetListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement IAlarmSetListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (visible) {
            if (mListener != null) mListener.setNextButtonCaption(getString(R.string.save));
        }
    }

    public void setSelectedChannel(ChannelRooster channelRooster) {
        Alarm alarm = mListener.getAlarmDetails();
        alarm.setChannel(new AlarmChannel(channelRooster.getName(), channelRooster.getChannel_uid()));
        mListener.setAlarmDetails(alarm);
    }

    public void selectEditedAlarmChannel() {

        final Alarm alarm = (mListener == null) ? new Alarm() : mListener.getAlarmDetails();
        AlarmChannel alarmChannel = alarm.getChannel();
        if(alarmChannel == null) return;

        for (ChannelRooster channelRooster : channelRoosters) {
            if (alarmChannel.getId().equals(channelRooster.getChannel_uid())) {
                channelRooster.setSelected(true);
                alarm.getChannel().setName(channelRooster.getName());
                mAdapter.notifyDataSetChanged();
            }
        }
    }
}
