/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.EventLog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.domain.Contact;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.firebase.FirebaseNetwork;
import com.roostermornings.android.fragment.NumberEntryDialogFragment;
import com.roostermornings.android.fragment.NumberEntryListener;
import com.roostermornings.android.fragment.base.BaseFragment;
import com.roostermornings.android.fragment.friends.FriendsInviteFragment3;
import com.roostermornings.android.fragment.friends.FriendsMyFragment1;
import com.roostermornings.android.fragment.friends.FriendsRequestFragment2;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.FontsOverride;
import com.roostermornings.android.util.StrUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

//Responsible for managing friends: 1) my friends, 2) addable friends, 3) friend invites
public class FriendsFragmentActivity extends BaseActivity implements
        FriendsMyFragment1.OnFragmentInteractionListener,
        FriendsRequestFragment2.OnFragmentInteractionListener,
        NumberEntryListener {

    public static final String TAG = FriendsFragmentActivity.class.getSimpleName();
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.home_friends)
    ImageButton buttonMyFriends;
    @BindView(R.id.button_bar)
    LinearLayout buttonBarLayout;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    @BindView(R.id.friendsViewPager)
    ViewPager mViewPager;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private DatabaseReference mFriendRequestsReceivedReference;
    private DatabaseReference mFriendRequestsSentReference;
    private DatabaseReference mCurrentUserReference;
    private BroadcastReceiver receiver;

    protected FriendsFragmentActivity mThis = this;

    FriendsMyFragment1 friendsFragment1;
    FriendsRequestFragment2 friendsFragment2;
    FriendsInviteFragment3 friendsFragment3;

    @Inject BaseApplication baseApplication;
    @Inject @Nullable FirebaseUser mCurrentUser;
    @Inject SharedPreferences sharedPreferences;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    public FriendsFragmentActivity() {

    }

    public interface FriendsInviteListAdapterInterface {
        //Send invite to Rooster user from contact list
        void addUser(Friend inviteFriend);
    }

    public interface FriendsRequestListAdapterInterface {
        //Accept friend request and update Firebase DB
        void acceptFriendRequest(Friend acceptFriend);
        void rejectFriendRequest(Friend rejectFriend);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialize(R.layout.activity_friends);
        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

        //If number hasn't been provided
        showNumberEntryDialog();

        setDayNightTheme();
        setButtonBarSelection();

        //Set toolbar title
        setupToolbar(toolbarTitle, getString(R.string.friends));

        //Keep local and Firebase alarm dbs synced, and enable offline persistence
        mFriendRequestsReceivedReference = FirebaseDatabase.getInstance().getReference()
                .child("friend_requests_received").child(mCurrentUser.getUid());

        mFriendRequestsSentReference = FirebaseDatabase.getInstance().getReference()
                .child("friend_requests_sent").child(mCurrentUser.getUid());

        mCurrentUserReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        mFriendRequestsReceivedReference.keepSynced(true);
        mFriendRequestsSentReference.keepSynced(true);
        mCurrentUserReference.keepSynced(true);

        //Create a viewpager with fragments controlled by SectionsPagerAdapter
        createViewPager(mViewPager);
        //This makes sure view is not recreated when scrolling, as we have 3 fragment pages
        mViewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(mViewPager);
        //Generate custom tab for tab layout
        createTabIcons();

        //Check for new Firebase datachange notifications and register broadcast receiver
        updateNotifications();

        //Listen for change to mViewPager page display - used for toggling notifications
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    //Clear request notification badge
                    setTabNotification(position, false);
                    setButtonBarNotification(false);
                    BaseApplication.setNotificationFlag(0, Constants.FLAG_FRIENDREQUESTS);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        FontsOverride.changeTabsFont(getApplicationContext(), tabLayout, Constants.APP_FONT);

        //Handle search intent
        handleIntent(getIntent());
    }

    private void showNumberEntryDialog() {
        if(!sharedPreferences.getBoolean(Constants.MOBILE_NUMBER_ENTRY_DISMISSED, false) && !sharedPreferences.getBoolean(Constants.MOBILE_NUMBER_VALIDATED, false)) {
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment newFragment = NumberEntryDialogFragment.newInstance(true);
            newFragment.show(fm, "new frag");
        }

        FirebaseNetwork.setOnFlagValidMobileNumberCompleteListener(new FirebaseNetwork.OnFlagValidMobileNumberCompleteListener() {
            @Override
            public void onEvent(boolean valid) {
                if(!valid) {
                    //Refresh UI fragment to show number entry dialog
                    if(mSectionsPagerAdapter != null) mSectionsPagerAdapter.notifyDataSetChanged();
                }
            }
        });
        FirebaseNetwork.flagValidMobileNumber(this, true);
    }



    @Override
    public void onStart() {
        super.onStart();
        //Display notifications
        updateRoosterNotification();
        //updateRequestNotification();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        //mViewPager.setCurrentItem(sharedPreferences.getInt(Constants.FRIENDS_ACTIVITY_CURRENT_FRAGMENT, 0));
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            handleSearch(query);
        }
    }

    private void handleSearch(String query) {
        if (mViewPager.getCurrentItem() == 0) {
            friendsFragment1.searchRecyclerViewAdapter(query);
        } else if (mViewPager.getCurrentItem() == 1) {
            if(friendsFragment2.isAdded()) friendsFragment2.searchRecyclerViewAdapter(query);
        } else if (mViewPager.getCurrentItem() == 2) {
            friendsFragment3.searchRecyclerViewAdapter(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_friends, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        //When searchView is closed, refresh data
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View arg0) {
                // search was detached/closed
                if(mViewPager.getCurrentItem() == 0) {
                    friendsFragment1.notifyAdapter();
                } else if(mViewPager.getCurrentItem() == 1) {
                    if(friendsFragment2.isAdded()) friendsFragment2.notifyAdapter();
                } else if(mViewPager.getCurrentItem() == 2) {
                    friendsFragment3.notifyAdapter();
                }
            }

            @Override
            public void onViewAttachedToWindow(View arg0) {
                // search was opened
            }
        });

        return true;
    }

    private void createViewPager(ViewPager mViewPager) {

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        friendsFragment1 = (FriendsMyFragment1) Fragment.instantiate(getApplicationContext(), FriendsMyFragment1.class.getName());
        friendsFragment2 = (FriendsRequestFragment2) Fragment.instantiate(getApplicationContext(), FriendsRequestFragment2.class.getName());
        friendsFragment3 = (FriendsInviteFragment3) Fragment.instantiate(getApplicationContext(), FriendsInviteFragment3.class.getName());

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return friendsFragment1;
                case 1:
                    if(sharedPreferences.getBoolean(Constants.MOBILE_NUMBER_VALIDATED, false)) {
                        return friendsFragment2;
                    } else {
                        return new NumberEntryDialogFragment();
                    }
                case 2:
                    return friendsFragment3;
                default:
                    return null;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
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
                default:
                    return null;
            }
        }
    }

    @Override
    public void onMobileNumberValidated(String mobileNumber) {
        mSectionsPagerAdapter.notifyDataSetChanged();
    }

    private void createTabIcons() {

        setTabLayout(0, "FRIENDS");
        setTabLayout(1, "REQUESTS");
        setTabLayout(2, "INVITE");
    }

    //Create custom tab layout
    public void setTabLayout(int position, String title) {
        FrameLayout frameLayout = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.custom_friends_tab, null);
        TextView tabText = (TextView) frameLayout.getChildAt(0);
        tabText.setText(title);
        //Disable clipping to ensure notification is shown properly
        ViewGroup tabs = ((ViewGroup)tabLayout.getChildAt(0));
        tabs.setClipToPadding(false);
        tabs.setClipChildren(false);
        if(tabs.getChildAt(position) instanceof ViewGroup) {
            ((ViewGroup) tabs.getChildAt(position)).setClipToPadding(false);
            ((ViewGroup) tabs.getChildAt(position)).setClipChildren(false);
        }

        tabLayout.getTabAt(position).setCustomView(frameLayout);
    }

    //Set current tab notification
    public void setTabNotification(int position, boolean notification) {
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        if(tab != null) {
            FrameLayout frameLayout = (FrameLayout) tab.getCustomView();
            if(frameLayout != null) {
                ImageView tabNotification = (ImageView) frameLayout.findViewById(R.id.notification_friends);
                if (notification) tabNotification.setVisibility(View.VISIBLE);
                else tabNotification.setVisibility(View.GONE);
                tab.setCustomView(frameLayout);
            }
        }
    }

    public void setButtonBarNotification(boolean notification) {
        ImageView buttonBarNotification = (ImageView) buttonBarLayout.findViewById(R.id.notification_friends);
        if (notification) buttonBarNotification.setVisibility(View.VISIBLE);
        else buttonBarNotification.setVisibility(View.GONE);
    }

    private void updateNotifications() {
        //Flag check for UI changes on load, broadcastreceiver for changes while activity running
        //If notifications waiting, display new friend request notification
        if (BaseApplication.getNotificationFlag(Constants.FLAG_FRIENDREQUESTS) > 0) {
            setButtonBarNotification(true);
            setTabNotification(1, true);
        }

        //Broadcast receiver filter to receive UI updates
        IntentFilter firebaseListenerServiceFilter = new IntentFilter();
        firebaseListenerServiceFilter.addAction(Constants.ACTION_REQUESTNOTIFICATION);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                if (BaseApplication.getNotificationFlag(Constants.FLAG_FRIENDREQUESTS) > 0) {
                    setButtonBarNotification(true);
                    setTabNotification(1, true);
                    manualSwipeRefreshRequests();
                }
            }
        };
        registerReceiver(receiver, firebaseListenerServiceFilter);
    }

    public void manualSwipeRefreshFriends() {
        friendsFragment1.manualSwipeRefresh();
    }

    public void manualSwipeRefreshRequests() {
        if(friendsFragment2.isAdded()) friendsFragment2.manualSwipeRefresh();
    }

    @Override
    protected void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    public int getTabNotification(int position) {
        TabLayout.Tab tab = tabLayout.getTabAt(position);
        ImageView imageNotification = (ImageView) tab.getCustomView().findViewById(R.id.notification_friends);
        return imageNotification.getVisibility();
    }

    public void onFragmentInteraction(Uri uri) {
        //you can leave it empty
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    friendsFragment3.requestPermissionReadContacts();
                } else {
                    friendsFragment3.displayRequestPermissionExplainer(true);
                }
                break;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //Delete friend from Firebase user friend list
    public void deleteFriend(User deleteFriend) {

        String currentUserUrl = String.format("users/%s/friends/%s", mCurrentUser.getUid(), deleteFriend.getUid());
        String friendUserUrl = String.format("users/%s/friends/%s", deleteFriend.getUid(), mCurrentUser.getUid());

        //Clear current user's and friend's friend list
        mDatabase.getDatabase().getReference(currentUserUrl).setValue(null);
        mDatabase.getDatabase().getReference(friendUserUrl).setValue(null);
    }

    //Invite contact via Whatsapp or fallback to SMS
    public void inviteContact(Contact contact) {
//        Uri uri = Uri.parse("smsto:" + contact.getPrimaryNumber());
//        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
//        intent.putExtra("sms_body", getResources().getString(R.string.invite_to_rooster_message));
//        intent.setType("text/plain");
//        intent.setPackage("com.whatsapp");
//        startActivity(Intent.createChooser(intent, ""));
//        Intent intent = new Intent(Intent.ACTION_SENDTO);
//        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, contact.getPrimaryNumber());
//        intent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.invite_to_rooster_message));
//        intent.setType("text/plain");
//        intent.setPackage("com.whatsapp");
//        startActivity(intent);
//        Intent sendIntent = new Intent("android.intent.action.MAIN");
//        //sendIntent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Conversation"));
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.setType("text/plain");
//        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");
//        sendIntent.putExtra("jid", contact.getPrimaryNumber() + "@s.whatsapp.net");
//        sendIntent.setPackage("com.whatsapp");
//        startActivity(sendIntent);
//        Intent sendIntent = new Intent("android.intent.action.SEND");
//        sendIntent.setComponent(new ComponentName("com.whatsapp","com.whatsapp.ContactPicker"));
//        sendIntent.putExtra("jid", contact.getPrimaryNumber() + "@s.whatsapp.net");
//        sendIntent.putExtra(Intent.EXTRA_TEXT,"sample text you want to send along with the image");
//        startActivity(sendIntent);

        Uri uri = Uri.parse("smsto:" + contact.getPrimaryNumber());
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra("sms_body", getResources().getString(R.string.invite_to_rooster_message));
        startActivity(intent);
    }
}
