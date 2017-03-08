package com.roostermornings.android.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.FriendsInviteListAdapter;
import com.roostermornings.android.domain.LocalContacts;
import com.roostermornings.android.domain.NodeUser;
import com.roostermornings.android.domain.NodeUsers;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.MyContactsController;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FriendsRequestFragment2.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FriendsRequestFragment2#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendsRequestFragment2 extends BaseFragment {

    protected static final String TAG = FriendsFragmentActivity.class.getSimpleName();

    private MyContactsController myContactsController;

    private BaseActivity baseActivity;

    ArrayList<NodeUser> mUsers = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;

    @BindView(R.id.friendsRequestListView)
    RecyclerView mRecyclerView;

    private OnFragmentInteractionListener mListener;

    public FriendsRequestFragment2() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment FriendsRequestFragment2.
     */
    public static FriendsRequestFragment2 newInstance(String param1, String param2) {
        FriendsRequestFragment2 fragment = new FriendsRequestFragment2();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize(R.layout.fragment_friends_fragment3);

        if (getArguments() != null) {
        }

        myContactsController = new MyContactsController(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_friends_fragment2, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    //NB: bind ButterKnife to view and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new FriendsInviteListAdapter(mUsers, getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        baseActivity = (BaseActivity) getActivity();
        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) executeNodeMyContactsTask();
        else  baseActivity.requestPermissionReadContacts();

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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

    //mListener.onFragmentInteraction(uri);

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onResume() {
        super.onResume();
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
        Call<NodeUsers> call = baseActivity.apiService().checkLocalContacts(new LocalContacts(myContactsController.processContacts(), idToken));

        call.enqueue(new Callback<NodeUsers>() {
            @Override
            public void onResponse(Response<NodeUsers> response,
                                   Retrofit retrofit) {

                int statusCode = response.code();
                NodeUsers apiResponse = response.body();

                if (statusCode == 200) {

                    mUsers = new ArrayList<>();
                    mUsers.addAll(apiResponse.users.get(0));
                    mAdapter = new FriendsInviteListAdapter(mUsers, getContext());

                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
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

