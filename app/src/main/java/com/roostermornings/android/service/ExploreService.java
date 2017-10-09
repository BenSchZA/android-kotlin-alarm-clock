/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RemoteViews;
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
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.util.InternetHelper;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.Toaster;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

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

public class ExploreService extends Service {

    public static final String TAG = ExploreService.class.getSimpleName();

    // Binder given to clients
    private final IBinder mBinder = new ExploreService.LocalBinder();

    public static boolean mRunning = false;

    private static final String NOTIFICATION_ID_CHANNEL = "ExploreService_NOTIFICATION_ID_CHANNEL";
    private static final int NOTIFICATION_ID = 2017; //Year of the Rooster

    //Extra events
    private static final String EXTRA_EXPLORE_NOTIFICATION_EVENT = "EXTRA_EXPLORE_NOTIFICATION_EVENT";
    private static final String EVENT_SEEK = "EVENT_SEEK";
    private static final String EVENT_PLAYPAUSE = "EVENT_PLAYPAUSE";
    private static final String EVENT_STOP = "EVENT_STOP";
    private static final String EVENT_PICTURE = "EVENT_PICTURE";
    private static final String EVENT_TITLE = "EVENT_TITLE";
    private static final String EVENT_DESTROY = "EVENT_DESTROY";

    Notification notification;
    NotificationCompat.Builder notificationBuilder;
    NotificationManager notificationManager;
    RemoteViews remoteViews;

    private DatabaseReference mChannelsReference;
    private DatabaseReference mChannelRoostersReference;
    private Map<Integer, List<ChannelRooster>> channelRoosterMap = new TreeMap<>(Collections.reverseOrder());
    private HashMap<Channel, Integer> channelIterationMap = new HashMap<>();
    private ArrayList<ChannelRooster> channelRoosters = new ArrayList<>();
    Future oneInstanceTaskFuture;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private ChannelRooster currentlyPlaying;

    @Inject
    SharedPreferences sharedPreferences;
    @Inject BaseApplication AppContext;
    @Inject
    JSONPersistence jsonPersistence;

    private static OnFlagExploreServiceEventListener onFlagExploreServiceEventListener;

    public interface OnFlagExploreServiceEventListener {
        void onEventSeekForward();
        void onEventSeekBackward();
        void onEventPlayPause(ChannelRooster channelRooster);
        void onEventNext();
        void onEventPrevious();
        void onEventStop();
        void onEventDestroy();
        void updateSingleUIEntry(int index, ChannelRooster tempChannelRooster);
        void updateUI(ArrayList<ChannelRooster> tempChannelRoosters);
        void clearMediaChecks();
        void refreshUI();
        void setRefreshing(boolean refreshing);
    }

    public static void setOnFlagExploreServiceEventListener(OnFlagExploreServiceEventListener listener) {
        onFlagExploreServiceEventListener = listener;
    }

    public ExploreService() {
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public ExploreService getService() {
            // Return this instance of ExploreService so clients can call public methods
            return ExploreService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRunning = false;

        ExploreService.setOnFlagExploreServiceEventListener(new OnFlagExploreServiceEventListener() {
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

            }

            @Override
            public void updateSingleUIEntry(int index, ChannelRooster tempChannelRooster) {

            }

            @Override
            public void updateUI(ArrayList<ChannelRooster> tempChannelRoosters) {

            }

            @Override
            public void clearMediaChecks() {
                clearChannelRoosterMediaChecks();
            }

            @Override
            public void refreshUI() {

            }

            @Override
            public void setRefreshing(boolean refreshing) {

            }
        });

        BaseApplication.getRoosterApplicationComponent().inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null && intent.getExtras() != null) {
            switch (intent.getStringExtra(EXTRA_EXPLORE_NOTIFICATION_EVENT)) {
                case EVENT_PLAYPAUSE:
                    if(currentlyPlaying != null) {
                        streamChannelRooster(currentlyPlaying);
                    }
                    refreshNotificationUI();
                    break;
                case EVENT_SEEK:
                    break;
                case EVENT_STOP:
                    break;
                case EVENT_PICTURE:
                    break;
                case EVENT_TITLE:
                    break;
                case EVENT_DESTROY:
                    if(oneInstanceTaskFuture != null) oneInstanceTaskFuture.cancel(true);
                    mediaPlayer.release();
                    //Stop service
                    this.stopSelf();
                    if(onFlagExploreServiceEventListener != null)
                        onFlagExploreServiceEventListener.onEventDestroy();
                    break;
                default:
                    break;
            }
        }

