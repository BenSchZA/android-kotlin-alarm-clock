/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.fragment.message_status.MessageStatusReceivedFragment1;
import com.roostermornings.android.fragment.message_status.MessageStatusSentFragment2;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.FontsOverride;

import javax.inject.Inject;

import butterknife.BindView;

public class MessageStatusFragmentActivity extends BaseActivity implements
        MessageStatusReceivedFragment1.OnFragmentInteractionListener,
        MessageStatusSentFragment2.OnFragmentInteractionListener {

    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;
    @BindView(R.id.tabs)
    TabLayout tabLayout;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    @BindView(R.id.messageViewPager)
    ViewPager mViewPager;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private MessageStatusFragmentActivity.SectionsPagerAdapter mSectionsPagerAdapter;

    MessageStatusReceivedFragment1 messageStatusFragment1;
    MessageStatusReceivedFragment1 messageStatusFragment2;
    MessageStatusSentFragment2 messageStatusFragment3;

    @Inject BaseApplication baseApplication;
    @Inject @Nullable FirebaseUser firebaseUser;
    @Inject
    AudioTableManager audioTableManager;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    public void onFragmentInteraction(Uri uri) {
        //you can leave it empty
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_message_status);
        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

        setDayNightTheme();
        setButtonBarSelection();

        //Set toolbar title
        setupToolbar(toolbarTitle, getString(R.string.message_status_activity_title));

        //Create a viewpager with fragments controlled by SectionsPagerAdapter
        createViewPager(mViewPager);
        //This makes sure view is not recreated when scrolling, as we have 3 fragment pages
        mViewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(mViewPager);
        //Generate custom tab for tab layout
        createTabIcons();

        //Set volume rocker to alarm stream
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //Listen for change to mViewPager page display - used for toggling notifications
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
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

    @Override
    public void onStart() {
        super.onStart();
        //Display notifications
        updateRoosterNotification();
        updateRequestNotification();
    }

    @Override
    public void onPause() {
        super.onPause();

        if(messageStatusFragment1 != null && messageStatusFragment2 != null) {
            messageStatusFragment1.mAdapterClass.resetMediaPlayer();
            messageStatusFragment2.mAdapterClass.resetMediaPlayer();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            handleSearch(query);
        }
    }

    private void handleSearch(String query) {
        if (mViewPager.getCurrentItem() == 0) {
            messageStatusFragment1.searchRecyclerViewAdapter(query);
        } else if (mViewPager.getCurrentItem() == 1) {
            messageStatusFragment2.searchRecyclerViewAdapter(query);
        } else if (mViewPager.getCurrentItem() == 2) {
            messageStatusFragment3.searchRecyclerViewAdapter(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_roosters, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search_roosters).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        //When searchView is closed, refresh data
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

            @Override
            public void onViewDetachedFromWindow(View arg0) {
                // search was detached/closed
                if(mViewPager.getCurrentItem() == 0) {
                    messageStatusFragment1.notifyAdapter();
                } else if(mViewPager.getCurrentItem() == 1) {
                    messageStatusFragment2.notifyAdapter();
                } else if(mViewPager.getCurrentItem() == 2) {
                    messageStatusFragment3.notifyAdapter();
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
        mSectionsPagerAdapter = new MessageStatusFragmentActivity.SectionsPagerAdapter(getSupportFragmentManager());

        Bundle bundleFragmentToday = new Bundle();
        bundleFragmentToday.putString(Constants.MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE, Constants.MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_TODAY);
        messageStatusFragment1 = (MessageStatusReceivedFragment1) Fragment.instantiate(getApplicationContext(), MessageStatusReceivedFragment1.class.getName(), bundleFragmentToday);

        Bundle bundleFragmentFavourite = new Bundle();
        bundleFragmentFavourite.putString(Constants.MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE, Constants.MESSAGE_STATUS_RECEIVED_FRAGMENT_TYPE_FAVOURITE);
        messageStatusFragment2 = (MessageStatusReceivedFragment1) Fragment.instantiate(getApplicationContext(), MessageStatusReceivedFragment1.class.getName(), bundleFragmentFavourite);

        messageStatusFragment3 = (MessageStatusSentFragment2) Fragment.instantiate(getApplicationContext(), MessageStatusSentFragment2.class.getName());

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return messageStatusFragment1;
                case 1:
                    return messageStatusFragment2;
                case 2:
                    return messageStatusFragment3;
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
                    return "TODAY";
                case 1:
                    return "FAVOURITE";
                case 2:
                    return "SENT";
                default:
                    return null;
            }
        }
    }

    private void createTabIcons() {
        setTabLayout(0, "TODAY");
        setTabLayout(1, "FAVOURITE");
        setTabLayout(2, "SENT");
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

    public void favouriteSocialRooster(int ID, boolean favourite) {
        audioTableManager.setFavourite(ID, favourite);

        if(mViewPager.getCurrentItem() == 0) {
            messageStatusFragment2.manualSwipeRefresh();
        } else if(mViewPager.getCurrentItem() == 1) {
            messageStatusFragment1.manualSwipeRefresh();
        }
    }
}
