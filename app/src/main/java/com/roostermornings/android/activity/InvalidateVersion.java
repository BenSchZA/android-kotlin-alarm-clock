/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.util.Constants;

import butterknife.BindView;
import butterknife.OnClick;

public class InvalidateVersion extends BaseActivity {

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @BindView(R.id.update_title)
    TextView updateTitle;
    @BindView(R.id.update_description)
    TextView updateDescription;
    @BindView(R.id.update_button)
    Button updateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_invalidate_version);

        if(getIntent().getExtras() != null) {
            updateTitle.setText(
                    getIntent().getStringExtra(Constants.FORCE_UPDATE_TITLE).isEmpty() ?
                            "Update Required":getIntent().getStringExtra(Constants.FORCE_UPDATE_TITLE)
            );
            updateDescription.setText(
                    getIntent().getStringExtra(Constants.FORCE_UPDATE_DESCRIPTION).isEmpty() ?
                            "You have an old version of Rooster, you need to update. But don't worry, the update brings new features!":getIntent().getStringExtra(Constants.FORCE_UPDATE_DESCRIPTION)
            );
        }
    }

    @OnClick(R.id.update_button)
    public void onUpdateButtonClick() {
        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        }
        catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}
