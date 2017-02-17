package com.roostermornings.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Alarm;
import com.roostermornings.android.fragment.NewAlarmFragment1;
import com.roostermornings.android.fragment.IAlarmSetListener;
import com.roostermornings.android.fragment.NewAlarmFragment2;

import java.util.Calendar;

public class NewAlarmFragmentActivity extends BaseActivity implements IAlarmSetListener {

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    Alarm mAlarm = new Alarm();
    Calendar mCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragmentactivity_new_alarm);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        int mHour = mCalendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = mCalendar.get(Calendar.MINUTE);

        mAlarm.setMinute(mMinute);
        mAlarm.setHour(mHour);
        mAlarm.setRecurring(false);
        mAlarm.setAllow_friend_audio_files(true);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

    }


    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_alarm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_next) {
            if (mViewPager.getCurrentItem() == 0) mViewPager.setCurrentItem(1);
            else {
                //save alarm!
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                String key = mDatabase.child("alarms").push().getKey();
                database.getReference(String.format("alarms/%s/%s", mAuth.getCurrentUser().getUid(), key)).setValue(mAlarm);
                Toast.makeText(getBaseContext(), "Alarm created!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getBaseContext(), MyAlarmsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();

            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setAlarmDetails(Alarm alarm) {
        mAlarm = alarm;
    }

    @Override
    public Alarm getAlarmDetails() {
        return mAlarm;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment fragment = null;

            switch (position) {
                case 0:
                    return NewAlarmFragment1.newInstance(getFirebaseUser().getUid());
                case 1:
                    return NewAlarmFragment2.newInstance(getFirebaseUser().getUid());
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Create an alarm";
                case 1:
                    return "Create an alarm";
            }
            return null;
        }
    }
}