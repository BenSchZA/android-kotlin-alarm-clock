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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.adapter.MessageStatusReceivedListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.keys.ViewType;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAudioQueueItem;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.MyContactsController;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;

public class MessageStatusReceivedFragment1 extends BaseFragment {

    @BindView(R.id.message_statusListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<DeviceAudioQueueItem> mRoosters = new ArrayList<>();

    private RecyclerView.Adapter mAdapter;
    public MessageStatusReceivedListAdapter mAdapterClass;
    private MessageStatusReceivedFragment1.OnFragmentInteractionListener mListener;

    String fragmentType = "";

    @Inject
    Context AppContext;
    @Inject @Nullable
    FirebaseUser firebaseUser;
    @Inject
    JSONPersistence jsonPersistence;
    @Inject
    MyContactsController myContactsController;
    @Inject
    AudioTableManager audioTableManager;

    public MessageStatusReceivedFragment1() {
    }

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        inject(BaseApplication.Companion.getRoosterApplicationComponent());

        if (context instanceof MessageStatusReceivedFragment1.OnFragmentInteractionListener) {
            mListener = (MessageStatusReceivedFragment1.OnFragmentInteractionListener) context;
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
        mAdapter = new MessageStatusReceivedListAdapter(mRoosters, getActivity(), fragmentType);
        mAdapterClass = (MessageStatusReceivedListAdapter)mAdapter;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(AppContext));
        mRecyclerView.setAdapter(mAdapter);

        //Retrieve DeviceAudioQueueItems to display
        retrieveSocialAudioItems();

        //Log how many favourites the user has
        if(fragmentType.equals(ViewType.MESSAGE_STATUS_RECEIVED_FRAGMENT_FAVOURITE.name())) {
            FA.SetUserProp(FA.UserProp.user_favourites.class, String.valueOf(mRoosters.size()));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            fragmentType = bundle.getString(ViewType.MESSAGE_STATUS_RECEIVED_FRAGMENT.name());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = initiate(inflater, R.layout.fragment_message_status_1, container, false);

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
                        retrieveSocialAudioItems();
                        if(mAdapterClass != null) mAdapterClass.resetMediaPlayer();
                    }
                }
        );

        return view;
    }

    private void retrieveSocialAudioItems() {
        if(fragmentType.equals(ViewType.MESSAGE_STATUS_RECEIVED_FRAGMENT_TODAY.name())) {
            mRoosters = audioTableManager.extractTodaySocialAudioFiles();
            mRoosters.addAll(audioTableManager.extractTodayChannelAudioFiles());
            //Sort names alphabetically before notifying adapter
            sortDeviceAudioQueueItems(mRoosters);
            notifyAdapter();
            swipeRefreshLayout.setRefreshing(false);
        } else {
            mRoosters = audioTableManager.extractFavouriteSocialAudioFiles();
            mRoosters.addAll(audioTableManager.extractFavouriteChannelAudioFiles());
            //Sort names alphabetically before notifying adapter
            sortDeviceAudioQueueItems(mRoosters);
            notifyAdapter();
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    public void manualSwipeRefresh() {
        if(swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
        retrieveSocialAudioItems();
    }

    public void searchRecyclerViewAdapter(String query) {
        //Filter contacts by CharSequence
        //Get reference to list adapter to access getFilter method
        ((MessageStatusReceivedListAdapter)mAdapter).refreshAll(mRoosters);
        ((MessageStatusReceivedListAdapter)mAdapter).getFilter().filter(query);
    }

    public void notifyAdapter() {
        ((MessageStatusReceivedListAdapter)mAdapter).refreshAll(mRoosters);
    }
}
