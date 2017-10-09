/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
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

    private MediaPlayer mediaPlayer = new MediaPlayer();
    Future oneInstanceTaskFuture;

    private ChannelManager channelManager = new ChannelManager(this,this);

    private RecyclerView.Adapter mAdapter;

    @Inject BaseApplication AppContext;
    @Inject SharedPreferences sharedPreferences;
    @Inject JSONPersistence jsonPersistence;

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

                //Check if uses explor user prop has been set, and if not then create shared pref with default no
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
            channelRoosters.addAll(jsonPersistence.getChannelRoosters());
            mAdapter.notifyDataSetChanged();
        } else if(checkInternetConnection()) {
            if(!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
        }

        ChannelManager.Companion.setOnFlagChannelManagerDataListener(new ChannelManager.Companion.OnFlagChannelManagerDataListener() {
            @Override
            public void onChannelRoosterDataChanged(@NotNull ArrayList<ChannelRooster> freshChannelRoosters) {
                channelRoosters.clear();
                channelRoosters.addAll(freshChannelRoosters);
            }

            @Override
            public void onSyncFinished() {
                if(mAdapter != null) mAdapter.notifyDataSetChanged();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

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
                        onPause();
                        channelManager.refreshChannelData(channelRoosters);
                    }
                }
        );

        //Set volume rocker to alarm stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //Fetch content from Firebase
        channelManager.refreshChannelData(channelRoosters);
    }

    @Override
    public void onStart() {
        super.onStart();
        //Display notifications
        updateRoosterNotification();
        updateRequestNotification();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        if(oneInstanceTaskFuture != null) oneInstanceTaskFuture.cancel(true);
        clearChannelRoosterMediaChecks();
        mAdapter.notifyDataSetChanged();

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

    private void notifyDataSetChangedFromUIThread() {
        this.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                }
        );
    }

    public void streamChannelRooster(final ChannelRooster channelRooster) {
        //The oneInstanceTask creates a new thread, only if one doesn't exist already
        ExecutorService executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
                    new SynchronousQueue<Runnable>(),
                    new ThreadPoolExecutor.DiscardPolicy());

        //Check for an active internet connection - displays toast if none
        if(!checkInternetConnection()) {
            mediaPlayer.release();
            return;
        }
        //Do not proceed if active download in progress on current channelrooster
        if(channelRooster.isDownloading()) return;

        class oneInstanceTask implements Runnable {
            public void run() {
                StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                final StorageReference audioFileRef = mStorageRef.getStorage().getReferenceFromUrl(channelRooster.getAudio_file_url());

                channelRooster.setDownloading(true);
                channelRooster.setPlaying(false);
                notifyDataSetChangedFromUIThread();

                mediaPlayer = new MediaPlayer();

                audioFileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                {
                    @Override
                    public void onSuccess(Uri downloadUrl)
                    {
                        //The asynchronous nature of this task meant that even after reset, the download task would return and start the mediaplayer...
                        //Now by releasing the mediaplayer and initialising it before the async call we avoid this
                        if(mediaPlayer == null) return;
                        try {
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(downloadUrl.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                            return;
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                            return;
                        }
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mediaPlayer.start();

                                clearChannelRoosterMediaChecks();
                                channelRooster.setDownloading(false);
                                channelRooster.setPlaying(true);
                                notifyDataSetChangedFromUIThread();

                                //Set Firebase user prop for uses_explore
                                String sharedPrefUsesExplore = sharedPreferences.getString(
                                        FA.UserProp.uses_explore.shared_pref_uses_explore, "no");
                                if(sharedPrefUsesExplore.contains(FA.UserProp.uses_explore.no)) {
                                    sharedPreferences.edit().putString(FA.UserProp.uses_explore.shared_pref_uses_explore,
                                            FA.UserProp.uses_explore.started_explore_playback).apply();
                                    FA.SetUserProp(FA.UserProp.uses_explore.class, FA.UserProp.uses_explore.started_explore_playback);
                                }

                                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        clearChannelRoosterMediaChecks();
                                        notifyDataSetChangedFromUIThread();

                                        //Set Firebase user prop for uses_explore
                                        FA.SetUserProp(FA.UserProp.uses_explore.class, FA.UserProp.uses_explore.completed_explore_playback);
                                        sharedPreferences.edit().putString(FA.UserProp.uses_explore.shared_pref_uses_explore,
                                                FA.UserProp.uses_explore.completed_explore_playback).apply();
                                    }
                                });
                            }
                        });

                        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                            @Override
                            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                                Toaster.makeToast(AppContext, "Content streaming failed.", Toast.LENGTH_SHORT).checkTastyToast();
                                clearChannelRoosterMediaChecks();
                                notifyDataSetChangedFromUIThread();
                                return true;
                            }
                        });

                        mediaPlayer.prepareAsync();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        Toaster.makeToast(AppContext, "Content streaming failed.", Toast.LENGTH_SHORT).checkTastyToast();
                        clearChannelRoosterMediaChecks();
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        //Check if playing or not playing and handle threading + mediaplayer appropriately
        if(channelRooster.isPlaying()) {
            try {
                mediaPlayer.pause();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            //If a previous task exists, kill it
            if(oneInstanceTaskFuture != null) oneInstanceTaskFuture.cancel(true);
            clearChannelRoosterMediaChecks();
            channelRooster.setPaused(true);
            mAdapter.notifyDataSetChanged();
        } else if(channelRooster.isPaused() && mediaPlayer.getCurrentPosition() > 0) {
            clearChannelRoosterMediaChecks();
            try {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
                mediaPlayer.start();
                channelRooster.setDownloading(false);
                channelRooster.setPlaying(true);
                mAdapter.notifyDataSetChanged();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                channelRooster.setDownloading(false);
                channelRooster.setPlaying(false);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            try {
                mediaPlayer.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            //If a previous task exists, kill it
            if(oneInstanceTaskFuture != null) oneInstanceTaskFuture.cancel(true);
            clearChannelRoosterMediaChecks();
            mAdapter.notifyDataSetChanged();
            //Start a new task thread
            oneInstanceTaskFuture = executor.submit(new oneInstanceTask());
        }
    }

    private void clearChannelRoosterMediaChecks() {
        //Clear all audio load spinners and set to not playing
        for (ChannelRooster rooster :
                channelRoosters) {
            rooster.setDownloading(false);
            rooster.setPlaying(false);
            rooster.setPaused(false);
        }
    }
}
