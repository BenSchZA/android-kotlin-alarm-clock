/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.ImageRequest;
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
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.firebase.FA;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.Toaster;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;

import static com.roostermornings.android.util.RoosterUtils.notNull;

public class SignInActivity extends BaseActivity {

    public static final String TAG = SignInActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 007;

    CallbackManager facebookCallbackManager;
    GoogleApiClient mGoogleApiClient;

    @BindView(R.id.already_user_textview)
    TextView mTextViewAlreadyUser;

    @BindView(R.id.sign_up_email_textview)
    TextView mTextViewSignUpEmail;

    @BindView(R.id.signin_button_facebook)
    Button mButtonSigninFacebook;

    @BindView(R.id.login_button)
    LoginButton facebookLoginButton;

    @BindView(R.id.signin_button_google)
    Button mButtonSigninGoogle;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_sign_in);
        inject(BaseApplication.getRoosterApplicationComponent());

        FA.Log(FA.Event.onboarding_intro_viewed.class, null, null);

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
                        Log.e("Facebook: ", "onCancel");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.e("Facebook: ", "onError");
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
        intent.putExtra(getApplicationContext().getString(R.string.extras_already_user), true);
        startActivity(intent);
    }

    @OnClick(R.id.sign_up_email_textview)
    public void onSignUpEmailClicked() {
        Intent intent = new Intent(SignInActivity.this, SignupEmailActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.terms_and_conditions_link)
    protected void onTermsAndConditionsClicked() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(getString(R.string.rooster_website_t_and_c_url)));
        startActivity(i);
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

        if (!checkInternetConnection()) return;

        //Result returned from launching Facebook authentication
        if (requestCode == CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()) {
            progressBar.setVisibility(View.VISIBLE);
            facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            progressBar.setVisibility(View.VISIBLE);
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    //Facebook
    private void handleFacebookAccessToken(AccessToken token) {
        if (!checkInternetConnection()) return;

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
                                //photoURLString = mAuth.getCurrentUser().getPhotoUrl().toString();
                                int dimensionPixelSize = getResources().getDimensionPixelSize(com.facebook.R.dimen.com_facebook_profilepictureview_preset_size_normal);
                                Profile profile = Profile.getCurrentProfile();
                                photoURLString = ImageRequest.getProfilePictureUri(profile.getId(), dimensionPixelSize, dimensionPixelSize).toString();
                            }catch (java.lang.NullPointerException e){
                                e.printStackTrace();
                                photoURLString = null;
                            }

                            if(mAuth.getCurrentUser() == null) return;
                            User user = new User(null,
                                    "android",
                                    deviceToken,
                                    photoURLString,
                                    notNull(mAuth.getCurrentUser().getDisplayName()) ? mAuth.getCurrentUser().getDisplayName():"",
                                    "",
                                    notNull(mAuth.getCurrentUser().getUid()) ? mAuth.getCurrentUser().getUid():null,
                                    null,
                                    0,
                                    null);

                            //Note: "friends" and "cell_number" node not changed TODO: should profile pic be kept?
                            Map<String, Object> childUpdates = new HashMap<>();
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

                        } else{
                            //Remove progress bar on failure
                            progressBar.setVisibility(View.GONE);

                            Log.w(TAG, "Facebook: signInWithCredential", task.getException());
                            Toaster.makeToast(SignInActivity.this, notNull(task.getException().getMessage()) ? task.getException().getMessage():"Facebook sign-in failed.",
                                    Toast.LENGTH_LONG);
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
            Toaster.makeToast(SignInActivity.this, "Google sign-in failed.",
            Toast.LENGTH_LONG);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInResult result) {
        if (!checkInternetConnection()) return;

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
                        if (task.isSuccessful()) {
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            FirebaseDatabase database = FirebaseDatabase.getInstance();

                            String photoURLString;
                            photoURLString = notNull(account.getPhotoUrl()) ? account.getPhotoUrl().toString():null;

                            if(mAuth.getCurrentUser() == null) return;
                            User user = new User(null,
                                    "android",
                                    deviceToken,
                                    photoURLString,
                                    notNull(mAuth.getCurrentUser().getDisplayName()) ? mAuth.getCurrentUser().getDisplayName():"",
                                    "",
                                    notNull(mAuth.getCurrentUser().getUid()) ? mAuth.getCurrentUser().getUid():null,
                                    null,
                                    0,
                                    null);

                            //Note: "friends" and "cell_number" node not changed TODO: should profile pic be kept?
                            Map<String, Object> childUpdates = new HashMap<>();
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

                        } else{
                            //Remove progress bar on failure
                            progressBar.setVisibility(View.GONE);

                            Log.w(TAG, "Google: signInWithCredential", task.getException());
                            Toaster.makeToast(SignInActivity.this, notNull(task.getException().getMessage()) ? task.getException().getMessage():"Google sign-in failed.",
                                    Toast.LENGTH_LONG);
                        }
                    }
                });
    }

    //On successful authentication, proceed to alarms activity
    private void proceedToMyAlarmsActivity() {
        //Remove progress bar on success
        progressBar.setVisibility(View.GONE);

        //TODO: go to alarm creation for new user?
        Intent intent = new Intent(SignInActivity.this, MyAlarmsFragmentActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        FA.Log(FA.Event.onboarding_first_entry.class, null, null);
        finish();
    }
}
