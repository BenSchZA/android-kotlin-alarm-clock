/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.friends;

import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.adapter.FriendsInviteListAdapter;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.domain.LocalContacts;
import com.roostermornings.android.domain.NodeUsers;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.MyContactsController;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FriendsInviteFragment3.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FriendsInviteFragment3#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendsInviteFragment3 extends BaseFragment {

    protected static final String TAG = FriendsFragmentActivity.class.getSimpleName();

    private MyContactsController myContactsController;

    private BaseActivity baseActivity;

    private static int statusCode = -1;

    ArrayList<Friend> mUsers = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;

    @BindView(R.id.friendsInviteListView)
    RecyclerView mRecyclerView;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.share_button)
    Button shareButton;

    private OnFragmentInteractionListener mListener;

    @Inject Context AppContext;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    public FriendsInviteFragment3() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment FriendsInviteFragment3.
     */
    public static FriendsInviteFragment3 newInstance(String param1, String param2) {
        FriendsInviteFragment3 fragment = new FriendsInviteFragment3();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inject(((BaseApplication)getActivity().getApplication()).getRoosterApplicationComponent());

        //Ensure check for Node complete reset
        statusCode = -1;

        if (getArguments() != null) {
        }
        myContactsController = new MyContactsController(AppContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return initiate(inflater, R.layout.fragment_friends_fragment3, container, false);
    }

    //NB: bind ButterKnife to view and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new FriendsInviteListAdapter(mUsers);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(AppContext));
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

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

    public void searchRecyclerViewAdapter(String query) {
        //Filter contacts by CharSequence
        //Get reference to list adapter to access getFilter method
        ((FriendsInviteListAdapter)mAdapter).refreshAll(mUsers);
        ((FriendsInviteListAdapter)mAdapter).getFilter().filter(query);
    }

    public void notifyAdapter() {
        ((FriendsInviteListAdapter)mAdapter).refreshAll(mUsers);
        mAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.share_button)
    public void onShareButtonClicked() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.invite_to_rooster_message));
        sendIntent.setType("text/plain");

        sendIntent = Intent.createChooser(sendIntent, "Share Rooster");
        startActivity(sendIntent);
    }

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

    public void requestGetContacts() {
        baseActivity = (BaseActivity) getActivity();
        if (ContextCompat.checkSelfPermission(AppContext,
                android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) executeNodeMyContactsTask();
        else  baseActivity.requestPermissionReadContacts();
    }

    public void executeNodeMyContactsTask() {
        //Check if node response already exists
        if(statusCode == 200) {
            progressBar.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            if (!checkInternetConnection()) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
                getFirebaseUser().getToken(true)
                        .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                            public void onComplete(@NonNull Task<GetTokenResult> task) {
                                if (task.isSuccessful()) {
                                    String idToken = task.getResult().getToken();
                                    checkLocalContactsNode(idToken);
                                } else {
                                    // Handle error -> task.getException();
                                    progressBar.setVisibility(View.GONE);
                                }
                            }
                        });
            }
        }
    }

    private void checkLocalContactsNode(String idToken) {
        Call<NodeUsers> call = apiService().checkLocalContacts(new LocalContacts(myContactsController.processContacts(), idToken));

        call.enqueue(new Callback<NodeUsers>() {
            @Override
            public void onResponse(Response<NodeUsers> response,
                                   Retrofit retrofit) {

                statusCode = response.code();
                NodeUsers apiResponse = response.body();

                if (statusCode == 200) {

                    mUsers = new ArrayList<>();
                    for (Friend user:
                         apiResponse.users.get(0)) {
                        if(user != null && !user.getUser_name().isEmpty()) mUsers.add(user);
                    }
                    //Sort names alphabetically before notifying adapter
                    sortNamesFriends(mUsers);

                    notifyAdapter();

                    //Make load spinner GONE and recyclerview VISIBLE
                    progressBar.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);

                    Log.d("apiResponse", apiResponse.toString());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.i(TAG, t.getLocalizedMessage()==null?"":t.getLocalizedMessage());
                Toast.makeText(getApplicationContext(), "Loading contacts failed, please try again.", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
