/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.roostermornings.android.fragment.SettingsFragment;

public class SettingsActivity extends PreferenceActivity {

//    @BindView(R.id.toolbar_title)
//    TextView toolbarTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
