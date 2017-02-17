package com.roostermornings.android.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.roostermornings.android.R;
import com.roostermornings.android.fragment.base.BaseFragment;

import butterknife.BindView;
import butterknife.OnClick;


public class IntroFragment3 extends BaseFragment {

    @BindView(R.id.intro_mobile_number_edit_text)
    @NotEmpty
    EditText mobileNumber;

    @BindView(R.id.intro_post_registration_next_button)
    Button nextButton;

    Validator validator;

    IMobileNumberSetListener mListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = initiate(inflater, R.layout.fragment_intro_fragment3, container, false);

        //instantiate saripaar validator to validate fields with NotEmpty annotations
        validator = new Validator(this);
        validator.setValidationListener(this);

        return view;
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        if (context instanceof IMobileNumberSetListener) {
            mListener = (IMobileNumberSetListener) context;
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
