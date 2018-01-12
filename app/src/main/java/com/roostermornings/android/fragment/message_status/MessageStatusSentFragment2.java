/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.message_status;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.adapter.MessageStatusSentListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.database.SocialRooster;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.MyContactsController;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;
import java.util.Calendar;

import javax.inject.Inject;

import butterknife.BindView;

public class MessageStatusSentFragment2 extends BaseFragment {

    public static final String TAG = MessageStatusSentFragment2.class.getSimpleName();

    @BindView(R.id.message_statusListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<SocialRooster> mRoosters = new ArrayList<>();

    private DatabaseReference mSocialRoosterUploadsReference;
    private DatabaseReference mSocialRoosterQueueReference;

    private RecyclerView.Adapter mAdapter;
    private MessageStatusSentFragment2.OnFragmentInteractionListener mListener;
    Calendar calendar;

    @Inject
    Context AppContext;
    @Inject @Nullable
    FirebaseUser firebaseUser;
    @Inject
    JSONPersistence jsonPersistence;
    @Inject
    MyContactsController myContactsController;
    @Inject DatabaseReference firebaseDatabaseReference;

    public MessageStatusSentFragment2() {
    }

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        inject(BaseApplication.getRoosterApplicationComponent());

        if (context instanceof MessageStatusSentFragment2.OnFragmentInteractionListener) {
            mListener = (MessageStatusSentFragment2.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    //NB: bind ButterKnife to activityContentView and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Sort names alphabetically before notifying adapter
        sortSocialRoosters(mRoosters);
        mAdapter = new MessageStatusSentListAdapter(mRoosters, getActivity());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(AppContext));
        mRecyclerView.setAdapter(mAdapter);

        //Reload adapter data and set message status, set listener for new data
        updateMessageStatus();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calendar = Calendar.getInstance();

        Bundle bundle = getArguments();
        if (bundle != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = initiate(inflater, R.layout.fragment_message_status_1, container, false);

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
                        updateMessageStatus();
                    }
                }
        );

        return view;
    }

    private void updateMessageStatus() {
        if(firebaseUser == null) {
            Toaster.makeToast(getContext(), "Couldn't load user. Try reconnect to the internet and try again.", Toast.LENGTH_SHORT);
            return;
        }

        mSocialRoosterUploadsReference = firebaseDatabaseReference
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
                mSocialRoosterQueueReference = firebaseDatabaseReference
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
                            Toaster.makeToast(getContext(), "Failed to load message status.",
                                    Toast.LENGTH_SHORT).checkTastyToast();
                    }
                };
                mSocialRoosterQueueReference.addValueEventListener(socialRoosterQueueListener);
            }
        }
    }

    public void manualSwipeRefresh() {
        if(swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
        updateMessageStatus();
    }

    public void searchRecyclerViewAdapter(String query) {
        //Filter contacts by CharSequence
        //Get reference to list adapter to access getFilter method
        ((MessageStatusSentListAdapter)mAdapter).refreshAll(mRoosters);
        ((MessageStatusSentListAdapter)mAdapter).getFilter().filter(query);
    }

    public void notifyAdapter() {
        sortSocialRoosters(mRoosters);
        mAdapter.notifyDataSetChanged();
    }

}
