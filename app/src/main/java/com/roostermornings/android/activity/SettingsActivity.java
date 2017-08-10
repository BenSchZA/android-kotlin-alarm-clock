/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.fragment.SettingsFragment;

import butterknife.BindView;

public class SettingsActivity extends BaseActivity {

    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.custom_preference_screen_layout);
        inject(BaseApplication.getRoosterApplicationComponent());

        //setDayNightTheme();

        //Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .add(R.id.main_content, new SettingsFragment())
                .commit();

        //Set toolbar title
        setupToolbar(toolbarTitle, getString(R.string.settings_activity_title));
    }
}
