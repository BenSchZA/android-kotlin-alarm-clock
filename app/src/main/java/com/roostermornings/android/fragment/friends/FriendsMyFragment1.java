/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.friends;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.adapter.FriendsMyListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.domain.Users;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.MyContactsController;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;

import butterknife.BindView;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

import static com.facebook.FacebookSdk.getApplicationContext;

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
    private String firebaseIdToken = "";

    private RecyclerView.Adapter mAdapter;

    private static int statusCode = -1;

    @BindView(R.id.friendsMyListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    private OnFragmentInteractionListener mListener;

    @Inject Context AppContext;
    @Inject @Nullable FirebaseUser firebaseUser;
    @Inject JSONPersistence jsonPersistence;
    @Inject MyContactsController myContactsController;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

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

        //Ensure check for Node complete reset
        statusCode = -1;

        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = initiate(inflater, R.layout.fragment_friends_fragment1, container, false);

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
                        retrieveMyFriends();
                    }
                }
        );

        if(!jsonPersistence.getFriends().isEmpty()) {
            mUsers = jsonPersistence.getFriends();
        } else if(checkInternetConnection()) {
            if(!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
        }

        //Listen for changes to friends node and refresh on change
        registerFriendsListener();

        return view;
    }

    public void manualSwipeRefresh() {
        if(swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
        retrieveMyFriends();
    }

    private void registerFriendsListener() {
        //Listen for changes to friends node

        DatabaseReference mFriendsReference = mDatabase
                .child("users").child(firebaseUser.getUid()).child("friends");
        mFriendsReference.keepSynced(true);

        ValueEventListener friendsListener = new ValueEventListener() {
            Boolean firstRun;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(firstRun == null) firstRun = true;
                if(!firstRun) {
                    manualSwipeRefresh();
                }
                firstRun = false;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        mFriendsReference.addValueEventListener(friendsListener);
    }

    //NB: bind ButterKnife to view and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Sort names alphabetically before notifying adapter
        sortNamesUsers(mUsers);
        mAdapter = new FriendsMyListAdapter(mUsers, getActivity());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(AppContext));
        mRecyclerView.setAdapter(mAdapter);

        retrieveMyFriends();
    }

    private void retrieveMyFriends() {

        if (!checkInternetConnection()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (firebaseUser == null) {
            Toaster.makeToast(getApplicationContext(), "Loading friends failed, please try again.", Toast.LENGTH_LONG).checkTastyToast();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        if("".equals(firebaseIdToken)) {
            firebaseUser.getToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            firebaseIdToken = task.getResult().getToken();

                            callNodeMyFriendsAPI();

                        } else {
                            // Handle error -> task.getException();
                            Toaster.makeToast(getApplicationContext(), "Loading friends failed, please try again.", Toast.LENGTH_LONG).checkTastyToast();
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });
        } else {
            callNodeMyFriendsAPI();
        }
    }

    private void callNodeMyFriendsAPI() {
        Call<Users> call = apiService().retrieveUserFriends(firebaseIdToken);

        call.enqueue(new Callback<Users>() {
            @Override
            public void onResponse(Response<Users> response,
                                   Retrofit retrofit) {

                statusCode = response.code();
                Users apiResponse = response.body();
                if(apiResponse.users == null) {
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }

                if (statusCode == 200) {

                    swipeRefreshLayout.setRefreshing(false);

                    mUsers.clear();
                    mUsers.addAll(apiResponse.users);
                    while(mUsers.remove(null));

                    //For each user, check if name appears in contacts, and allocate name
                    HashMap<String, String> numberNamePairs = new HashMap<>();
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            android.Manifest.permission.READ_CONTACTS)
                            == PackageManager.PERMISSION_GRANTED) {
                        //Get a map of contact numbers to names
                        numberNamePairs = myContactsController.getNumberNamePairs();
                        for (User user :
                                mUsers) {
                            if (numberNamePairs.containsKey(user.getCell_number())) {
                                user.setUser_name(numberNamePairs.get(user.getCell_number()));
                            }
                        }
                    }

                    sortNamesUsers(mUsers);
                    //Persist friends array to disk
                    jsonPersistence.setFriends(mUsers);
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.i(TAG, t.getLocalizedMessage()==null?"":t.getLocalizedMessage());
                Toaster.makeToast(getApplicationContext(), "Loading friends failed, please try again.", Toast.LENGTH_LONG).checkTastyToast();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        inject(((BaseApplication) AppContext).getRoosterApplicationComponent());

        getDatabaseReference();

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

    @Override
    public void onResume() {
        super.onResume();
    }

    public void searchRecyclerViewAdapter(String query) {
        //Filter contacts by CharSequence
        //Get reference to list adapter to access getFilter method
        ((FriendsMyListAdapter)mAdapter).refreshAll(mUsers);
        ((FriendsMyListAdapter)mAdapter).getFilter().filter(query);
    }

    public void notifyAdapter() {
        ((FriendsMyListAdapter)mAdapter).refreshAll(mUsers);
        mAdapter.notifyDataSetChanged();
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
}
