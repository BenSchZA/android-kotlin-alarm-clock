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
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.adapter.ChannelsListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import butterknife.BindView;

public class NewAlarmFragment2 extends BaseFragment {

    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = NewAlarmFragment2.class.getSimpleName();
    private String mUserUidParam;
    private IAlarmSetListener mListener;

    private DatabaseReference mChannelsReference;
    private DatabaseReference mChannelRoostersReference;
    private ArrayList<ChannelRooster> channelRoosters = new ArrayList<>();
    private Map<Integer, List<ChannelRooster>> channelRoosterMap = new TreeMap<>(Collections.reverseOrder());

    @BindView(R.id.main_content)
    RecyclerView mRecyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Inject DeviceAlarmTableManager deviceAlarmTableManager;
    @Inject Context AppContext;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    public NewAlarmFragment2() {
        // Required empty public constructor
    }

    public interface ChannelInterface {
        void setSelectedChannel(ChannelRooster channelRooster);
        void clearSelectedChannel();
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
        inject(((BaseApplication)getActivity().getApplication()).getRoosterApplicationComponent());

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
        mAdapter = new ChannelsListAdapter(channelRoosters, getActivity());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(AppContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        new Thread() {
            @Override
            public void run() {
                ValueEventListener channelsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            final Channel channel = postSnapshot.getValue(Channel.class);

                            if (channel.isActive()) {
                                if(channel.isNew_alarms_start_at_first_iteration()) {
                                    Integer iteration = deviceAlarmTableManager.getChannelStoryIteration(channel.getUid());
                                    if(iteration == null || iteration <= 0) iteration = 1;
                                    getChannelRoosterData(channel, iteration);
                                } else {
                                    Integer iteration = channel.getCurrent_rooster_cycle_iteration();
                                    if(iteration == null || iteration <= 0) iteration = 1;
                                    getChannelRoosterData(channel, iteration);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                        showToast(AppContext, "Failed to load channel.", Toast.LENGTH_SHORT);
                    }
                };
                mChannelsReference.addValueEventListener(channelsListener);
            }
        }.run();

        return view;
    }

    private void getChannelRoosterData(final Channel channel, final Integer iteration) {
        final DatabaseReference channelRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                .child("channel_rooster_uploads").child(channel.getUid());
        //Ensure latest data is pulled
        channelRoosterUploadsReference.keepSynced(true);

        ValueEventListener channelRoosterUploadsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TreeMap<Integer,ChannelRooster> channelIterationMap = new TreeMap<>();
                //Check if node has children i.e. channelId content exists
                if(dataSnapshot.getChildrenCount() == 0) return;
                //Iterate over all content children
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    ChannelRooster channelRooster = postSnapshot.getValue(ChannelRooster.class);

                    //Set channelrooster display picture to banner image & description to channel description
                    //In future channelrooster specific details will be shown in discover page
                    channelRooster.setChannel_photo(channel.getPhoto());
                    channelRooster.setChannel_description(channel.getDescription());

                    if(channelRooster.isActive() && (channelRooster.getRooster_cycle_iteration() != iteration)) {
                        channelIterationMap.put(channelRooster.getRooster_cycle_iteration(), channelRooster);
                    } else if(channelRooster.isActive()) {
                        channelRooster.setSelected(false);
                        //This method allows multiple objects per key
                        if(channelRoosterMap.containsKey(channel.getPriority())) {
                            channelRoosterMap.get(channel.getPriority()).add(channelRooster);
                        } else {
                            List<ChannelRooster> values = new ArrayList<>();
                            values.add(channelRooster);
                            channelRoosterMap.put(channel.getPriority(), values);
                        }
                        refreshChannelRoosters();
                        return;
                    }
                }
                if(!channelIterationMap.isEmpty() && iteration != null) {
                    findNextValidChannelRooster(channelIterationMap, channel, iteration);
                }
                refreshChannelRoosters();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        channelRoosterUploadsReference.addListenerForSingleValueEvent(channelRoosterUploadsListener);
    }

    private void refreshChannelRoosters() {
        new Thread() {
            @Override
            public void run() {
                //TreeMap ensures unique and allows sorting by priority! How cool is that?
                channelRoosters.clear();
                //This method allows us to have multiple objects per priority key
                List<ChannelRooster> values = new ArrayList<>();
                for(List<ChannelRooster> channelRoosterList : channelRoosterMap.values()) {
                    values.addAll(channelRoosterList);
                }
                if(!values.isEmpty()) channelRoosters.addAll(values);
                if (mListener != null)
                    mListener.retrieveAlarmDetailsFromSQL(); //this is only relevant for alarms being edited
                mAdapter.notifyDataSetChanged();
            }
        }.run();
    }

    private void findNextValidChannelRooster(final TreeMap<Integer,ChannelRooster> channelIterationMap, final Channel channel, final Integer iteration) {
        new Thread() {
            @Override
            public void run() {
                //Check head and tail of naturally sorted TreeMap for next valid channel content
                SortedMap<Integer,ChannelRooster> tailMap = channelIterationMap.tailMap(iteration);
                SortedMap<Integer,ChannelRooster> headMap = channelIterationMap.headMap(iteration);
                if(!tailMap.isEmpty()) {
                    //User is starting story at next valid entry
                    //Set SQL entry for iteration to current valid story iteration, to be incremented on play
                    deviceAlarmTableManager.setChannelStoryIteration(channel.getUid(), tailMap.firstKey());
                    //Retrieve channel audio
                    ChannelRooster channelRooster = channelIterationMap.get(tailMap.firstKey());
                    channelRooster.setSelected(false);
                    //This method allows multiple objects per key
                    if(channelRoosterMap.containsKey(channel.getPriority())) {
                        channelRoosterMap.get(channel.getPriority()).add(channelRooster);
                    } else {
                        List<ChannelRooster> values = new ArrayList<>();
                        values.add(channelRooster);
                        channelRoosterMap.put(channel.getPriority(), values);
                    }
                }
                else if(!headMap.isEmpty()) {
                    //User is starting story from beginning again, at valid entry
                    //Set SQL entry for iteration to current valid story iteration, to be incremented on play
                    deviceAlarmTableManager.setChannelStoryIteration(channel.getUid(), headMap.firstKey());
                    //Retrieve channel audio
                    ChannelRooster channelRooster = channelIterationMap.get(headMap.firstKey());
                    channelRooster.setSelected(false);
                    //This method allows multiple objects per key
                    if(channelRoosterMap.containsKey(channel.getPriority())) {
                        channelRoosterMap.get(channel.getPriority()).add(channelRooster);
                    } else {
                        List<ChannelRooster> values = new ArrayList<>();
                        values.add(channelRooster);
                        channelRoosterMap.put(channel.getPriority(), values);
                    }
                }
            }
        }.run();
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
