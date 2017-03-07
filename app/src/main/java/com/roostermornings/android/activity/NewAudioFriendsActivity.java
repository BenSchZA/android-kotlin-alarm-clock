package com.roostermornings.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.NewAudioFriendsListAdapter;
import com.roostermornings.android.domain.SocialRooster;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.domain.Users;

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
    private String mAudioFileUrl = "";
    private User mCurrentUser;

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
        mAudioFileUrl = extras.getString("downloadUrl");

        retrieveMyUserDetails();

    }

    private void retrieveMyUserDetails() {

        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                mCurrentUser = user;
                retrieveMyFriends();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(NewAudioFriendsActivity.this, "Failed to load user.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        DatabaseReference thisUserReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(getFirebaseUser().getUid());

        thisUserReference.addListenerForSingleValueEvent(userListener);


    }

    private void retrieveMyFriends() {

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

        for (User friend : mFriends) {
            if (friend.getSelected()) {
                inviteUsers();
                return;
            }
        }
        Toast.makeText(NewAudioFriendsActivity.this, "Please select at least 1 friend", Toast.LENGTH_LONG);

    }

    private void inviteUsers() {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        FirebaseUser currentUser = getFirebaseUser();

        for (User friend : mFriends) {
            if (friend.getSelected()) {

                String uploadUrl = String.format("social_rooster_uploads/%s", mAuth.getCurrentUser().getUid());
                String queueUrl = String.format("social_rooster_queue/%s", friend.getUid());

                String uploadKey = mDatabase.child(uploadUrl).push().getKey();
                String queueKey = mDatabase.child(String.format("social_rooster_queue/%s", friend.getUid())).push().getKey();

                SocialRooster socialRoosterUploaded = new SocialRooster(mAudioFileUrl,
                        friend.getCell_number(),
                        friend.getUser_name(),
                        false,
                        friend.getProfile_pic(),
                        timestamp.getTime(),
                        friend.getUid(), uploadKey, currentUser.getUid());

                SocialRooster socialRoosterQueue = new SocialRooster(mAudioFileUrl,
                        mCurrentUser.getCell_number(),
                        mCurrentUser.getUser_name(),
                        false,
                        mCurrentUser.getProfile_pic(),
                        timestamp.getTime(),
                        friend.getUid(), queueKey, mCurrentUser.getUid());


                mDatabase.getDatabase().getReference(uploadUrl + "/" + uploadKey).setValue(socialRoosterUploaded);
                mDatabase.getDatabase().getReference(queueUrl + "/" + queueKey).setValue(socialRoosterQueue);
                Toast.makeText(NewAudioFriendsActivity.this, friend.getUser_name() + " invited!", Toast.LENGTH_LONG).show();

            }
        }

        Intent intent = new Intent(NewAudioFriendsActivity.this, MyAlarmsFragmentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


}
