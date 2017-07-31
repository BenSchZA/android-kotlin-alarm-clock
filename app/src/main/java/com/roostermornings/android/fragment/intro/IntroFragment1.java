/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.intro;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.fragment.base.BaseFragment;

import butterknife.OnClick;

public class IntroFragment1 extends BaseFragment {

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return initiate(inflater, R.layout.fragment_intro_fragment1, container, false);
    }

    @Override
    public void onAttach(Context context) {
        inject(((BaseApplication)getActivity().getApplication()).getRoosterApplicationComponent());

        super.onAttach(context);
    }
}
