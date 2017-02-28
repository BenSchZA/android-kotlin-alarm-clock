package com.roostermornings.android.activity;

import android.content.Intent;
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
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SignInActivity extends BaseActivity {

    public static final String TAG = SignInActivity.class.getSimpleName();

    private int RC_SIGN_IN = 1234;

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
                null
        );
        //Arrays.asList("user_photos", "email", "user_birthday", "public_profile")
    }

    @OnClick(R.id.signin_button_google)
    public void onGoogleSigninButtonClick(View v) {
        signIn();
    }

    //Receive auth activity result and start callback
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    //Facebook
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //Google
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
//            GoogleSignInAccount acct = result.getSignInAccount();
//            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
//            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
//            updateUI(false);
        }
    }
}
