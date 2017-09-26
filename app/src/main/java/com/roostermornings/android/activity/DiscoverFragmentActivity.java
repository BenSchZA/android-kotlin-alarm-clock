/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.DiscoverListAdapter;
import com.roostermornings.android.channels.ChannelManager;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.service.AudioService;
import com.roostermornings.android.service.ExploreService;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.geolocation.GeoHashUtils;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.Toaster;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;

public class DiscoverFragmentActivity extends BaseActivity implements DiscoverListAdapter.DiscoverAudioSampleInterface {

    @BindView(R.id.discoverListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.button_bar)
    LinearLayout buttonBarLayout;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    public static final String TAG = DiscoverFragmentActivity.class.getSimpleName();

    private ArrayList<ChannelRooster> channelRoosters = new ArrayList<>();

    private RecyclerView.Adapter mAdapter;

    @Inject BaseApplication AppContext;
    @Inject JSONPersistence jsonPersistence;
    @Inject
    SharedPreferences sharedPreferences;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_discover);

        inject(BaseApplication.getRoosterApplicationComponent());

        //Notify user of no internet connection
        checkInternetConnection();

        //Final context to be used in threads
        final Context context = this;

        //UI setup thread
        new Thread() {
            @Override
            public void run() {
                //Setup day/night theme selection (based on settings, and time)
                setDayNightTheme();
                //Set highlighting of button bar
                setButtonBarSelection();
                //Set toolbar title
                Toolbar toolbar = setupToolbar(toolbarTitle, getString(R.string.discover));
                if(toolbar != null) {
                    toolbar.setNavigationIcon(R.drawable.md_nav_back);
                    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startHomeActivity();
                        }
                    });
                }

                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                mRecyclerView.setHasFixedSize(true);
                //Set up adapter for monitoring channelrooster objects
                mAdapter = new DiscoverListAdapter(channelRoosters, DiscoverFragmentActivity.this);
                //Use a linear layout manager
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mAdapter);

                //Check if uses explore user prop has been set, and if not then create shared pref with default no
                String sharedPrefUsesExplore = sharedPreferences.getString(
                        FA.UserProp.uses_explore.shared_pref_uses_explore, "null");
                if(!sharedPrefUsesExplore.contains(FA.UserProp.uses_explore.no)
                && !sharedPrefUsesExplore.contains(FA.UserProp.uses_explore.started_explore_playback)
                && !sharedPrefUsesExplore.contains(FA.UserProp.uses_explore.completed_explore_playback)) {
                    sharedPreferences.edit().putString(FA.UserProp.uses_explore.shared_pref_uses_explore,
                            FA.UserProp.uses_explore.no).apply();
                    FA.SetUserProp(FA.UserProp.uses_explore.class, FA.UserProp.uses_explore.no);
                }
            }
        }.run();

        if(!jsonPersistence.getChannelRoosters().isEmpty()) {
            channelRoosters.clear();
            channelRoosters.addAll(jsonPersistence.getChannelRoosters());
            mAdapter.notifyDataSetChanged();
        } else if(checkInternetConnection()) {
            if(!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
        }

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
                        if(mBound)
                            mExploreService.refreshData();
                    }
                }
        );

        //Set volume rocker to alarm stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onStart() {
        super.onStart();
        //Display notifications
        updateRoosterNotification();
        updateRequestNotification();

        //Bind to audio service to allow playback and pausing of alarms in background
        Intent intent = new Intent(this, ExploreService.class);
        startService(intent);
        //0 indicates that service should not be restarted
        bindService(intent, mExploreServiceConnection, 0);
    }

    ExploreService mExploreService;
    private boolean mBound = false;

    private ServiceConnection mExploreServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Because we have bound to an explicit
            // service that is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            ExploreService.LocalBinder binder = (ExploreService.LocalBinder) service;
            mExploreService = binder.getService();
            mBound = true;

            mExploreService.refreshData();
            mExploreService.startExploreStatusBarPlayer();

            ExploreService.setOnFlagExploreServiceEventListener(new ExploreService.OnFlagExploreServiceEventListener() {
                @Override
                public void onEventSeekForward() {

                }

                @Override
                public void onEventSeekBackward() {

                }

                @Override
                public void onEventPlayPause(ChannelRooster channelRooster) {

                }

                @Override
                public void onEventNext() {

                }

                @Override
                public void onEventPrevious() {

                }

                @Override
                public void onEventStop() {

                }

                @Override
                public void onEventDestroy() {
                    if(mAdapter != null)
                        mAdapter.notifyDataSetChanged();
                }

                @Override
                public void updateSingleUIEntry(int index, ChannelRooster tempChannelRooster) {
                    if(channelRoosters.get(index) != null) {
                        channelRoosters.remove(index);
                        channelRoosters.add(index, tempChannelRooster);
                        if(mAdapter != null)
                            mAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void updateUI(ArrayList<ChannelRooster> tempChannelRoosters) {
                    if(!channelRoosters.equals(tempChannelRoosters)) {
                        channelRoosters.clear();
                        channelRoosters.addAll(tempChannelRoosters);
                        if(mAdapter != null)
                            mAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void clearMediaChecks() {
                }

                @Override
                public void refreshUI() {
                    if(mAdapter != null)
                        mAdapter.notifyDataSetChanged();
                }

                @Override
                public void setRefreshing(boolean refreshing) {
                    swipeRefreshLayout.setRefreshing(refreshing);
                }
            });
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    @Override
    public void onPause() {
        super.onPause();

        //Persist channel roosters for seamless loading
        jsonPersistence.setChannelRoosters(channelRoosters);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            handleSearch(query);
        }
    }

    private void handleSearch(String query) {
        ((DiscoverListAdapter)mAdapter).refreshAll(channelRoosters);
        ((DiscoverListAdapter)mAdapter).getFilter().filter(query);
    }

    public void notifyAdapter() {
        ((DiscoverListAdapter)mAdapter).refreshAll(channelRoosters);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_explore, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search_explore).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        //When searchView is closed, refresh data
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View arg0) {
                // search was detached/closed
                notifyAdapter();
            }

            @Override
            public void onViewAttachedToWindow(View arg0) {
                // search was opened
            }
        });

        return true;
    }

    public void streamChannelRooster(ChannelRooster channelRooster) {
        if(mExploreService != null) mExploreService.streamChannelRooster(channelRooster);
    }
}
