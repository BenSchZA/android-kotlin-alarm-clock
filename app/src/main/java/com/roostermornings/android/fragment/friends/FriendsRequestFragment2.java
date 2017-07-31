/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.friends;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.adapter.FriendsRequestListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.MyContactsController;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FriendsRequestFragment2.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FriendsRequestFragment2#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendsRequestFragment2 extends BaseFragment {

    protected static final String TAG = FriendsFragmentActivity.class.getSimpleName();

    ArrayList<Friend> mUsers = new ArrayList<>();
    private DatabaseReference mRequestsReference;

    private RecyclerView.Adapter mAdapter;

    @BindView(R.id.friendsRequestListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    private OnFragmentInteractionListener mListener;

    @Inject Context AppContext;
    @Inject @Nullable FirebaseUser firebaseUser;
    @Inject MyContactsController myContactsController;
    @Inject JSONPersistence jsonPersistence;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    public FriendsRequestFragment2() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment FriendsRequestFragment2.
     */
    public static FriendsRequestFragment2 newInstance(String param1, String param2) {
        FriendsRequestFragment2 fragment = new FriendsRequestFragment2();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = initiate(inflater, R.layout.fragment_friends_fragment2, container, false);

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
                        getDatabaseReference();
                        getRequests();
                    }
                }
        );

        return view;
    }

    //NB: bind ButterKnife to view and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sortNamesFriends(mUsers);
        mAdapter = new FriendsRequestListAdapter(mUsers);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(AppContext));
        mRecyclerView.setAdapter(mAdapter);
    }

    public void manualSwipeRefresh() {
        if(swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        getDatabaseReference();
        getRequests();
    }

    private void getRequests() {
        mRequestsReference = mDatabase
                .child("friend_requests_received").child(firebaseUser.getUid());
        mRequestsReference.keepSynced(true);

        ValueEventListener friendsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Clear before repopulating
                mUsers.clear();

                //Get a map of number name pairs from my contacts
                //For each user, check if name appears in contacts, and allocate name
                HashMap<String, String> numberNamePairs = new HashMap<>();
                if (ContextCompat.checkSelfPermission(AppContext,
                        android.Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED) {
                    //Get a map of contact numbers to names
                    numberNamePairs = myContactsController.getNumberNamePairs();
                }

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Friend user = postSnapshot.getValue(Friend.class);

                    //For each user, check if name appears in contacts, and allocate name
                    if(numberNamePairs.containsKey(user.getCell_number())) {
                        user.setUser_name(numberNamePairs.get(user.getCell_number()));
                    }

                    mUsers.add(user);
                }

                //Sort names alphabetically before notifying adapter
                sortNamesFriends(mUsers);
                mAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toaster.makeToast(AppContext, "Failed to load user.", Toast.LENGTH_SHORT).checkTastyToast();
            }
        };
        mRequestsReference.addListenerForSingleValueEvent(friendsListener);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        inject(BaseApplication.getRoosterApplicationComponent());

        getDatabaseReference();
        getRequests();

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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

    public void searchRecyclerViewAdapter(String query) {
        //Filter contacts by CharSequence
        //Get reference to list adapter to access getFilter method
        ((FriendsRequestListAdapter)mAdapter).refreshAll(mUsers);
        ((FriendsRequestListAdapter)mAdapter).getFilter().filter(query);
    }

    public void notifyAdapter() {
        ((FriendsRequestListAdapter)mAdapter).refreshAll(mUsers);
        mAdapter.notifyDataSetChanged();
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

    @Override
    public void onResume() {
        super.onResume();
    }
}

