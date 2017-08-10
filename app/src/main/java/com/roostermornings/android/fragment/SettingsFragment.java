/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.roostermornings.android.BuildConfig;
import com.roostermornings.android.R;
import com.roostermornings.android.util.Constants;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.application_user_settings);

        // Display app version to user
        String versionName = BuildConfig.VERSION_NAME;
        findPreference(Constants.ABOUT_APP_VERSION).setSummary(versionName);
    }
}