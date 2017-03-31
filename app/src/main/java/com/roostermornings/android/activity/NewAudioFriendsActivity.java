/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.NewAudioFriendsListAdapter;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.service.UploadService;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.domain.Users;
import com.roostermornings.android.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class NewAudioFriendsActivity extends BaseActivity {

    protected static final String TAG = NewAudioFriendsActivity.class.getSimpleName();
    private NewAudioFriendsActivity mActivity = this;
    ArrayList<User> mFriends = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;
    private String localFileString = "";
    private String firebaseIdToken = "";

    UploadService mUploadService;
    private boolean mBound;

    @BindView(R.id.new_audio_upload_button)
    Button btnNewAudioSave;

    @BindView(R.id.new_audio_friendsListView)
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_new_audio_friends);

        getFirebaseUser().getToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            firebaseIdToken = task.getResult().getToken();
                        } else {
                            // Handle error -> task.getException();
                        }
                    }
                });

        Bundle extras = getIntent().getExtras();
        localFileString = extras.getString(Constants.EXTRA_LOCAL_FILE_STRING);

        if(extras.containsKey(Constants.EXTRA_FRIENDS_LIST) && (ArrayList<User>)extras.getSerializable(Constants.EXTRA_FRIENDS_LIST) != null) {
            mFriends.addAll((ArrayList<User>) extras.getSerializable(Constants.EXTRA_FRIENDS_LIST));
            if(mFriends.size() == 1 && mFriends.get(0).getSelected()) {
                onSaveButtonClick();
            } else if(!mFriends.isEmpty()) {
                mAdapter = new NewAudioFriendsListAdapter(mFriends, NewAudioFriendsActivity.this);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(NewAudioFriendsActivity.this));
                mRecyclerView.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
                btnNewAudioSave.setVisibility(View.VISIBLE);
            } else if (checkInternetConnection()) {
                Toast.makeText(this, "Do you have any Rooster friends?", Toast.LENGTH_LONG).show();
                startHomeActivity();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mBound) unbindService(mUploadServiceConnection);
    }

    private ServiceConnection mUploadServiceConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Because we have bound to an explicit
            // service that is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            UploadService.LocalBinder binder = (UploadService.LocalBinder) service;
            mUploadService = binder.getService();
            mBound = true;

            //Start upload service thread task
            mUploadService.processAudioFile(firebaseIdToken, localFileString, mFriends);
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    @OnClick(R.id.new_audio_upload_button)
    protected void onSaveButtonClick() {

        if (!checkInternetConnection()) return;

        if(mFriends.isEmpty()) {
            Toast.makeText(NewAudioFriendsActivity.this, R.string.new_audio_at_least_one_friend, Toast.LENGTH_LONG);
            return;
        }

        //Bind to upload service to allow asynchronous management of Rooster upload
        Intent intent = new Intent(this, UploadService.class);
        startService(intent);
        //0 indicates that service should not be restarted
        bindService(intent, mUploadServiceConnection, 0);

        //Switch to home activity - alarms
        startHomeActivity();
    }
}
