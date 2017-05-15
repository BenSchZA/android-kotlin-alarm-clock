/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.fragment.base;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.Friend;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.node_api.IHTTPClient;
import com.roostermornings.android.util.Toaster;

import butterknife.ButterKnife;

public abstract class BaseFragment extends Fragment implements Validator.ValidationListener {

    protected DatabaseReference mDatabase;

    @Inject Context AppContext;
    @Inject BaseApplication baseApplication;

    public static BaseActivityListener baseActivityListener;

    protected abstract void inject(RoosterApplicationComponent component);

    public interface BaseActivityListener {
        void onValidationSucceeded();
        void onValidationFailed(List<ValidationError> errors);
        boolean checkInternetConnection();
        IHTTPClient apiService();
    }

    public boolean checkInternetConnection() {
        return baseActivityListener.checkInternetConnection();
    }

    public IHTTPClient apiService() {
        return baseActivityListener.apiService();
    }

    @Override
    public void onValidationSucceeded() {
        baseActivityListener.onValidationSucceeded();
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        baseActivityListener.onValidationFailed(errors);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        baseApplication = (BaseApplication)getActivity().getApplication();

        getDatabaseReference();

        baseApplication.getRoosterApplicationComponent().inject(this);

        try {
            baseActivityListener = (BaseActivityListener) getActivity();
        } catch (ClassCastException castException) {
            /* The activity does not implement the listener. */
        }
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

    public void showToast(Context c, String message, int toastLength) {
        try{
            Toaster.makeToast(c, message,
                    toastLength);
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    public void sortNamesFriends(ArrayList<Friend> mUsers){
        //Take arraylist and sort alphabetically
        Collections.sort(mUsers, new Comparator<Friend>() {
            @Override
            public int compare(Friend lhs, Friend rhs) {
                //If null, pretend equal
                if(lhs == null || rhs == null || lhs.getUser_name() == null || rhs.getUser_name() == null) return 0;
                return lhs.getUser_name().compareTo(rhs.getUser_name());
            }
        });
    }

    public void sortNamesUsers(ArrayList<User> mUsers){
        //Take arraylist and sort alphabetically
        Collections.sort(mUsers, new Comparator<User>() {
            @Override
            public int compare(User lhs, User rhs) {
                //If null, pretend equal
                if(lhs == null || rhs == null || lhs.getUser_name() == null || rhs.getUser_name() == null) return 0;
                return lhs.getUser_name().compareTo(rhs.getUser_name());
            }
        });
    }

    protected void startHomeActivity() {
        Intent homeIntent = new Intent(AppContext, MyAlarmsFragmentActivity.class);
        startActivity(homeIntent);
    }
}
