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
import com.roostermornings.android.channels.ChannelManager;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.geolocation.GeoHashUtils;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.Toaster;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import butterknife.BindView;

import static com.facebook.FacebookSdk.getApplicationContext;

public class NewAlarmFragment2 extends BaseFragment {

    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = NewAlarmFragment2.class.getSimpleName();
    private String mUserUidParam;
    private IAlarmSetListener mListener;

    private ChannelManager channelManager = new ChannelManager(getApplicationContext());

    private ArrayList<ChannelRooster> channelRoosters = new ArrayList<>();

    @BindView(R.id.main_content)
    RecyclerView mRecyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Inject JSONPersistence jsonPersistence;
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
        inject(BaseApplication.getRoosterApplicationComponent());

        if (getArguments() != null) {
            mUserUidParam = getArguments().getString(ARG_USER_UID_PARAM);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //Persist channels for seamless loading
        jsonPersistence.setNewAlarmChannelRoosters(channelRoosters);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = initiate(inflater, R.layout.fragment_new_alarm_step2, container, false);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ChannelsListAdapter(channelRoosters, getActivity());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(AppContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        if(!jsonPersistence.getNewAlarmChannelRoosters().isEmpty()) {
            channelRoosters.addAll(jsonPersistence.getNewAlarmChannelRoosters());
            mAdapter.notifyDataSetChanged();
        }

        ChannelManager.Companion.setOnFlagChannelManagerDataListener(new ChannelManager.Companion.OnFlagChannelManagerDataListener() {
            @Override
            public void onChannelRoosterDataChanged(@NotNull ArrayList<ChannelRooster> freshChannelRoosters) {
                channelRoosters.clear();
                channelRoosters.addAll(freshChannelRoosters);
            }

            @Override
            public void onSyncFinished() {
                if (mListener != null)
                    mListener.retrieveAlarmDetailsFromSQL(); //this is only relevant for alarms being edited
                if(mAdapter != null) mAdapter.notifyDataSetChanged();
            }
        });
        channelManager.refreshChannelData(channelRoosters);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        inject(BaseApplication.getRoosterApplicationComponent());

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
                return;
            }
        }
    }
}
