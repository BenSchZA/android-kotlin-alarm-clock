/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.friends;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.FriendsFragmentActivity;
import com.roostermornings.android.adapter.FriendsInviteListAdapter;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Contact;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.domain.LocalContacts;
import com.roostermornings.android.domain.NodeUsers;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.JSONPersistence;
import com.roostermornings.android.util.MyContactsController;
import com.roostermornings.android.util.Toaster;

import java.util.ArrayList;
import java.util.HashMap;

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

    ArrayList<Object> mRecyclerViewElements = new ArrayList<>();
    ArrayList<Friend> mAddableContacts = new ArrayList<>();
    ArrayList<Contact> mInvitableContacts = new ArrayList<>();

    private RecyclerView.Adapter mAddContactsAdapter;

    String addableHeader = "";
    String invitableHeader = "";

    @BindView(R.id.friendsAddListView)
    RecyclerView mAddContactsRecyclerView;

    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.share_button)
    Button shareButton;

    @BindView(R.id.retrieve_contacts_permission_text)
    TextView retrieveContactsPermissionText;

    private OnFragmentInteractionListener mListener;

    ProcessAddableBackground processAddableBackground = new ProcessAddableBackground();

    @Inject Context AppContext;
    @Inject @Nullable FirebaseUser firebaseUser;
    @Inject JSONPersistence jsonPersistence;
    @Inject MyContactsController myContactsController;

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
        inject(BaseApplication.getRoosterApplicationComponent());

        addableHeader = getResources().getString(R.string.add_contacts);
        invitableHeader = getResources().getString(R.string.invite_contacts);

        myContactsController = new MyContactsController(AppContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = initiate(inflater, R.layout.fragment_friends_3, container, false);

        return view;
    }

    //NB: bind ButterKnife to view and then initialise UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAddContactsAdapter = new FriendsInviteListAdapter(mRecyclerViewElements);
        mAddContactsRecyclerView.setLayoutManager(new LinearLayoutManager(AppContext));
        mAddContactsRecyclerView.setAdapter(mAddContactsAdapter);

        mAddContactsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                notifyAdapter();
            }
        });


        swipeRefreshLayout.setRefreshing(true);
        /*
        * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user
        * performs a swipe-to-refresh gesture.
        */
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.

                        //If pull to refresh and permission has been denied, retry, else request permission as per normal
                        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                                android.Manifest.permission.READ_CONTACTS)) {
                            retrieveContactsPermissionRetry();
                        } else {
                            requestPermissionReadContacts();
                        }
                    }
                }
        );

        requestPermissionReadContacts();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void searchRecyclerViewAdapter(String query) {
        //Filter contacts by CharSequence
        //Get reference to list adapter to access getFilter method
        ((FriendsInviteListAdapter) mAddContactsAdapter).refreshAll(mRecyclerViewElements);
        ((FriendsInviteListAdapter) mAddContactsAdapter).getFilter().filter(query);
    }

    public void notifyAdapter() {
        setHeaderVisibility();

        ((FriendsInviteListAdapter) mAddContactsAdapter).refreshAll(mRecyclerViewElements);
        mAddContactsAdapter.notifyDataSetChanged();
    }

    private void setHeaderVisibility() {
        //If list is empty, don't show header
        try {
            if (mRecyclerViewElements.contains(addableHeader)
                    && !(mRecyclerViewElements.get(
                    mRecyclerViewElements.indexOf(addableHeader) + 1) instanceof Friend)) {
                mRecyclerViewElements.remove(addableHeader);
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        try {
            if (mRecyclerViewElements.contains(invitableHeader)
                    && !(mRecyclerViewElements.get(
                    mRecyclerViewElements.indexOf(invitableHeader) + 1) instanceof Contact)) {
                mRecyclerViewElements.remove(invitableHeader);
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.share_button)
    public void onShareButtonClicked() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.share_rooster_message));
        sendIntent.setType("text/plain");
        sendIntent = Intent.createChooser(sendIntent, "Share Rooster");
        startActivity(sendIntent);
        FA.Log(FA.Event.invitation_to_join_rooster_sent.class, null, null);
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

    public void requestPermissionReadContacts() {
        //Clear explainer on entry, show if necessary i.e. permission previously denied
        displayRequestPermissionExplainer(false);

        if (ContextCompat.checkSelfPermission(AppContext,
                android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    android.Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                displayRequestPermissionExplainer(true);
            } else {

                // No explanation needed, we can request the permission.

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.READ_CONTACTS},
                        Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else if(ContextCompat.checkSelfPermission(AppContext,
                android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {

            //If there is no internet connection, attempt to retrieve invitable contacts from persistence
            if(!jsonPersistence.getInvitableContacts().isEmpty() && !checkInternetConnection()) {
                mInvitableContacts = jsonPersistence.getInvitableContacts();
                //Populate recycler view elements
                mRecyclerViewElements.add(invitableHeader);
                mRecyclerViewElements.addAll(mInvitableContacts);
                //Display results
                notifyAdapter();
            }

            executeNodeMyContactsTask();
        }
    }

    private void executeNodeMyContactsTask() {
        if (!checkInternetConnection()) {
            swipeRefreshLayout.setRefreshing(false);
        } else {
            if(!swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(true);
            firebaseUser.getIdToken(true)
                    .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                        public void onComplete(@NonNull Task<GetTokenResult> task) {
                            if (task.isSuccessful()) {
                                String idToken = task.getResult().getToken();

                                if(processAddableBackground.getStatus().equals(AsyncTask.Status.FINISHED)) {
                                    processAddableBackground = null;
                                    processAddableBackground = new ProcessAddableBackground();
                                    processAddableBackground.execute(idToken);
                                } else if(processAddableBackground.getStatus().equals(AsyncTask.Status.PENDING)) {
                                    processAddableBackground.execute(idToken);
                                } else if(!processAddableBackground.getStatus().equals(AsyncTask.Status.RUNNING)) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            } else {
                                // Handle error -> task.getException();
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    });
        }
    }

    private class ProcessAddableBackground extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            mRecyclerViewElements.clear();
        }

        @Override
        protected String doInBackground(String... params) {
            String idToken = params[0];

            Call<NodeUsers> call = apiService().checkLocalContacts(new LocalContacts(myContactsController.getNodeNumberList(), idToken));

            call.enqueue(new Callback<NodeUsers>() {
                @Override
                public void onResponse(Response<NodeUsers> response,
                                       Retrofit retrofit) {

                    Integer statusCode = response.code();
                    NodeUsers apiResponse = response.body();

                    if (statusCode == 200) {

                        if(apiResponse.users != null) {
                            mAddableContacts = new ArrayList<>();

                            //Get a map of number name pairs from my contacts
                            HashMap<String, String> numberNamePairs = myContactsController.getNumberNamePairs();
                            for (Friend user :
                                    apiResponse.users.get(0)) {
                                if (user != null && !user.getUser_name().isEmpty()) {
                                    //If user in my contacts, use that as user name
                                    if(numberNamePairs.containsKey(user.getCell_number())) {
                                        user.setUser_name(numberNamePairs.get(user.getCell_number()));
                                    }
                                    mAddableContacts.add(user);
                                }
                            }

                            //Sort names alphabetically before notifying adapter
                            sortNamesFriends(mAddableContacts);
                        }

                        Log.d("apiResponse", apiResponse.toString());
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.i(TAG, t.getLocalizedMessage()==null?"":t.getLocalizedMessage());
                    Toaster.makeToast(getApplicationContext(), "Loading contacts failed, please try again.", Toast.LENGTH_LONG).checkTastyToast();
                    new ProcessInvitableBackground().execute("");
                }
            });

            return null;
        }

        protected void onPostExecute(String result) {
            new ProcessInvitableBackground().execute("");
        }
    }

    private class ProcessInvitableBackground extends AsyncTask<String, Void, String> {

        private Boolean exit = false;

        @Override
        protected void onPreExecute() {
            //If list is already populated, return
            if(!mInvitableContacts.isEmpty()) exit = true;
        }

        @Override
        protected String doInBackground(String... params) {
            if(exit) return "";

            //Get the list of all local contacts, C
            ArrayList<Contact> contacts = myContactsController.getContacts();
            //Temporarily set the invitable contacts, I, to all local contacts
            mInvitableContacts.clear();
            mInvitableContacts.addAll(contacts);
            ArrayList<String> contactNumbersToRemove = new ArrayList<>();

            //Check for current friends, F
            ArrayList<User> currentFriends = new ArrayList<>();
            if(!jsonPersistence.getFriends().isEmpty()) {
                currentFriends = jsonPersistence.getFriends();
            }

            //Make a list of all contact numbers from mAddableContacts and currentFriends, A + F
            for (Friend addableFriend:
                    mAddableContacts) {
                contactNumbersToRemove.add(addableFriend.cell_number);
            }
            for (User currentFriend:
                    currentFriends) {
                contactNumbersToRemove.add(currentFriend.cell_number);
            }

            //Perform the operation I = C - (A + F)
            for (Contact contact:
                    contacts) {
                for (String number:
                        contact.getNumbers().keySet()) {
                    if(contactNumbersToRemove.contains(number)) {
                        mInvitableContacts.remove(contact);
                        break;
                    }
                }
            }

            //Sort names alphabetically before notifying adapter
            sortNamesContacts(mInvitableContacts);

            return "";
        }

        protected void onPostExecute(String result) {
            //Persist invitable contacts for those times where user has no internet connection
            jsonPersistence.setInvitableContacts(mInvitableContacts);

            //Load new content (including section headers) into adapter
            mRecyclerViewElements.add(addableHeader);
            mRecyclerViewElements.addAll(mAddableContacts);
            mRecyclerViewElements.add(invitableHeader);
            mRecyclerViewElements.addAll(mInvitableContacts);
            //Display new content
            notifyAdapter();
            //Make load spinner GONE and recyclerview VISIBLE
            swipeRefreshLayout.setRefreshing(false);
        }
    }


    @OnClick(R.id.retrieve_contacts_permission_text)
    public void retrieveContactsPermissionRetry() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.READ_CONTACTS},
                Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    public void displayRequestPermissionExplainer(Boolean display) {
        if(display) {
            retrieveContactsPermissionText.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
        } else {
            retrieveContactsPermissionText.setVisibility(View.GONE);
        }
    }
}
