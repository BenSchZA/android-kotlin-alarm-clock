/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.fragment.intro.IntroFragment1;
import com.roostermornings.android.fragment.intro.IntroFragment2;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class IntroFragmentActivity extends BaseActivity  {

    /**
    * The pager widget, which handles animation and allows swiping horizontally to access previous
    * and next wizard steps.
    */
    private IntroFragmentActivity.SectionsPagerAdapter mSectionsPagerAdapter;
    @BindView(R.id.pager)
    ViewPager mPager;
    @BindView(R.id.tabDots)
    TabLayout tabLayout;
    @BindView(R.id.intro_get_started_button)
    Button getStartedButton;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.fragmentactivity_intro);
        inject(BaseApplication.getRoosterApplicationComponent());

        createViewPager(mPager);
        tabLayout.setupWithViewPager(mPager, true);
    }

    private void createViewPager(ViewPager mViewPager) {

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new IntroFragmentActivity.SectionsPagerAdapter(getSupportFragmentManager());

        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), IntroFragment1.class.getName()));
        mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), IntroFragment2.class.getName()));
        //mSectionsPagerAdapter.addFrag(Fragment.instantiate(getApplicationContext(), IntroFragment3.class.getName()));

        // Set up the ViewPager with the sections adapter.
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        getStartedButton.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        getStartedButton.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
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

    @OnClick(R.id.intro_get_started_button)
    public void onGetStartedClick() {
        //mPager.setCurrentItem(2);
        Intent intent = new Intent(IntroFragmentActivity.this, SignInActivity.class);
        startActivity(intent);
    }

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



