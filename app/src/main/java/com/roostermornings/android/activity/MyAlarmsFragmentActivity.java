package com.roostermornings.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.sqlutil.DeviceAlarmController;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

public class MyAlarmsFragmentActivity extends BaseActivity {

    public static final String TAG = MyAlarmsFragmentActivity.class.getSimpleName();
    private DatabaseReference mMyAlarmsReference;
    private ArrayList<Alarm> mAlarms = new ArrayList<>();

    private DeviceAlarmController deviceAlarmController;

    @BindView(R.id.home_alarmsListView)
    RecyclerView mRecyclerView;

    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_my_alarms);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbarTitle.setText(getString(R.string.my_alarms));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MyAlarmsFragmentActivity.this, NewAlarmFragmentActivity.class));
            }
        });

        mMyAlarmsReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(getFirebaseUser().getUid());

        mAdapter = new MyAlarmsListAdapter(mAlarms, MyAlarmsFragmentActivity.this);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        ValueEventListener alarmsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Alarm alarm = postSnapshot.getValue(Alarm.class);
                    mAlarms.add(alarm);
                    mAdapter.notifyItemInserted(mAlarms.size() - 1);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(MyAlarmsFragmentActivity.this, "Failed to load mAlarms.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        mMyAlarmsReference.addValueEventListener(alarmsListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_signout) {
            signOut();
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.home_record_audio)
    public void recordNewAudio() {
        startActivity(new Intent(MyAlarmsFragmentActivity.this, NewAudioRecordActivity.class));
    }

    @OnClick(R.id.home_friends)
    public void manageFriends() {
        startActivity(new Intent(MyAlarmsFragmentActivity.this, MyFriendsFragmentActivity.class));
    }

    public void deleteAlarm(String setId, String alarmId) {
        DeviceAlarmController deviceAlarmController = new DeviceAlarmController(this);

        //Remove alarm from firebase
        DatabaseReference alarmReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(getFirebaseUser().getUid()).child(alarmId);
        alarmReference.removeValue();

        //Remove alarm *set* from local SQL database using retreived setId from firebase
        deviceAlarmController.deleteAlarmSet(Long.valueOf(setId));

        //TODO: find out why alarm content being duplicated in recycler view on delete - do not refresh the activity like this
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
}
