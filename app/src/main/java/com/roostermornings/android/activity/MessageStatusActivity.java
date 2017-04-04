/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MessageStatusListAdapter;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.OnClick;

public class MessageStatusActivity extends BaseActivity {

    public static final String TAG = MessageStatusActivity.class.getSimpleName();
    @BindView(R.id.message_statusListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.button_bar)
    LinearLayout buttonBarLayout;

    private DatabaseReference mSocialRoosterUploadsReference;
    private DatabaseReference mSocialRoosterQueueReference;

    ArrayList<SocialRooster> mRoosters = new ArrayList<>();
    Calendar calendar;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_message_status);

        setDayNight();

        mAdapter = new MessageStatusListAdapter(mRoosters, MessageStatusActivity.this, getApplication());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        calendar = Calendar.getInstance();

        mSocialRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                .child("social_rooster_uploads").child(getFirebaseUser().getUid());
        mSocialRoosterUploadsReference.keepSynced(true);

        ValueEventListener socialRoosterUploadsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    final SocialRooster socialRoosterU  = postSnapshot.getValue(SocialRooster.class);
                    if(socialRoosterU.getDate_uploaded() > (calendar.getTimeInMillis() - 2*Constants.TIME_MILLIS_1_DAY)) {
                        mRoosters.add(socialRoosterU);
                        mRoosters.get(mRoosters.indexOf(socialRoosterU)).setStatus(Constants.MESSAGE_STATUS_SENT);
                        if(socialRoosterU.getListened()) {
                            mRoosters.get(mRoosters.indexOf(socialRoosterU)).setStatus(Constants.MESSAGE_STATUS_RECEIVED);
                            notifyAdapter();
                            return;
                        }
                        notifyAdapter();

                        mSocialRoosterQueueReference = FirebaseDatabase.getInstance().getReference()
                                .child("social_rooster_queue").child(socialRoosterU.getReceiver_id()).child(socialRoosterU.getQueue_id());
                        mSocialRoosterQueueReference.keepSynced(true);

                        ValueEventListener socialRoosterQueueListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(!dataSnapshot.exists()) {
                                    mRoosters.get(mRoosters.indexOf(socialRoosterU)).setStatus(Constants.MESSAGE_STATUS_DELIVERED);
                                }
                                notifyAdapter();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                                if(BuildConfig.DEBUG) Toast.makeText(MessageStatusActivity.this, "Failed to load message status.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }; mSocialRoosterQueueReference.addValueEventListener(socialRoosterQueueListener);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                if(BuildConfig.DEBUG) Toast.makeText(MessageStatusActivity.this, "Failed to load message status.",
                        Toast.LENGTH_SHORT).show();
            }
        }; mSocialRoosterUploadsReference.addValueEventListener(socialRoosterUploadsListener);

        //Set toolbar title
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbarTitle.setText(getString(R.string.roosters));
    }

    private void notifyAdapter() {
        sortMessages(mRoosters);
        mAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.home_record_audio)
    public void recordNewAudio() {
        if (!checkInternetConnection()) return;
        startActivity(new Intent(MessageStatusActivity.this, NewAudioRecordActivity.class));
    }

    @OnClick(R.id.home_friends)
    public void manageFriends() {
        startActivity(new Intent(MessageStatusActivity.this, FriendsFragmentActivity.class));
    }

    @OnClick(R.id.home_my_alarms)
    public void manageAlarms() {
        startHomeActivity();
    }

    private void sortMessages(ArrayList<SocialRooster> socialRoosters){
        //Take arraylist and sort by date
        Collections.sort(socialRoosters, new Comparator<SocialRooster>() {
            @Override
            public int compare(SocialRooster lhs, SocialRooster rhs) {
                return rhs.getDate_uploaded().compareTo(lhs.getDate_uploaded());
            }
        });
    }
}
