/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignInActivity extends BaseActivity {

    public static final String TAG = SignInActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 007;

    String mMobileNumber = "";

    CallbackManager facebookCallbackManager;
    GoogleApiClient mGoogleApiClient;

    @BindView(R.id.already_user_textview)
    TextView mTextViewAlreadyUser;

    @BindView(R.id.signin_button_email)
    Button mButtonSigninEmail;

    @BindView(R.id.signin_button_facebook)
    Button mButtonSigninFacebook;

    @BindView(R.id.login_button)
    LoginButton facebookLoginButton;

    @BindView(R.id.signin_button_google)
    Button mButtonSigninGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_sign_in);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mMobileNumber = bundle.getString(getApplicationContext().getString(R.string.extras_mobile_number));
        }

        //Facebook
        facebookLoginButton.setReadPermissions("email", "public_profile");
        facebookCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(
                facebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                    }
                }
        );

        //Google
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, null  /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @OnClick(R.id.already_user_textview)
    public void onAlreadyUserClicked() {

        Intent intent = new Intent(SignInActivity.this, SignupEmailActivity.class);
        intent.putExtra(getApplicationContext().getString(R.string.extras_mobile_number), mMobileNumber);
        intent.putExtra(getApplicationContext().getString(R.string.extras_already_user), true);
        startActivity(intent);

    }

    @OnClick(R.id.signin_button_email)
    public void onEmailSigninButtonClick() {

        Intent intent = new Intent(SignInActivity.this, SignupEmailActivity.class);
        intent.putExtra(getApplicationContext().getString(R.string.extras_mobile_number), mMobileNumber);
        startActivity(intent);
    }

    @OnClick(R.id.signin_button_facebook)
    public void onFacebookSigninButtonClick(View v) {
        LoginManager.getInstance().logInWithReadPermissions(
                this,
                Arrays.asList("public_profile")
        );
        //Arrays.asList("user_photos", "email", "user_birthday", "public_profile")
    }

    @OnClick(R.id.signin_button_google)
    public void onGoogleSigninButtonClick(View v) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    //Receive auth activity result and start callback
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Result returned from launching Facebook authentication
        if (requestCode == CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()) {
            facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    //Facebook
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (task.isSuccessful()) {
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            FirebaseDatabase database = FirebaseDatabase.getInstance();

                            String photoURLString;
                            try {
                                photoURLString = mAuth.getCurrentUser().getPhotoUrl().toString();
                            }catch (java.lang.NullPointerException e){
                                e.printStackTrace();
                                photoURLString = null;
                            }

                            User user = new User(null,
                                    "android",
                                    deviceToken,
                                    photoURLString,
                                    mAuth.getCurrentUser().getDisplayName(),
                                    mMobileNumber,
                                    mAuth.getCurrentUser().getUid(),
                                    null);

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

                            mDatabase.updateChildren(childUpdates);

                            proceedToMyAlarmsActivity();

                        } else{
                            Log.w(TAG, "Facebook: signInWithCredential", task.getException());
                            Toast.makeText(SignInActivity.this, task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    //Google
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Google Sign In was successful, authenticate with Firebase
            firebaseAuthWithGoogle(result);
        } else {
            // Signed out, show unauthenticated UI.
//            updateUI(false);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInResult result) {
        final GoogleSignInAccount account = result.getSignInAccount();
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

        final AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Google: signInWithCredential", task.getException());
                            Toast.makeText(SignInActivity.this, task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        } else{
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            FirebaseDatabase database = FirebaseDatabase.getInstance();

                            String photoURLString;
                            try {
                                photoURLString = mAuth.getCurrentUser().getPhotoUrl().toString();
                            }catch (java.lang.NullPointerException e){
                                e.printStackTrace();
                                photoURLString = null;
                            }

                            User user = new User(null,
                                    "android",
                                    deviceToken,
                                    photoURLString,
                                    mAuth.getCurrentUser().getDisplayName(),
                                    mMobileNumber,
                                    mAuth.getCurrentUser().getUid(),
                                    null);

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

                            mDatabase.updateChildren(childUpdates);

                            proceedToMyAlarmsActivity();
                        }
                    }
                });
    }

    //On successful authentication, proceed to alarms activity
    private void proceedToMyAlarmsActivity() {
        //TODO: go to alarm creation for new user?
        Intent intent = new Intent(SignInActivity.this, MyAlarmsFragmentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
