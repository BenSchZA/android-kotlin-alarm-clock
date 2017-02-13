package com.roostermornings.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;

import java.util.List;

import javax.inject.Inject;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.activity.base.BaseActivity;

public class BaseFragment extends Fragment implements Validator.ValidationListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((BaseApplication) getActivity().getApplication()).getRoosterApplicationComponent().inject(this);
    }

    @Override
    public void onValidationSucceeded() {
        ((BaseActivity) getActivity()).onValidationSucceeded();
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {

        ((BaseActivity) getActivity()).onValidationFailed(errors);

    }

    protected void hideSoftKeyboard() {

        ((BaseActivity) getActivity()).hideSoftKeyboard();

    }

}
