package com.roostermornings.android.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.R;
import com.roostermornings.android.adapter.MyAlarmsListAdapter;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.fragment.base.BaseFragment;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;


public class MyAlarmsFragment extends BaseFragment {

    private static final String ARG_USER_UID_PARAM = "user_uid_param";
    public static final String TAG = MyAlarmsFragment.class.getSimpleName();
    private String mUserUidParam;
    private OnMyAlarmsFragmentInteractionListener mListener;
    private DatabaseReference mMyAlarmsReference;
    private ArrayList<Alarm> alarms = new ArrayList<Alarm>();

    @BindView(R.id.alarmsListView)
    RecyclerView mRecyclerView;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    public MyAlarmsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment MyAlarmsFragment.
     */
    public static MyAlarmsFragment newInstance(String param1) {
        MyAlarmsFragment fragment = new MyAlarmsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_UID_PARAM, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUserUidParam = getArguments().getString(ARG_USER_UID_PARAM);
        }
        mMyAlarmsReference = FirebaseDatabase.getInstance().getReference()
                .child("alarms").child(mUserUidParam);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = initiate(inflater, R.layout.fragment_my_alarms, container, false);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new MyAlarmsListAdapter(alarms);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        ValueEventListener alarmsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Alarm alarm = dataSnapshot.getValue(Alarm.class);
                    alarms.add(alarm);
                    mAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(getContext(), "Failed to load alarms.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        mMyAlarmsReference.addValueEventListener(alarmsListener);

        return view;


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMyAlarmsFragmentInteractionListener) {
            mListener = (OnMyAlarmsFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnMyAlarmsFragmentInteractionListener {
        //void onFragmentInteraction(Uri uri);
    }
}
