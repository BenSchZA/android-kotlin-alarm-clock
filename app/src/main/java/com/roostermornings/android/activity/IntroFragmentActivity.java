/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.fragment.FriendsInviteFragment3;
import com.roostermornings.android.fragment.FriendsMyFragment1;
import com.roostermornings.android.fragment.FriendsRequestFragment2;
import com.roostermornings.android.fragment.IIntroFragmentListener;
import com.roostermornings.android.fragment.IntroFragment1;
import com.roostermornings.android.fragment.IntroFragment2;
import com.roostermornings.android.fragment.IntroFragment3;
import com.roostermornings.android.util.DepthPageTransformer;
import com.roostermornings.android.util.MyContactsController;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class IntroFragmentActivity extends BaseActivity implements IIntroFragmentListener {

    /**
    * The pager widget, which handles animation and allows swiping horizontally to access previous
    * and next wizard steps.
    */
    private IntroFragmentActivity.SectionsPagerAdapter mSectionsPagerAdapter;
    @BindView(R.id.pager)
    ViewPager mPager;
    @BindView(R.id.tabDots)
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.fragmentactivity_intro);
        createViewPager(mPager);
        tabLayout.setupWithViewPager(mPager, true);
    }

    private void createViewPager(ViewPager mViewPager) {

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new IntroFragmentActivity.SectionsPagerAdapter(getSupportFragmentManager());

        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), IntroFragment1.class.getName()));
        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), IntroFragment2.class.getName()));
        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), IntroFragment3.class.getName()));

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    @Override
    public void onMobileNumberSet(String mobileNumber) {
        MyContactsController myContactsController = new MyContactsController(this);
        String NSNNumber = myContactsController.processContactCountry(mobileNumber);

        Intent intent = new Intent(IntroFragmentActivity.this, SignInActivity.class);
        intent.putExtra(getApplicationContext().getString(R.string.extras_mobile_number), NSNNumber);
        finish();
        startActivity(intent);
    }

    @Override
    public void onGetStartedClick() {
        mPager.setCurrentItem(2);
    }

//    /**
//     * The number of pages (wizard steps) to show in this demo.
//     */
//    private static final int NUM_PAGES = 3;
//
//
//
//    /**
//     * The pager adapter, which provides the pages to the view pager widget.
//     */
//    private PagerAdapter mPagerAdapter;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.fragmentactivity_intro);
//
//        // Instantiate a ViewPager and a PagerAdapter.
//        mPager = (ViewPager) findViewById(R.id.pager);
//        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
//        mPager.setAdapter(mPagerAdapter);
//        mPager.setPageTransformer(true, new DepthPageTransformer());
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (mPager.getCurrentItem() == 0) {
//            // If the user is currently looking at the first step, allow the system to handle the
//            // Back button. This calls finish() on this activity and pops the back stack.
//            super.onBackPressed();
//        } else {
//            // Otherwise, select the previous step.
//            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
//        }
//    }
//

//

//
//    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
//        public ScreenSlidePagerAdapter(FragmentManager fm) {
//            super(fm);
//        }
//
//        @Override
//        public Fragment getItem(int position) {
//
//            Fragment fragment = null;
//
//            //Log.d(TAG, String.valueOf(position));
//
//            switch (position) {
//                case 0:
//                    fragment = new IntroFragment1();
//                    break;
//                case 1:
//                    fragment = new IntroFragment2();
//                    break;
//                case 2:
//                    fragment = new IntroFragment3();
//                    break;
//            }
//
//            return fragment;
//
//        }
//
//        @Override
//        public int getCount() {
//            return NUM_PAGES;
//        }
//    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();

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

        public void addFrag(Fragment fragment) {
            mFragmentList.add(fragment);
        }
    }
}



