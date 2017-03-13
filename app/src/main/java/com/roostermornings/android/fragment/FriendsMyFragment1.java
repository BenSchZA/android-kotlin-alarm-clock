/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.ChannelsListAdapter;
import com.roostermornings.android.adapter.FriendsInviteListAdapter;
import com.roostermornings.android.adapter.FriendsMyListAdapter;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.LocalContacts;
import com.roostermornings.android.domain.NodeUser;
import com.roostermornings.android.domain.NodeUsers;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.domain.Users;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.MyContactsController;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FriendsMyFragment1.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FriendsMyFragment1#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendsMyFragment1 extends BaseFragment {

    protected static final String TAG = FriendsFragmentActivity.class.getSimpleName();

    ArrayList<User> mUsers = new ArrayList<>();
    private DatabaseReference mFriendsReference;
    private DatabaseReference mUserReference;

    private RecyclerView.Adapter mAdapter;

    @BindView(R.id.friendsMyListView)
    RecyclerView mRecyclerView;

    private OnFragmentInteractionListener mListener;

    public FriendsMyFragment1() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment FriendsMyFragment1.
     */
    public static FriendsMyFragment1 newInstance(String param1, String param2) {
        FriendsMyFragment1 fragment = new FriendsMyFragment1();
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
        View view = inflater.inflate(R.layout.fragment_friends_fragment1, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    //NB: bind ButterKnife to view and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new FriendsMyListAdapter(mUsers, getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
    }

    private void getFriends() {
        mFriendsReference = mDatabase
                .child("users").child(getFirebaseUser().getUid()).child("friends");

        ChildEventListener friendsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                mUsers.add(dataSnapshot.getValue(User.class));
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                User user = dataSnapshot.getValue(User.class);
                for (User oldUser:mUsers) {
                    if(oldUser.getUid().equals(user.getUid())){
                        mUsers.remove(oldUser);
                        mUsers.add(user);
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                mUsers.remove(dataSnapshot.getValue(User.class));
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                //TODO: Attempt to invoke virtual method 'android.content.res.Resources android.content.Context.getResources()' on a null object reference
                Toast.makeText(getContext(), "Failed to load user.",
                        Toast.LENGTH_SHORT).show();
            }
        };
        mFriendsReference.addChildEventListener(friendsListener);
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
        getFriends();

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

    //mListener.onFragmentInteraction(uri);

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
