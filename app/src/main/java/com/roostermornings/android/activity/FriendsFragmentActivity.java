/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.fragment.FriendsInviteFragment3;
import com.roostermornings.android.fragment.FriendsMyFragment1;
import com.roostermornings.android.fragment.FriendsRequestFragment2;
import com.roostermornings.android.util.FontsOverride;

import java.util.ArrayList;
import java.util.List;

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

    public FriendsFragmentActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_friends);

        setSupportActionBar(toolbar);

        //Create a viewpager with fragments controlled by SectionsPagerAdapter
        createViewPager(mViewPager);
        tabLayout.setupWithViewPager(mViewPager);
        //Generate custom tab for tab layout
        createTabIcons();

        //Listen for change to mViewPager page display - used for toggling notifications
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if(position == 1 && getTabNotification(position) == View.VISIBLE) setTabNotification(position, false);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

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

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private void createViewPager(ViewPager mViewPager) {

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), FriendsMyFragment1.class.getName()), "FRIENDS");
        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), FriendsRequestFragment2.class.getName()), "REQUESTS");
        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), FriendsInviteFragment3.class.getName()), "INVITE");

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    private void createTabIcons() {

        setTabLayout(0, "FRIENDS", false);
        setTabLayout(1, "REQUESTS", false);
        setTabLayout(2, "INVITE", false);
    }

    //Create custom tab layout
    public void setTabLayout(int position, String title, boolean notification) {
        RelativeLayout relativeLayout = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.custom_friends_tab, null);
        TextView tabText = (TextView) relativeLayout.getChildAt(0);
        tabText.setText(title);
        tabLayout.getTabAt(position).setCustomView(relativeLayout);
        setTabNotification(position, false);
    }

    //Set current tab notification
    public void setTabNotification(int position, boolean notification) {
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        RelativeLayout relativeLayout = (RelativeLayout) tab.getCustomView();
        ImageView imageNotification = (ImageView) tab.getCustomView().findViewById(R.id.tab_notification);
        if(notification) imageNotification.setVisibility(View.VISIBLE);
        else imageNotification.setVisibility(View.GONE);
        tab.setCustomView(relativeLayout);
    }

    public int getTabNotification(int position) {
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        RelativeLayout relativeLayout = (RelativeLayout) tab.getCustomView();
        ImageView imageNotification = (ImageView) tab.getCustomView().findViewById(R.id.tab_notification);
        return imageNotification.getVisibility();
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
    public void inviteUser(Friend inviteFriend) {

        if (!checkInternetConnection()) return;

        String inviteUrl = String.format("friend_requests_received/%s/%s", inviteFriend.getUid(), mCurrentUser.getUid());
        String currentUserUrl = String.format("friend_requests_sent/%s/%s", mCurrentUser.getUid(), inviteFriend.getUid());

        //Create friend object from current signed in user
        Friend currentUserFriend = new Friend(mCurrentUser.getUid(), mCurrentUser.getUser_name(), mCurrentUser.getProfile_pic(), mCurrentUser.getCell_number());

        //Append to received and sent request list
        mDatabase.getDatabase().getReference(inviteUrl).setValue(currentUserFriend);
        mDatabase.getDatabase().getReference(currentUserUrl).setValue(inviteFriend);

        Toast.makeText(this, inviteFriend.getUser_name() + " invited!", Toast.LENGTH_LONG).show();
    }

    //Delete friend from Firebase user friend list
    public void deleteFriend(Friend deleteFriend) {

        if (!checkInternetConnection()) return;

        String currentUserUrl = String.format("users/%s/friends/%s", mCurrentUser.getUid(), deleteFriend.getUid());
        String friendUserUrl = String.format("users/%s/friends/%s", deleteFriend.getUid(), mCurrentUser.getUid());

        //Clear current user's and friend's friend list
        mDatabase.getDatabase().getReference(currentUserUrl).setValue(null);
        mDatabase.getDatabase().getReference(friendUserUrl).setValue(null);
    }

    //Accept friend request and update Firebase DB
    public void acceptFriendRequest(Friend acceptFriend) {

        if (!checkInternetConnection()) return;

        String currentUserUrl = String.format("users/%s/friends/%s", mCurrentUser.getUid(), acceptFriend.getUid());
        String friendUserUrl = String.format("users/%s/friends/%s", acceptFriend.getUid(), mCurrentUser.getUid());

        //Create friend object from current signed in user
        Friend currentUserFriend = new Friend(mCurrentUser.getUid(), mCurrentUser.getUser_name(), mCurrentUser.getProfile_pic(), mCurrentUser.getCell_number());

        mDatabase.getDatabase().getReference(currentUserUrl).setValue(acceptFriend);
        mDatabase.getDatabase().getReference(friendUserUrl).setValue(currentUserFriend);

        String receivedUrl = String.format("friend_requests_received/%s/%s", mCurrentUser.getUid(), acceptFriend.getUid());
        String sentUrl = String.format("friend_requests_sent/%s/%s", acceptFriend.getUid(), mCurrentUser.getUid());

        //Clear received and sent request list
        mDatabase.getDatabase().getReference(receivedUrl).setValue(null);
        mDatabase.getDatabase().getReference(sentUrl).setValue(null);

        //Notify user that friend request accepted
        Toast.makeText(this, acceptFriend.getUser_name() + "'s friend request accepted!", Toast.LENGTH_LONG).show();
    }

    public void rejectFriendRequest(Friend rejectFriend) {

        if (!checkInternetConnection()) return;

        String receivedUrl = String.format("friend_requests_received/%s/%s", mCurrentUser.getUid(), rejectFriend.getUid());
        String sentUrl = String.format("friend_requests_sent/%s/%s", rejectFriend.getUid(), mCurrentUser.getUid());

        //Clear received and sent request list
        mDatabase.getDatabase().getReference(receivedUrl).setValue(null);
        mDatabase.getDatabase().getReference(sentUrl).setValue(null);

        //Notify user that friend request accepted
        Toast.makeText(this, rejectFriend.getUser_name() + "'s friend request rejected!", Toast.LENGTH_LONG).show();
    }
}
