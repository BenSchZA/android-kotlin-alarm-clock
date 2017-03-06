package com.roostermornings.android.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MyFriendsListAdapter;
import com.roostermornings.android.domain.LocalContacts;
import com.roostermornings.android.domain.NodeUser;
import com.roostermornings.android.domain.NodeUsers;
import com.roostermornings.android.util.MyContactsController;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

//Responsible for managing friends: 1) my friends, 2) addable friends, 3) friend invites
public class MyFriendsFragmentActivity extends BaseActivity {

    protected static final String TAG = MyFriendsFragmentActivity.class.getSimpleName();
    private MyContactsController myContactsController;

    ArrayList<NodeUser> mFriends = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;

    @BindView(R.id.home_myFriendsListView)
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_my_friends);

        mAdapter = new MyFriendsListAdapter(mFriends, MyFriendsFragmentActivity.this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(MyFriendsFragmentActivity.this));
        mRecyclerView.setAdapter(mAdapter);

        myContactsController = new MyContactsController(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) executeNodeMyContactsTask();
        else requestPermissionReadContacts();
    }

    @OnClick(R.id.home_record_audio)
    public void recordNewAudio() {
        startActivity(new Intent(MyFriendsFragmentActivity.this, NewAudioRecordActivity.class));
    }

    @OnClick(R.id.home_my_alarms)
    public void manageAlarms() {
        startActivity(new Intent(MyFriendsFragmentActivity.this, MyAlarmsFragmentActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    executeNodeMyContactsTask();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void executeNodeMyContactsTask() {
        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUser.getToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            String idToken = task.getResult().getToken();
                            checkLocalContactsNode(idToken);
                        } else {
                            // Handle error -> task.getException();
                        }
                    }
                });
    }

    private void checkLocalContactsNode(String idToken) {
        Call<NodeUsers> call = this.apiService().checkLocalContacts(new LocalContacts(myContactsController.processContacts(), idToken));

        call.enqueue(new Callback<NodeUsers>() {
            @Override
            public void onResponse(Response<NodeUsers> response,
                                   Retrofit retrofit) {

                int statusCode = response.code();
                NodeUsers apiResponse = response.body();

                if (statusCode == 200) {

                    mFriends = new ArrayList<>();
                    mFriends.addAll(apiResponse.users.get(0));
                    mAdapter = new MyFriendsListAdapter(mFriends, MyFriendsFragmentActivity.this);

                    mRecyclerView.setLayoutManager(new LinearLayoutManager(MyFriendsFragmentActivity.this));
                    mRecyclerView.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();

                    Log.d("apiResponse", apiResponse.toString());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.i(TAG, t.getLocalizedMessage());
            }
        });
    }
}