        if (!mRunning) {
            mRunning = true;
            getChannelData();
        } else {
            if(onFlagExploreServiceEventListener != null)
                onFlagExploreServiceEventListener.updateUI(channelRoosters);
        }

        return START_STICKY;
    }

    private void refreshNotificationUI() {
        if(currentlyPlaying != null && currentlyPlaying.isPlaying()) {
            remoteViews.setInt(R.id.explore_notifcation_audio_listen, "setBackgroundResource", R.drawable.ic_pause_white_24dp);
        } else if(currentlyPlaying != null) {
            remoteViews.setInt(R.id.explore_notifcation_audio_listen, "setBackgroundResource", R.drawable.ic_play_arrow_white_24dp);
        }

        if(currentlyPlaying != null) {
            try {
                Picasso.with(AppContext)
                        .load(currentlyPlaying.getPhoto())
                        .resize(50, 50)
                        .centerCrop()
                        .into(remoteViews, R.id.explore_notification_profile_pic, NOTIFICATION_ID, notification);

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            remoteViews.setTextViewText(R.id.explore_notification_channel_title, currentlyPlaying.getName());
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    public void startExploreStatusBarPlayer() {
        createExploreStatusBarPlayer();
    }

    private void createExploreStatusBarPlayer() {

        Intent destroyIntent = new Intent(this, ExploreService.class);
        destroyIntent.putExtra(EXTRA_EXPLORE_NOTIFICATION_EVENT, EVENT_DESTROY);
        PendingIntent destroyPI = PendingIntent.getService(this, 0, destroyIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_ID_CHANNEL);
        notificationBuilder
                .setContentTitle("Rooster Explore")
                .setSmallIcon(R.drawable.logo)
                .setOngoing(true)
                .setAutoCancel(true)
                .setDeleteIntent(destroyPI).build();

        remoteViews = new RemoteViews(this.getPackageName(), R.layout.explore_service_notification);

        //Set the button listeners
        setListeners(remoteViews);
        notificationBuilder.setContent(remoteViews);

        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notification = notificationBuilder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void setListeners(RemoteViews remoteViews) {

        //Play/pause Listener
        Intent playPauseIntent = new Intent(this, ExploreService.class);
        playPauseIntent.putExtra(EXTRA_EXPLORE_NOTIFICATION_EVENT, EVENT_PLAYPAUSE);
        PendingIntent playPausePI = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.explore_notifcation_audio_listen, playPausePI);

        //Picture listener
        Intent pictureIntent = new Intent(this, ExploreService.class);
        pictureIntent.putExtra(EXTRA_EXPLORE_NOTIFICATION_EVENT, EVENT_PICTURE);
        PendingIntent picturePI = PendingIntent.getService(this, 2, pictureIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.explore_notification_profile_pic, picturePI);

        //Title listener
        Intent titleIntent = new Intent(this, ExploreService.class);
        titleIntent.putExtra(EXTRA_EXPLORE_NOTIFICATION_EVENT, EVENT_TITLE);
        PendingIntent titlePI = PendingIntent.getService(this, 3, titleIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.explore_notification_channel_title, titlePI);
    }

    public void refreshData() {
        mediaPlayer.release();
        startExploreStatusBarPlayer();
        getChannelData();
    }

    private void getChannelData() {
        mChannelsReference = FirebaseDatabase.getInstance().getReference()
                .child("channels");
        mChannelsReference.keepSynced(true);

        //Clear old data before syncing
        //Must be called outside thread, or use mThis context to access
        channelRoosterMap.clear();
        channelIterationMap.clear();

        ValueEventListener channelsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    final Channel channel = postSnapshot.getValue(Channel.class);
                    if (channel.isActive()) {
                        if(channel.isNew_alarms_start_at_first_iteration()) {
                            Integer iteration = jsonPersistence.getStoryIteration(channel.getUid());
                            if(iteration == null || iteration <= 0) iteration = 1;
                            channelIterationMap.put(channel, iteration);
                        } else {
                            Integer iteration = channel.getCurrent_rooster_cycle_iteration();
                            if(iteration == null || iteration <= 0) iteration = 1;
                            channelIterationMap.put(channel, iteration);
                        }
                    }
                }

                //Attach listeners all at once, so that last listener indicates data sync complete
                for (Map.Entry<Channel, Integer> entry:
                        channelIterationMap.entrySet()) {
                    getChannelRoosterData(entry.getKey(), entry.getValue());
                }

                mChannelRoostersReference = FirebaseDatabase.getInstance().getReference()
                        .child("channel_rooster_uploads");

                //https://stackoverflow.com/questions/34530566/find-out-if-child-event-listener-on-firebase-completely-load-all-data
                //Value events are always triggered last and are guaranteed to contain updates from any other events which occurred before that snapshot was taken.
                mChannelRoostersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(onFlagExploreServiceEventListener != null) {
                            onFlagExploreServiceEventListener.updateUI(channelRoosters);
                            onFlagExploreServiceEventListener.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toaster.makeToast(AppContext, "Failed to load channel.", Toast.LENGTH_SHORT).checkTastyToast();
            }
        };
        mChannelsReference.addListenerForSingleValueEvent(channelsListener);
    }

    private void getChannelRoosterData(final Channel channel, final Integer iteration) {
        final DatabaseReference channelRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                .child("channel_rooster_uploads").child(channel.getUid());
        //Ensure latest data is pulled
        channelRoosterUploadsReference.keepSynced(true);

        ValueEventListener channelRoosterUploadsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TreeMap<Integer,ChannelRooster> channelRoosterIterationMap = new TreeMap<>();
                //Check if node has children i.e. channelId content exists
                if(dataSnapshot.getChildrenCount() == 0) return;
                //Iterate over all content children
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    ChannelRooster channelRooster = postSnapshot.getValue(ChannelRooster.class);

                    //Set channelrooster display picture to banner image & description to channel description
                    //In future channelrooster specific details will be shown in discover page
                    channelRooster.setChannel_photo(channel.getPhoto());
                    channelRooster.setChannel_description(channel.getDescription());

                    //Only place non-current iterations into the map, to ensure we don't play current iteration in discover
                    if(channelRooster.isActive() && (channelRooster.getRooster_cycle_iteration() != iteration)) {
                        channelRoosterIterationMap.put(channelRooster.getRooster_cycle_iteration(), channelRooster);
                    }
                }
                if(!channelRoosterIterationMap.isEmpty() && iteration != null) {
                    findNextValidChannelRooster(channelRoosterIterationMap, channel, iteration);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        channelRoosterUploadsReference.addListenerForSingleValueEvent(channelRoosterUploadsListener);
    }

    private void findNextValidChannelRooster(final TreeMap<Integer,ChannelRooster> channelRoosterIterationMap, final Channel channel, final Integer iteration) {
        new Thread() {
            @Override
            public void run() {
                //Check head and tail of naturally sorted TreeMap for next valid channel content
                SortedMap<Integer,ChannelRooster> tailMap = channelRoosterIterationMap.tailMap(iteration);
                SortedMap<Integer,ChannelRooster> headMap = channelRoosterIterationMap.headMap(iteration);
                //        head               tail
                //  00000000000000[0]  x   00000000
                // We want to select [] where x is the current iteration, !NOT! included in the channelRoosterIterationMap - this ensures best content for discover
                // Or   x   0000000[0]  in the case of a story that is unstarted or a channel on first iteration
                if(!headMap.isEmpty()) {
                    //Retrieve channel audio
                    ChannelRooster channelRooster = channelRoosterIterationMap.get(headMap.lastKey());
                    channelRooster.setSelected(false);
                    //This method allows multiple objects per key
                    if(channelRoosterMap.containsKey(channel.getPriority())) {
                        channelRoosterMap.get(channel.getPriority()).add(channelRooster);
                    } else {
                        List<ChannelRooster> values = new ArrayList<>();
                        values.add(channelRooster);
                        channelRoosterMap.put(channel.getPriority(), values);
                    }
                } else if(!tailMap.isEmpty()) {
                    //Retrieve channel audio
                    ChannelRooster channelRooster = channelRoosterIterationMap.get(tailMap.lastKey());
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
                //For each channel rooster fetched, refresh the display list
                refreshTempChannelRoosters();
            }
        }.run();
    }

    private void refreshTempChannelRoosters() {
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
            }
        }.run();
    }

    public void streamChannelRooster(final ChannelRooster channelRooster) {
        currentlyPlaying = channelRooster;
        refreshNotificationUI();

        //The oneInstanceTask creates a new thread, only if one doesn't exist already
        ExecutorService executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
                new SynchronousQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());

        //Check for an active internet connection - displays toast if none
        if(InternetHelper.noInternetConnection(this)) {
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
                if(onFlagExploreServiceEventListener != null)
                    onFlagExploreServiceEventListener.updateSingleUIEntry(channelRoosters.indexOf(channelRooster), channelRooster);

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

                                if(onFlagExploreServiceEventListener != null)
                                    onFlagExploreServiceEventListener.clearMediaChecks();

                                channelRooster.setDownloading(false);
                                channelRooster.setPlaying(true);

                                refreshNotificationUI();

                                if(onFlagExploreServiceEventListener != null)
                                    onFlagExploreServiceEventListener.updateSingleUIEntry(channelRoosters.indexOf(channelRooster), channelRooster);

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
                                        if(onFlagExploreServiceEventListener != null) {
                                            onFlagExploreServiceEventListener.clearMediaChecks();
                                            onFlagExploreServiceEventListener.updateUI(channelRoosters);
                                        }

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
                                if(onFlagExploreServiceEventListener != null) {
                                    onFlagExploreServiceEventListener.clearMediaChecks();
                                    onFlagExploreServiceEventListener.updateUI(channelRoosters);
                                }

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
                        if(onFlagExploreServiceEventListener != null) {
                            onFlagExploreServiceEventListener.clearMediaChecks();
                            onFlagExploreServiceEventListener.updateUI(channelRoosters);
                        }
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
            if(onFlagExploreServiceEventListener != null)
                onFlagExploreServiceEventListener.clearMediaChecks();
            channelRooster.setPaused(true);
            if(onFlagExploreServiceEventListener != null)
                onFlagExploreServiceEventListener.updateUI(channelRoosters);
        } else if(channelRooster.isPaused() && mediaPlayer.getCurrentPosition() > 0) {
            if(onFlagExploreServiceEventListener != null)
                onFlagExploreServiceEventListener.clearMediaChecks();
            try {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
                mediaPlayer.start();
                channelRooster.setDownloading(false);
                channelRooster.setPlaying(true);
                if(onFlagExploreServiceEventListener != null)
                    onFlagExploreServiceEventListener.updateUI(channelRoosters);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                channelRooster.setDownloading(false);
                channelRooster.setPlaying(false);
                if(onFlagExploreServiceEventListener != null)
                    onFlagExploreServiceEventListener.updateUI(channelRoosters);
            }
        } else {
            try {
                mediaPlayer.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            //If a previous task exists, kill it
            if(oneInstanceTaskFuture != null) oneInstanceTaskFuture.cancel(true);
            if(onFlagExploreServiceEventListener != null) {
                onFlagExploreServiceEventListener.clearMediaChecks();
                onFlagExploreServiceEventListener.updateUI(channelRoosters);
            }
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
