/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.domain.NodeUser;
import com.roostermornings.android.fragment.FriendsInviteFragment3;
import com.roostermornings.android.fragment.FriendsMyFragment1;
import com.roostermornings.android.fragment.FriendsRequestFragment2;
import com.roostermornings.android.util.FontsOverride;
import com.roostermornings.android.util.RoosterUtils;

import butterknife.BindView;
import butterknife.OnClick;

//Responsible for managing friends: 1) my friends, 2) addable friends, 3) friend invites
public class FriendsFragmentActivity extends BaseActivity implements
        FriendsMyFragment1.OnFragmentInteractionListener,
        FriendsRequestFragment2.OnFragmentInteractionListener,
        FriendsInviteFragment3.OnFragmentInteractionListener {

    public static final String TAG = FriendsFragmentActivity.class.getSimpleName();

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.tabs)
    TabLayout tabLayout;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    @BindView(R.id.container)
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_friends);

        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout.setupWithViewPager(mViewPager);

        FontsOverride.changeTabsFont(getApplicationContext(), tabLayout, "fonts/Nunito/Nunito-Bold.ttf");
    }

    @OnClick(R.id.home_record_audio)
    public void recordNewAudio() {
        startActivity(new Intent(FriendsFragmentActivity.this, NewAudioRecordActivity.class));
    }

    @OnClick(R.id.home_my_alarms)
    public void manageAlarms() {
        startHomeActivity();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment =null;
            switch (position) {
                case 0:
                    fragment = Fragment.instantiate(getApplicationContext(), FriendsMyFragment1.class.getName());
                    break;
                case 1:
                    fragment = Fragment.instantiate(getApplicationContext(), FriendsRequestFragment2.class.getName());
                    break;
                case 2:
                    fragment = Fragment.instantiate(getApplicationContext(), FriendsInviteFragment3.class.getName());
                    break;
                default:
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "FRIENDS";
                case 1:
                    return "REQUESTS";
                case 2:
                    return "INVITE";
            }
            return null;
        }
    }

    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
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

                } else {
                    //TODO: add button to ask for permission again, display blank fragment with explainer
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //Send invite to Rooster user from contact list
    public void inviteUser(NodeUser nodeUser) {

        //TODO: consolidate nodeUser and friend class
        if (nodeUser.getSelected()) {

            String inviteUrl = String.format("friend_requests_received/%s/%s", nodeUser.getId(), mCurrentUser.getUid());
            String currentUserUrl = String.format("friend_requests_sent/%s/%s", mCurrentUser.getUid(), nodeUser.getId());

            //Create friend object from current signed in user
            Friend currentUserFriend = new Friend(mCurrentUser.getUid(), mCurrentUser.getUser_name(), mCurrentUser.getProfile_pic(), mCurrentUser.getCell_number());
            //Create friend object from Rooster user in contact list
            Friend inviteFriend = new Friend(nodeUser.getId(), nodeUser.getUser_name(), nodeUser.getProfile_pic(), nodeUser.getCell_number());

            //Append to received and sent request list
            mDatabase.getDatabase().getReference(inviteUrl).setValue(currentUserFriend);
            mDatabase.getDatabase().getReference(currentUserUrl).setValue(inviteFriend);

            Toast.makeText(this, nodeUser.getUser_name() + " invited!", Toast.LENGTH_LONG).show();
        }
    }

    //Accept friend request and update Firebase DB
    public void acceptFriendRequest(Friend friend) {

        if (friend.getSelected()) {

            String currentUserUrl = String.format("users/%s/friends/%s", mCurrentUser.getUid(), friend.getUid());
            String friendUserUrl = String.format("users/%s/friends/%s", friend.getUid(), mCurrentUser.getUid());

            //Create friend object from current signed in user
            Friend currentUserFriend = new Friend(mCurrentUser.getUid(), mCurrentUser.getUser_name(), mCurrentUser.getProfile_pic(), mCurrentUser.getCell_number());

            mDatabase.getDatabase().getReference(currentUserUrl).setValue(friend);
            mDatabase.getDatabase().getReference(friendUserUrl).setValue(currentUserFriend);

            String receivedUrl = String.format("friend_requests_received/%s/%s", friend.getUid(), getFirebaseUser().getUid());
            String sentUrl = String.format("friend_requests_sent/%s/%s", mCurrentUser.getUid(), friend.getUid());

            //Clear received and sent request list
            mDatabase.getDatabase().getReference(receivedUrl).setValue(null);
            mDatabase.getDatabase().getReference(sentUrl).setValue(null);

            //Notify user that friend request accepted
            Toast.makeText(this, friend.getUser_name() + "'s friend request accepted!", Toast.LENGTH_LONG).show();
        }
    }
}
