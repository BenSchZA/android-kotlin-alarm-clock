/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity.base;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
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
import com.roostermornings.android.R;
import com.roostermornings.android.activity.MyAlarmsFragmentActivity;
import com.roostermornings.android.activity.SplashActivity;
import com.roostermornings.android.receiver.BackgroundTaskReceiver;
import com.roostermornings.android.service.FirebaseListenerService;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.node_api.IHTTPClient;
import com.roostermornings.android.sqlutil.AudioTableManager;
import com.roostermornings.android.sqlutil.DeviceAlarmController;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.InternetHelper;
import com.roostermornings.android.util.RoosterUtils;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

public class BaseActivity extends AppCompatActivity implements Validator.ValidationListener {

    private Dialog progressDialog;
    public FirebaseAuth mAuth;
    protected FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = BaseActivity.class.getSimpleName();

    private static Calendar calendar = Calendar.getInstance();

    public DatabaseReference mDatabase;

    public static User mCurrentUser;

    @Inject
    public SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BaseApplication baseApplication = (BaseApplication) getApplication();

        //inject Dagger dependencies
        baseApplication.getRoosterApplicationComponent().inject(this);

        //Set default application settings preferences - don't overwrite existing if false
        setPreferenceManagerDefaultSettings(false);

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
                    //Start Firebase listeners applicable to all activities - primarily to update notifications
                    if(!isServiceRunning(FirebaseListenerService.class))
                        startService(new Intent(getApplicationContext(), FirebaseListenerService.class));
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

    public BaseActivity(){}

    private void setPreferenceManagerDefaultSettings(Boolean overwrite) {
        // ensure user settings are set to default once when new user
        // As long as you set the third argument to false, you can safely call this method
        // every time your activity starts without overriding the user's saved preferences
        // by resetting them to the defaults. However, if you set it to true, you will
        // override any previous values with the defaults.

        if(overwrite) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("pref_key_user_settings").apply();
        }
        PreferenceManager.setDefaultValues(this, R.xml.application_user_settings, overwrite);
    }

    public boolean checkInternetConnection() {
        if(!checkMobileDataConnection()) {
            Toast.makeText(getApplicationContext(), "'Download/upload on mobile data' is disabled in Settings, " +
                    "please enable this and try again.", Toast.LENGTH_LONG).show();
        }

        if (this.noInternetConnection()) {
            Toast.makeText(getApplicationContext(), "No internet connection was found, please " +
                    "connect and try again.", Toast.LENGTH_LONG).show();
            return false;
        }

        //checkFirebaseConnection(); #TODO - check why this is randomly failing
        return true;
    }

    public boolean checkMobileDataConnection() {
        if (mobileDataConnection() && !sharedPreferences.getBoolean(Constants.USER_SETTINGS_DOWNLOAD_ON_DATA, true)) {
            //TODO: maybe display this once?
//            Toast.makeText(getApplicationContext(), "'Download on mobile data' is disabled in Settings, " +
//                    "please enable this and try again.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    protected void checkFirebaseConnection() {

        FirebaseDatabase.getInstance().getReference(".info/connected")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.getValue(Boolean.class)) {
                            Log.i(TAG, "Firebase CONNECTED");
                        } else {
                            Log.i(TAG, "Firebase NOT CONNECTED");
                            Toast.makeText(getApplicationContext(), "The application could not connect to the " +
                                    "Rooster backend, please check your internet connection and try again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "onCancelled: ", error.toException());
                    }
                });


    }

    protected boolean noInternetConnection() {
        return InternetHelper.noInternetConnection(this);
    }

    protected boolean mobileDataConnection() {
        return  InternetHelper.mobileDataConnection(this);
    }

    protected void startHomeActivity() {
        Intent homeIntent = new Intent(this, MyAlarmsFragmentActivity.class);
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

        if (getFirebaseUser() != null) {
            DatabaseReference thisUserReference = mDatabase
                    .child("users").child(getFirebaseUser().getUid());
            thisUserReference.addValueEventListener(userListener);
        }
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
        //Ensure no audio remaining from old user
        AudioTableManager audioTableManager = new AudioTableManager(this);
        audioTableManager.clearAudioFiles();
        //Ensure no alarms left from old user
        DeviceAlarmController deviceAlarmController = new DeviceAlarmController(this);
        deviceAlarmController.deleteAlarmsLocal();
        //End user session
        mAuth.signOut();
        //Cancel background task intents
        BackgroundTaskReceiver backgroundTaskReceiver = new BackgroundTaskReceiver();
        backgroundTaskReceiver.scheduleBackgroundCacheFirebaseData(this, false);
        backgroundTaskReceiver.scheduleBackgroundDailyTask(this, false);
        backgroundTaskReceiver.scheduleBackgroundUpdateNotificationsTask(this, false);
        //Set default application settings preferences - don't overwrite existing if false
        setPreferenceManagerDefaultSettings(true);
        //Clear shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //TODO:
        editor.apply();
        //Go to splash activity and onboarding
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void setBadge(Context context, int count) {
        String launcherClassName = getLauncherClassName(context);
        if (launcherClassName == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        intent.putExtra("badge_count", count);
        intent.putExtra("badge_count_package_name", context.getPackageName());
        intent.putExtra("badge_count_class_name", launcherClassName);
        context.sendBroadcast(intent);
    }

    public static String getLauncherClassName(Context context) {

        PackageManager pm = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resolveInfos) {
            String pkgName = resolveInfo.activityInfo.applicationInfo.packageName;
            if (pkgName.equalsIgnoreCase(context.getPackageName())) {
                String className = resolveInfo.activityInfo.name;
                return className;
            }
        }
        return null;
    }

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
                        Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    public void requestPermissionIgnoreBatteryOptimization(Context context) {
        //Google wasn't happy with us programatically requesting this permission, so a dialog will have to do
//        String packageName = getPackageName();
//        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        RoosterUtils.hasM() && pm.isIgnoringBatteryOptimizations(packageName) &&

        if(!sharedPreferences.getBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, false)) {
            View dialogMmpView = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_permissions_explainer, null);
            new MaterialDialog.Builder(context)
                    .customView(dialogMmpView, false)
                    .neutralText(R.string.neutral)
                    .negativeColorRes(R.color.grey)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(Constants.PERMISSIONS_DIALOG_OPTIMIZATION, true);
                            editor.commit();
                            startActivity(getIntent());
                        }
                    })
                    .show();
        }
    }

    //TODO: notify user to add protected app if exists - this add not working
    public void requestPermissionProtectActivity() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanger.optimize.process.ProtectActivity"));
        startActivity(intent);
    }

    public void setDayNight() {
        try {
            if (calendar.get(Calendar.HOUR_OF_DAY) >= 17) {
                if(findViewById(R.id.activity_content) != null) findViewById(R.id.activity_content).setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.main_background_layer_list_night, null));
                if(findViewById(R.id.toolbar) != null) findViewById(R.id.toolbar).setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.rooster_dark_blue, null));
                if(findViewById(R.id.tabs) != null) findViewById(R.id.tabs).setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.rooster_dark_blue, null));
            } else if (calendar.get(Calendar.HOUR_OF_DAY) > 7) {
                if(findViewById(R.id.activity_content) != null) findViewById(R.id.activity_content).setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.main_background_layer_list_day, null));
                if(findViewById(R.id.toolbar) != null) findViewById(R.id.toolbar).setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.rooster_blue, null));
                if(findViewById(R.id.tabs) != null) findViewById(R.id.tabs).setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.rooster_blue, null));
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
