/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.base;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Friend;

import butterknife.ButterKnife;

public class BaseFragment extends Fragment implements Validator.ValidationListener {

    protected DatabaseReference mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getDatabaseReference();

        ((BaseApplication) getActivity().getApplication()).getRoosterApplicationComponent().inject(this);

    }

    protected View initiate(LayoutInflater inflater, int resource, ViewGroup root, boolean attachToRoot){

        View view = inflater.inflate(resource, root, attachToRoot);
        ButterKnife.bind(this, view);
        return view;

    }

    protected DatabaseReference getDatabaseReference() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        return mDatabase;
    }

    @Override
    public void onValidationSucceeded() {
        ((BaseActivity) getActivity()).onValidationSucceeded();
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        //TODO: fix crash here
        ((BaseActivity) getActivity()).onValidationFailed(errors);

    }

    protected void hideSoftKeyboard() {
        ((BaseActivity) getActivity()).hideSoftKeyboard();
    }

    protected FirebaseUser getFirebaseUser() {
        return ((BaseActivity) getActivity()).getFirebaseUser();
    }

    public void showToast(Context c, String message, int toastLength) {
        try{
            Toast.makeText(c, message,
                    toastLength).show();
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    public void sortNames(ArrayList<Friend> mUsers){
        //Take arraylist and sort alphabetically
        Collections.sort(mUsers, new Comparator<Friend>() {
            @Override
            public int compare(Friend lhs, Friend rhs) {
                return lhs.getUser_name().compareTo(rhs.getUser_name());
            }
        });
    }
}
