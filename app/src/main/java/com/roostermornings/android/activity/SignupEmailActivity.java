/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.User;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;

public class SignupEmailActivity extends BaseActivity implements Validator.ValidationListener {

    public static final String TAG = SignupEmailActivity.class.getSimpleName();

    @NotEmpty
    @BindView(R.id.signup_username_edittext)
    EditText mUserName;

    @BindView(R.id.signup_email_address_edittext)
    EditText mEmailAddress;

    @Password(min = 6, scheme = Password.Scheme.ALPHA_NUMERIC)
    @BindView(R.id.signup_password_edittext)
    EditText mPassword;

    Validator validator;
    String mMobileNumber = "";
    boolean mAlreadyUser = false;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_signup_email);
        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

        //instantiate saripaar validator to validate fields with NotEmpty annotations
        validator = new Validator(this);
        validator.setValidationListener(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mMobileNumber = bundle.getString(getApplicationContext().getString(R.string.extras_mobile_number), "");
            mAlreadyUser = bundle.getBoolean(getApplicationContext().getString(R.string.extras_already_user), false);

            if (mAlreadyUser) {
                mUserName.setVisibility(View.INVISIBLE);

            }

        }

    }

    @OnClick(R.id.signup_button_email)
    public void onSignupButtonClicked() {

        validator.validate();

    }

    @Override
    public void onValidationSucceeded() {

        if (!checkInternetConnection()) return;

        final String email = mEmailAddress.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        final String name = mUserName.getText().toString().trim();

        if (mAlreadyUser) {

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                            if (!task.isSuccessful()) {

                                Toast.makeText(SignupEmailActivity.this, R.string.signup_auth_failed,
                                        Toast.LENGTH_LONG).show();

                            } else {
                                String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                database.getReference(String.format("users/%s/device_type", mAuth.getCurrentUser().getUid())).setValue("android");
                                database.getReference(String.format("users/%s/device_token", mAuth.getCurrentUser().getUid())).setValue(deviceToken);
                                database.getReference(String.format("users/%s/cell_number", mAuth.getCurrentUser().getUid())).setValue(mMobileNumber);
                                database.getReference(String.format("users/%s/unseen_roosters", mAuth.getCurrentUser().getUid())).setValue(0);

                                //Add user as a friend of theirs
                                database.getReference(String.format("users/%s/%s/%s", mAuth.getCurrentUser().getUid(), "friends", mAuth.getCurrentUser().getUid())).setValue(true);

                                proceedToMyAlarmsActivity();
                            }
                        }
                    });


        } else {

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                            if (!task.isSuccessful()) {
                                Toast.makeText(SignupEmailActivity.this, task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            } else {

                                String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                FirebaseDatabase database = FirebaseDatabase.getInstance();

                                User user = new User(null,
                                        "android",
                                        deviceToken,
                                        "",
                                        name,
                                        mMobileNumber,
                                        mAuth.getCurrentUser().getUid(),
                                        null,
                                        0);

                                //Note: "friends" node not changed TODO: should profile pic be kept?
                                Map<String, Object> childUpdates = new HashMap<>();
                                childUpdates.put(String.format("users/%s/%s",
                                        mAuth.getCurrentUser().getUid(), "cell_number"), user.getCell_number());
                                childUpdates.put(String.format("users/%s/%s",
                                        mAuth.getCurrentUser().getUid(), "device_token"), user.getDevice_token());
                                childUpdates.put(String.format("users/%s/%s",
                                        mAuth.getCurrentUser().getUid(), "device_type"), user.getDevice_type());
                                childUpdates.put(String.format("users/%s/%s",
                                        mAuth.getCurrentUser().getUid(), "profile_pic"), user.getProfile_pic());
                                childUpdates.put(String.format("users/%s/%s",
                                        mAuth.getCurrentUser().getUid(), "uid"), user.getUid());
                                childUpdates.put(String.format("users/%s/%s",
                                        mAuth.getCurrentUser().getUid(), "user_name"), user.getUser_name());
                                childUpdates.put(String.format("users/%s/%s",
                                        mAuth.getCurrentUser().getUid(), "unseen_roosters"), user.getUnseen_roosters());

                                //Add user as a friend of theirs
                                childUpdates.put(String.format("users/%s/%s/%s", mAuth.getCurrentUser().getUid(), "friends", mAuth.getCurrentUser().getUid()), true);

                                mDatabase.updateChildren(childUpdates);

                                proceedToMyAlarmsActivity();

                            }

                        }
                    });
        }


    }

    private void proceedToMyAlarmsActivity() {

        if (!checkInternetConnection()) return;

        Intent intent = new Intent(SignupEmailActivity.this, MyAlarmsFragmentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
