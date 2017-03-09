package com.roostermornings.android.activity.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.domain.Channel;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.ExceptionHandler;
import com.roostermornings.android.activity.SplashActivity;
import com.roostermornings.android.node_api.IHTTPClient;
import com.roostermornings.android.util.FontsOverride;
import com.roostermornings.android.util.MyContactsController;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

public class BaseActivity extends AppCompatActivity implements Validator.ValidationListener {

    private Dialog progressDialog;
    protected FirebaseAuth mAuth;
    protected FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = BaseActivity.class.getSimpleName();
    protected DatabaseReference mDatabase;
    public static User mCurrentUser;

    @Inject
    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //If activity is not MyAlarms page, set default uncaught exception handler - this is to ensure loop does not occur
//        if(!this.getClass().getSimpleName().equals(MyAlarmsFragmentActivity.class.getSimpleName())) {
//            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
//        }

        BaseApplication baseApplication = (BaseApplication) getApplication();

        //inject Dagger dependencies
        baseApplication.getRoosterApplicationComponent().inject(this);

        //get reference to Firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //retrieve static User for current user
        retrieveMyUserDetails();

        // [START auth_state_listener]
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
//                    Intent intent = new Intent(BaseActivity.this, SplashActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
                }
            }
        };

    }

    protected void startHomeActivity() {
        //This clears the back stack when starting home activity, to stop back stack loop
        Intent homeIntent = new Intent(this, MyAlarmsFragmentActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
    }

    protected void initialize(int layoutId) {

        setContentView(layoutId);

        //Bind to butterknife delegate
        //Calls to ButterKnife.bind can be made anywhere you would otherwise put findViewById calls.
        ButterKnife.bind(this);

    }

    public IHTTPClient apiService() {

        BaseApplication baseApplication = (BaseApplication) getApplication();

        return baseApplication.getAPIService();

    }

    public FirebaseUser getFirebaseUser() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }

    protected void retrieveMyUserDetails() {
        ValueEventListener userListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCurrentUser = dataSnapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(), "Failed to load user.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        DatabaseReference thisUserReference = mDatabase
                .child("users").child(getFirebaseUser().getUid());
        thisUserReference.addValueEventListener(userListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = showIndeterminateProgress();
        } else {
            progressDialog.show();
        }

    }

    public void hideProgressDialog() {
        if (progressDialog != null) progressDialog.hide();
    }

    protected Dialog showIndeterminateProgress() {
        final Dialog dialog = new Dialog(this,
                android.R.style.Theme_Translucent);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // here we set layout of progress dialog
        //dialog.setContentView(R.layout.dialog_custom_progress);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    @Override
    public void onValidationSucceeded() {
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);

            if (view instanceof TextInputEditText) {

                TextInputLayout parent = (TextInputLayout) view.getParent();
                parent.setError(message);

            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void hideSoftKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void signOut() {
        mAuth.signOut(); //End user session
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    final public int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 10;
    public void requestPermissionReadContacts() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }
}
