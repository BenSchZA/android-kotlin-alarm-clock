package com.roostermornings.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.roostermornings.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class IntroFragment3 extends BaseFragment {

    @BindView(R.id.intro_mobile_number_edit_text)
    @NotEmpty
    EditText mobileNumber;

    @BindView(R.id.intro_post_registration_next_button)
    Button nextButton;

    Validator validator;

    OnMobileNumberSetListener mListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_intro_fragment3, container, false);
        ButterKnife.bind(this, view);

        //instantiate saripaar validator to validate fields with NotEmpty annotations
        validator = new Validator(this);
        validator.setValidationListener(this);

        return view;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        if (context instanceof OnMobileNumberSetListener) {
            mListener = (OnMobileNumberSetListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }


    @OnClick(R.id.intro_post_registration_next_button)
    public void onNextButtonClicked() {

        validator.validate();

    }

    @Override
    public void onValidationSucceeded() {

        mListener.onMobileNumberSet(mobileNumber.getText().toString().trim());

    }
}
