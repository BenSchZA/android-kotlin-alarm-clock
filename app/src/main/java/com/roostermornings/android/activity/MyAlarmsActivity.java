package com.roostermornings.android.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.roostermornings.android.fragment.MyAlarmsFragment;

import java.util.ArrayList;

import butterknife.BindView;

public class MyAlarmsActivity extends BaseActivity {

    public static final String TAG = MyAlarmsActivity.class.getSimpleName();
    private MyAlarmsFragment.OnMyAlarmsFragmentInteractionListener mListener;
    private DatabaseReference mMyAlarmsReference;
    private ArrayList<Alarm> alarms = new ArrayList<Alarm>();

    @BindView(R.id.alarmsListView)
    RecyclerView mRecyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_my_alarms);

        mMyAlarmsReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(getFirebaseUser().getUid());

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new MyAlarmsListAdapter(alarms);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        ValueEventListener alarmsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Object test = postSnapshot.getValue();
                    Alarm alarm = postSnapshot.getValue(Alarm.class);
                    alarms.add(alarm);
                    mAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(MyAlarmsActivity.this, "Failed to load alarms.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        mMyAlarmsReference.addValueEventListener(alarmsListener);
    }
}
