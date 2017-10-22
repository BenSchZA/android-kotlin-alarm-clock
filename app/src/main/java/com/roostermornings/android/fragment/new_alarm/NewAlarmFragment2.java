/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.new_alarm;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.adapter.ChannelsListAdapter;
import com.roostermornings.android.adapter_data.ChannelManager;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.AlarmChannel;
import com.roostermornings.android.domain.ChannelRooster;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.FileUtils;
import com.roostermornings.android.util.FirstMileManager;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.RoosterUtils;
import com.roostermornings.android.util.Toaster;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;

import butterknife.BindView;

import static android.app.Activity.RESULT_OK;

public class NewAlarmFragment2 extends BaseFragment {

    private static final int AUDIO_GET_REQUEST = 10110;

    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = NewAlarmFragment2.class.getSimpleName();
    private String mUserUidParam;
    private IAlarmSetListener mListener;

    private NewAlarmFragment2 mThis = this;

    private ArrayList<ChannelRooster> channelRoosters = new ArrayList<>();

    @BindView(R.id.main_content)
    RecyclerView mRecyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Inject JSONPersistence jsonPersistence;
    @Inject Context AppContext;
    @Inject ChannelManager channelManager;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    public NewAlarmFragment2() {
        // Required empty public constructor
    }

    public interface ChannelInterface {
        void setSelectedChannel(ChannelRooster channelRooster);
        void clearSelectedChannel();
    }

    public static NewAlarmFragment2 newInstance(String param1) {
        NewAlarmFragment2 fragment = new NewAlarmFragment2();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID_PARAM, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject(BaseApplication.getRoosterApplicationComponent());

        if (getArguments() != null) {
            mUserUidParam = getArguments().getString(ARG_USER_UID_PARAM);
        }

//        Intent musicPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
//        musicPickerIntent.setType("audio/*");
//        startActivityForResult(musicPickerIntent, AUDIO_GET_REQUEST);
    }

    @Override
    public void onPause() {
        super.onPause();

        //Persist channels for seamless loading
        jsonPersistence.setNewAlarmChannelRoosters(channelRoosters);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = initiate(inflater, R.layout.fragment_new_alarm_step2, container, false);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new ChannelsListAdapter(channelRoosters, getActivity());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(AppContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        if(!jsonPersistence.getNewAlarmChannelRoosters().isEmpty()) {
            channelRoosters.addAll(jsonPersistence.getNewAlarmChannelRoosters());
            mAdapter.notifyDataSetChanged();
        }

        ChannelManager.Companion.setOnFlagChannelManagerDataListener(new ChannelManager.Companion.OnFlagChannelManagerDataListener() {
            @Override
            public void onChannelRoosterDataChanged(@NotNull ArrayList<ChannelRooster> freshChannelRoosters) {
                channelRoosters.clear();
                channelRoosters.addAll(freshChannelRoosters);
            }

            @Override
            public void onSyncFinished() {
                if (mListener != null)
                    mListener.retrieveAlarmDetailsFromSQL(); //this is only relevant for alarms being edited
                if(mAdapter != null) mAdapter.notifyDataSetChanged();
            }
        });
        channelManager.refreshChannelData(channelRoosters);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        MediaPlayer mediaPlayer = new MediaPlayer();
        if(resultCode == RESULT_OK){
            switch (requestCode) {
                case AUDIO_GET_REQUEST:
                    if(data != null) {
                        Uri audioUri = data.getData();
                        if(audioUri != null) {

                            File customFilePath = new File(getContext().getFilesDir(), "Media");
                            String timeStamp = DateFormat.getDateTimeInstance().format(new Date());
                            String customFileName = "Rooster_Custom_Audio_" + RoosterUtils.createRandomUID(5) + "_" + timeStamp + ".3gp";
                            File customFile = new File(customFilePath, customFileName);

                            if(customFile.getParentFile().exists() || customFile.getParentFile().mkdirs()) {

                                try {
                                    //Copy file contents to new public file
                                    FileUtils.copyFromStream(getContext().getContentResolver().openInputStream(audioUri), customFile);

                                    //If file is valid, open a share dialog
//                                  && shareFile.length() < Constants.MAX_ROOSTER_FILE_SIZE
                                    if (customFile.isFile() && validMimeType(customFile)) {
                                        //Uri shareFileUri = FileProvider.getUriForFile(this, "com.roostermornings.android.fileprovider", shareFile);
                                        //Send audio file to friends selection activity
//                                        Intent intent = new Intent(SplashActivity.this, NewAudioFriendsActivity.class);
//                                        Bundle bun = new Bundle();
//                                        bun.putString(Constants.EXTRA_LOCAL_FILE_STRING, shareFile.getPath());
//                                        intent.putExtras(bun);
//                                        startActivity(intent);

                                        try {
                                            mediaPlayer.setDataSource(customFile.getAbsolutePath());
                                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                @Override
                                                public void onPrepared(MediaPlayer mediaPlayer) {
                                                    mediaPlayer.start();
                                                }
                                            });
                                            mediaPlayer.prepareAsync();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        return;
                                    } else {
                                        Toaster.makeToast(getContext(), "File is greater than 8 MB, or is corrupt.", Toast.LENGTH_LONG);
                                        return;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toaster.makeToast(getContext(), "Error loading file.", Toast.LENGTH_LONG);
                                    return;
                                }
                            }
                        }
                    }
                default:
                    break;
            }
        }
    }

    private boolean validMimeType(File audioFile) {
        if(audioFile != null) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(audioFile.getPath());
            String mimeTypeStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            if(mimeTypeStr != null &&
                    (mimeTypeStr.contains("audio/mpeg")
                            || mimeTypeStr.contains("audio/mp4")
                            || mimeTypeStr.contains("audio/3gpp"))) {
                return true;
            } else if(mimeTypeStr != null) {
                Toaster.makeToast(getContext(), "Invalid audio mime-type " + mimeTypeStr, Toast.LENGTH_SHORT);
            }
        }
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        inject(BaseApplication.getRoosterApplicationComponent());

        if (context instanceof IAlarmSetListener) {
            mListener = (IAlarmSetListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement IAlarmSetListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (visible) {
            if (mListener != null) mListener.setNextButtonCaption(getString(R.string.save));
        }
    }

    public void selectEditedAlarmChannel() {

        final Alarm alarm = (mListener == null) ? new Alarm() : mListener.getAlarmDetails();
        AlarmChannel alarmChannel = alarm.getChannel();
        if(alarmChannel == null) return;

        for (ChannelRooster channelRooster : channelRoosters) {
            if (alarmChannel.getId().equals(channelRooster.getChannel_uid())) {
                channelRooster.setSelected(true);
                alarm.getChannel().setName(channelRooster.getName());
                return;
            }
        }
    }
}
