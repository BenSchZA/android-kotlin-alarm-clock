/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.adapter.FriendsRequestListAdapter;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.fragment.base.BaseFragment;

import java.util.ArrayList;

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
    private DatabaseReference mUserReference;

    private RecyclerView.Adapter mAdapter;

    @BindView(R.id.friendsRequestListView)
    RecyclerView mRecyclerView;

    private OnFragmentInteractionListener mListener;

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
        //initialize(R.layout.fragment_friends_fragment3);

        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return initiate(inflater, R.layout.fragment_friends_fragment2, container, false);
    }

    //NB: bind ButterKnife to view and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new FriendsRequestListAdapter(mUsers, getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
    }

    private void getRequests() {
        mRequestsReference = mDatabase
                .child("friend_requests_received").child(getFirebaseUser().getUid());

        ValueEventListener friendsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    //Set notification icon for new request
                    ((FriendsFragmentActivity)getActivity()).setTabNotification(1, true);

                    mUsers.add(postSnapshot.getValue(Friend.class));
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                //TODO: Attempt to invoke virtual method 'android.content.res.Resources android.content.Context.getResources()' on a null object reference
                Toast.makeText(getContext(), "Failed to load user.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        mRequestsReference.addValueEventListener(friendsListener);

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

