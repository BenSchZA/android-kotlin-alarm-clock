/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class RoosterFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) return;
        FirebaseUser user = auth.getCurrentUser();


        try {
            String deviceToken = FirebaseInstanceId.getInstance().getToken();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference(String.format("users/%s/device_type", user.getUid())).setValue("android");
            database.getReference(String.format("users/%s/device_token", user.getUid())).setValue(deviceToken);
        } catch(NullPointerException e) {
            e.printStackTrace();
        }
    }
}