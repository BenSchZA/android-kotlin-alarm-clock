/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.roostermornings.android.background.AudioService;
import com.roostermornings.android.background.UploadService;
import com.roostermornings.android.domain.DeviceAudioQueueItem;
import com.roostermornings.android.domain.FCMPayloadSocialRooster;
import com.roostermornings.android.domain.NodeAPIResult;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.domain.Users;
import com.roostermornings.android.sqlutil.DeviceAlarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;

import java.sql.Timestamp;
import java.util.ArrayList;

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

        mAdapter = new NewAudioFriendsListAdapter(mFriends, NewAudioFriendsActivity.this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(NewAudioFriendsActivity.this));
        mRecyclerView.setAdapter(mAdapter);

        Bundle extras = getIntent().getExtras();
        localFileString = extras.getString("localFileString");

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


        retrieveMyFriends();

        //Bind to upload service to allow asynchronous management of Rooster upload
        Intent intent = new Intent(this, UploadService.class);
        startService(intent);
        //0 indicates that service should not be restarted
        bindService(intent, mUploadServiceConnection, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mUploadServiceConnection);
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
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    private void retrieveMyFriends() {

        if (!checkInternetConnection()) return;

        FirebaseUser firebaseUser = getFirebaseUser();

        if (firebaseUser == null) {
            Log.d(TAG, "User not authenticated on FB!");
            return;
        }

        Call<Users> call = mActivity.apiService().listUserFriendList(firebaseUser.getUid());

        call.enqueue(new Callback<Users>() {
            @Override
            public void onResponse(Response<Users> response,
                                   Retrofit retrofit) {

                int statusCode = response.code();
                Users apiResponse = response.body();

                if (statusCode == 200) {

                    mFriends = new ArrayList<>();
                    mFriends.addAll(apiResponse.users);
                    mAdapter = new NewAudioFriendsListAdapter(mFriends, NewAudioFriendsActivity.this);

                    mRecyclerView.setLayoutManager(new LinearLayoutManager(NewAudioFriendsActivity.this));
                    mRecyclerView.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                    btnNewAudioSave.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.i(TAG, t.getLocalizedMessage());
            }
        });
    }

    @OnClick(R.id.new_audio_upload_button)
    protected void onSaveButtonClick() {

        if (!checkInternetConnection()) return;

        if(mFriends.isEmpty()) {
            Toast.makeText(NewAudioFriendsActivity.this, R.string.new_audio_at_least_one_friend, Toast.LENGTH_LONG);
            return;
        }
        mUploadService.processAudioFile(firebaseIdToken, localFileString, mFriends);
        startHomeActivity();
    }
}
