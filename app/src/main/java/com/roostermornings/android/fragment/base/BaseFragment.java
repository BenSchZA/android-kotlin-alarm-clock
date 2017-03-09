package com.roostermornings.android.fragment.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;

import java.util.List;

import javax.inject.Inject;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;

import butterknife.ButterKnife;

public class BaseFragment extends Fragment implements Validator.ValidationListener {

    protected DatabaseReference mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        ((BaseApplication) getActivity().getApplication()).getRoosterApplicationComponent().inject(this);

    }

    protected View initiate(LayoutInflater inflater, int resource, ViewGroup root, boolean attachToRoot){

        View view = inflater.inflate(resource, root, attachToRoot);
        ButterKnife.bind(this, view);
        return view;

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

    protected FirebaseUser getFirebaseUser() {
        return ((BaseActivity) getActivity()).getFirebaseUser();
    }

}
