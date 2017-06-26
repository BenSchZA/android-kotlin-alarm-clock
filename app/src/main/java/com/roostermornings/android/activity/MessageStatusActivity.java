/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MessageStatusListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import javax.inject.Inject;

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
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    private DatabaseReference mSocialRoosterUploadsReference;
    private DatabaseReference mSocialRoosterQueueReference;

    ArrayList<SocialRooster> mRoosters = new ArrayList<>();
    Calendar calendar;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Inject BaseApplication baseApplication;
    @Inject FirebaseUser firebaseUser;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_message_status);
        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

        setDayNightTheme();
        setButtonBarSelection();

        mAdapter = new MessageStatusListAdapter(mRoosters, MessageStatusActivity.this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        calendar = Calendar.getInstance();

        swipeRefreshLayout.setRefreshing(true);
        /*
        * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
        * performs a swipe-to-refresh gesture.
        */
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        //Reload adapter data and set message status, set listener for new data
                        updateMessageStatus();
                    }
                }
        );

        //Reload adapter data and set message status, set listener for new data
        updateMessageStatus();

        //Set toolbar title
        setupToolbar(toolbarTitle, getString(R.string.message_status_activity_title));
    }

    @Override
    public void onStart() {
        super.onStart();
        //Display notifications
        updateRoosterNotification();
        updateRequestNotification();
    }

    private void updateMessageStatus() {
        mSocialRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                .child("social_rooster_uploads").child(firebaseUser.getUid());
        mSocialRoosterUploadsReference.keepSynced(true);

        final ChildEventListener socialRoosterUploadsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final SocialRooster socialRoosterU = dataSnapshot.getValue(SocialRooster.class);
                for (SocialRooster item: mRoosters) if(item.getQueue_id().equals(socialRoosterU.getQueue_id())) {
                    mRoosters.remove(item);
                    break;
                }
                processSocialRoosterUploadsItem(socialRoosterU);
                notifyAdapter();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                final SocialRooster socialRoosterU = dataSnapshot.getValue(SocialRooster.class);
                for (SocialRooster item: mRoosters) if(item.getQueue_id().equals(socialRoosterU.getQueue_id())) {
                    mRoosters.remove(item);
                    break;
                }
                processSocialRoosterUploadsItem(socialRoosterU);
                notifyAdapter();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                final SocialRooster socialRoosterU = dataSnapshot.getValue(SocialRooster.class);
                for (SocialRooster item: mRoosters) if(item.getQueue_id().equals(socialRoosterU.getQueue_id())) {
                    mRoosters.remove(item);
                    break;
                }
                notifyAdapter();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                notifyAdapter();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mSocialRoosterUploadsReference.addChildEventListener(socialRoosterUploadsListener);

        //https://stackoverflow.com/questions/34530566/find-out-if-child-event-listener-on-firebase-completely-load-all-data
        //Value events are always triggered last and are guaranteed to contain updates from any other events which occurred before that snapshot was taken.
        mSocialRoosterUploadsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void processSocialRoosterUploadsItem(final SocialRooster socialRoosterU) {
        long dateUploaded;
        try {
            dateUploaded = socialRoosterU.getDate_uploaded();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }
        if (dateUploaded > (calendar.getTimeInMillis() - 2 * Constants.TIME_MILLIS_1_DAY)) {
            //Clear old entries on change
            mRoosters.add(socialRoosterU);
            if(mRoosters.indexOf(socialRoosterU) > -1) mRoosters.get(mRoosters.indexOf(socialRoosterU)).setStatus(Constants.MESSAGE_STATUS_SENT);
            if (socialRoosterU.getListened()) {
                if(mRoosters.indexOf(socialRoosterU) > -1) mRoosters.get(mRoosters.indexOf(socialRoosterU)).setStatus(Constants.MESSAGE_STATUS_RECEIVED);
                notifyAdapter();
            } else {
                mSocialRoosterQueueReference = FirebaseDatabase.getInstance().getReference()
                        .child("social_rooster_queue").child(socialRoosterU.getReceiver_id()).child(socialRoosterU.getQueue_id());
                mSocialRoosterQueueReference.keepSynced(true);

                ValueEventListener socialRoosterQueueListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            if(mRoosters.indexOf(socialRoosterU) > -1) mRoosters.get(mRoosters.indexOf(socialRoosterU)).setStatus(Constants.MESSAGE_STATUS_DELIVERED);
                            notifyAdapter();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                        if (BuildConfig.DEBUG)
                            Toaster.makeToast(MessageStatusActivity.this, "Failed to load message status.",
                                    Toast.LENGTH_SHORT).checkTastyToast();
                    }
                };
                mSocialRoosterQueueReference.addValueEventListener(socialRoosterQueueListener);
            }
        }
    }

    private void notifyAdapter() {
        sortMessages(mRoosters);
        mAdapter.notifyDataSetChanged();
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
