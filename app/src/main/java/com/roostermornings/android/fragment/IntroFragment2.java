/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.roostermornings.android.R;
import com.roostermornings.android.fragment.base.BaseFragment;

import butterknife.OnClick;

public class IntroFragment2 extends BaseFragment {

    IIntroFragmentListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return initiate(inflater, R.layout.fragment_intro_fragment2, container, false);
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        if (context instanceof IIntroFragmentListener) {
            mListener = (IIntroFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @OnClick(R.id.intro_2_get_started_button)
    public void onGetStartedClick() {
        mListener.onGetStartedClick();
    }
}
