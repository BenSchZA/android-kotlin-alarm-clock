/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.roostermornings.android.fragment.base.BaseFragment;

import java.util.ArrayList;

import butterknife.BindView;


public class NewAlarmFragment2 extends BaseFragment  {

    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = NewAlarmFragment2.class.getSimpleName();
    private String mUserUidParam;
    private IAlarmSetListener mListener;

    private DatabaseReference mChannelsReference;
    private ArrayList<Channel> channels = new ArrayList<Channel>();

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

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ChannelsListAdapter(channels, NewAlarmFragment2.this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        ValueEventListener alarmsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Channel channel = postSnapshot.getValue(Channel.class);
                    channel.setSelected(false);
                    channels.add(channel);
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                //TODO: Attempt to invoke virtual method 'android.content.res.Resources android.content.Context.getResources()' on a null object reference
                Toast.makeText(getContext(), "Failed to load alarms.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        mChannelsReference.addValueEventListener(alarmsListener);

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

    public void setSelectedChannel(Channel channel) {
        Alarm alarm = mListener.getAlarmDetails();
        alarm.setChannel(new AlarmChannel(channel.getName(), channel.getId()));
        mListener.setAlarmDetails(alarm);
    }
}
