/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
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
import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.DiscoverListAdapter;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.fragment.new_alarm.NewAlarmFragment2;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmTableManager;
import com.roostermornings.android.util.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;

import static com.roostermornings.android.BaseApplication.AppContext;

public class DiscoverFragmentActivity extends BaseActivity implements DiscoverListAdapter.DiscoverAudioSampleInterface {

    @BindView(R.id.discoverListView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.button_bar)
    LinearLayout buttonBarLayout;

    public static final String TAG = DiscoverFragmentActivity.class.getSimpleName();

    private DatabaseReference mChannelsReference;
    private ArrayList<ChannelRooster> channelRoosters = new ArrayList<>();
    private Map<Integer, List<ChannelRooster>> channelRoosterMap = new TreeMap<>(Collections.reverseOrder());

    private final MediaPlayer mediaPlayer = new MediaPlayer();

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Inject DeviceAlarmTableManager deviceAlarmTableManager;
    @Inject AudioTableManager audioTableManager;
    @Inject BaseApplication AppContext;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_discover);

        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

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
                setupToolbar(toolbarTitle, getString(R.string.discover));

                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                mRecyclerView.setHasFixedSize(true);
                //Set up adapter for monitoring channelrooster objects
                mAdapter = new DiscoverListAdapter(channelRoosters, DiscoverFragmentActivity.this);
                //Use a linear layout manager
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(context);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mAdapter);
            }
        }.run();

        mChannelsReference = FirebaseDatabase.getInstance().getReference()
                .child("channels");
        mChannelsReference.keepSynced(true);

        new Thread() {
            @Override
            public void run() {
                ValueEventListener channelsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            final Channel channel = postSnapshot.getValue(Channel.class);

                            if (channel.isActive()) {
                                if(channel.isNew_alarms_start_at_first_iteration()) {
                                    Integer iteration = deviceAlarmTableManager.getChannelStoryIteration(channel.getUid());
                                    if(iteration == null || iteration <= 0) iteration = 1;
                                    getChannelRoosterData(channel, iteration);
                                } else {
                                    Integer iteration = channel.getCurrent_rooster_cycle_iteration();
                                    if(iteration == null || iteration <= 0) iteration = 1;
                                    getChannelRoosterData(channel, iteration);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                        Toast.makeText(AppContext, "Failed to load channel.", Toast.LENGTH_SHORT).show();
                    }
                };
                mChannelsReference.addValueEventListener(channelsListener);
            }
        }.run();
    }

    @Override
    public void onStop() {
        super.onStop();
        //Delete all files not contained in SQL db
        File[] files = getCacheDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(Constants.FILENAME_PREFIX_ROOSTER_EXAMPLE_CONTENT);
            }
        });
        ArrayList<String> audioFileNames = audioTableManager.extractAllAudioFileNames();
        if(audioFileNames != null) {
            for (File file :
                    files) {
                if (!audioFileNames.contains(file.getName()))
                    file.delete();
            }
        }
    }

    private void getChannelRoosterData(final Channel channel, final Integer iteration) {
        final DatabaseReference channelRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                .child("channel_rooster_uploads").child(channel.getUid());
        //Ensure latest data is pulled
        channelRoosterUploadsReference.keepSynced(true);

        ValueEventListener channelRoosterUploadsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TreeMap<Integer,ChannelRooster> channelIterationMap = new TreeMap<>();
                //Check if node has children i.e. channelId content exists
                if(dataSnapshot.getChildrenCount() == 0) return;
                //Iterate over all content children
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    ChannelRooster channelRooster = postSnapshot.getValue(ChannelRooster.class);

                    //Set channelrooster display picture to banner image & description to channel description
                    //In future channelrooster specific details will be shown in discover page
                    channelRooster.setChannel_photo(channel.getPhoto());
                    channelRooster.setChannel_description(channel.getDescription());

                    if(channelRooster.isActive() && (channelRooster.getRooster_cycle_iteration() != iteration)) {
                        channelIterationMap.put(channelRooster.getRooster_cycle_iteration(), channelRooster);
                    } else if(channelRooster.isActive()) {
                        channelRooster.setSelected(false);
                        //This method allows multiple objects per key
                        if(channelRoosterMap.containsKey(channel.getPriority())) {
                            channelRoosterMap.get(channel.getPriority()).add(channelRooster);
                        } else {
                            List<ChannelRooster> values = new ArrayList<>();
                            values.add(channelRooster);
                            channelRoosterMap.put(channel.getPriority(), values);
                        }
                        refreshChannelRoosters();
                        return;
                    }
                }
                if(!channelIterationMap.isEmpty() && iteration != null) {
                    findNextValidChannelRooster(channelIterationMap, channel, iteration);
                }
                refreshChannelRoosters();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        channelRoosterUploadsReference.addListenerForSingleValueEvent(channelRoosterUploadsListener);
    }

    private void refreshChannelRoosters() {
        new Thread() {
            @Override
            public void run() {
                //TreeMap ensures unique and allows sorting by priority! How cool is that?
                channelRoosters.clear();
                //This method allows us to have multiple objects per priority key
                List<ChannelRooster> values = new ArrayList<>();
                for(List<ChannelRooster> channelRoosterList : channelRoosterMap.values()) {
                    values.addAll(channelRoosterList);
                }
                if(!values.isEmpty()) channelRoosters.addAll(values);
                mAdapter.notifyDataSetChanged();
            }
        }.run();
    }

    private void findNextValidChannelRooster(final TreeMap<Integer,ChannelRooster> channelIterationMap, final Channel channel, final Integer iteration) {
        new Thread() {
            @Override
            public void run() {
                //Check head and tail of naturally sorted TreeMap for next valid channel content
                SortedMap<Integer,ChannelRooster> tailMap = channelIterationMap.tailMap(iteration);
                SortedMap<Integer,ChannelRooster> headMap = channelIterationMap.headMap(iteration);
                if(!tailMap.isEmpty()) {
                    //User is starting story at next valid entry
                    //Set SQL entry for iteration to current valid story iteration, to be incremented on play
                    deviceAlarmTableManager.setChannelStoryIteration(channel.getUid(), tailMap.firstKey());
                    //Retrieve channel audio
                    ChannelRooster channelRooster = channelIterationMap.get(tailMap.firstKey());
                    channelRooster.setSelected(false);
                    //This method allows multiple objects per key
                    if(channelRoosterMap.containsKey(channel.getPriority())) {
                        channelRoosterMap.get(channel.getPriority()).add(channelRooster);
                    } else {
                        List<ChannelRooster> values = new ArrayList<>();
                        values.add(channelRooster);
                        channelRoosterMap.put(channel.getPriority(), values);
                    }
                }
                else if(!headMap.isEmpty()) {
                    //User is starting story from beginning again, at valid entry
                    //Set SQL entry for iteration to current valid story iteration, to be incremented on play
                    deviceAlarmTableManager.setChannelStoryIteration(channel.getUid(), headMap.firstKey());
                    //Retrieve channel audio
                    ChannelRooster channelRooster = channelIterationMap.get(headMap.firstKey());
                    channelRooster.setSelected(false);
                    //This method allows multiple objects per key
                    if(channelRoosterMap.containsKey(channel.getPriority())) {
                        channelRoosterMap.get(channel.getPriority()).add(channelRooster);
                    } else {
                        List<ChannelRooster> values = new ArrayList<>();
                        values.add(channelRooster);
                        channelRoosterMap.put(channel.getPriority(), values);
                    }
                }
            }
        }.run();
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
        Future oneInstanceTaskFuture;

        if(channelRooster.isDownloading()) return;

        class oneInstanceTask implements Runnable {
            public void run() {
                StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
                final StorageReference audioFileRef = mStorageRef.getStorage().getReferenceFromUrl(channelRooster.getAudio_file_url());

                channelRoosters.get(channelRoosters.indexOf(channelRooster)).setDownloading(true);
                channelRoosters.get(channelRoosters.indexOf(channelRooster)).setPlaying(false);
                notifyDataSetChangedFromUIThread();

                audioFileRef.getBytes(Constants.MAX_ROOSTER_FILE_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {

                        try {
                            File outputDir = getCacheDir(); // context being the Activity pointer
                            File tempFile = new File(outputDir.getAbsolutePath() + Constants.FILENAME_PREFIX_ROOSTER_EXAMPLE_CONTENT + ".3gp");
                            tempFile.deleteOnExit();
                            FileOutputStream outputStream = new FileOutputStream(tempFile);
                            outputStream.write(bytes);
                            outputStream.close();

                            if (BuildConfig.DEBUG)
                                Toast.makeText(AppContext, "successfully downloaded", Toast.LENGTH_SHORT).show();

                            FileInputStream inputStream = new FileInputStream(tempFile);

                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(inputStream.getFD());
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mediaPlayer.start();

                                    for (ChannelRooster rooster :
                                            channelRoosters) {
                                        rooster.setDownloading(false);
                                        rooster.setPlaying(false);
                                    }
                                    channelRoosters.get(channelRoosters.indexOf(channelRooster)).setDownloading(false);
                                    channelRoosters.get(channelRoosters.indexOf(channelRooster)).setPlaying(true);
                                    notifyDataSetChangedFromUIThread();

                                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mp) {
                                            channelRoosters.get(channelRoosters.indexOf(channelRooster)).setDownloading(false);
                                            channelRoosters.get(channelRoosters.indexOf(channelRooster)).setPlaying(false);
                                            notifyDataSetChangedFromUIThread();
                                        }
                                    });
                                }
                            });

                            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                @Override
                                public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                                    Toast.makeText(AppContext, "Content streaming failed.", Toast.LENGTH_SHORT).show();
                                    channelRoosters.get(channelRoosters.indexOf(channelRooster)).setDownloading(false);
                                    channelRoosters.get(channelRoosters.indexOf(channelRooster)).setPlaying(false);
                                    notifyDataSetChangedFromUIThread();
                                    return true;
                                }
                            });

                            mediaPlayer.prepareAsync();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        Toast.makeText(AppContext, "Content streaming failed.", Toast.LENGTH_SHORT).show();
                        channelRoosters.get(channelRoosters.indexOf(channelRooster)).setDownloading(false);
                        channelRoosters.get(channelRoosters.indexOf(channelRooster)).setPlaying(false);
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
        oneInstanceTaskFuture = executor.submit(new oneInstanceTask());

        //Reset GUI playing/downloading
        if(channelRooster.isPlaying()) {
            oneInstanceTaskFuture.cancel(true);
            mediaPlayer.reset();
            for (ChannelRooster rooster :
                    channelRoosters) {
                rooster.setDownloading(false);
                rooster.setPlaying(false);
            }
            mAdapter.notifyDataSetChanged();
        } else {
            oneInstanceTaskFuture.cancel(true);
            mediaPlayer.reset();
            for (ChannelRooster rooster :
                    channelRoosters) {
                rooster.setDownloading(false);
                rooster.setPlaying(false);
            }
            mAdapter.notifyDataSetChanged();
            executor.submit(new oneInstanceTask());
        }
    }
}