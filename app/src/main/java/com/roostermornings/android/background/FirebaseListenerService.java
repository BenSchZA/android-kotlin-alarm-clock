/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.background;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.Friend;

public class FirebaseListenerService extends Service {
    protected DatabaseReference mDatabase;
    protected FirebaseAuth mAuth;

    public FirebaseListenerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Listen for changes to Firebase user friend requests, display notification
        if(getFirebaseUser() != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();

            DatabaseReference mRequestsReference = mDatabase
                    .child("friend_requests_received").child(getFirebaseUser().getUid());

            ChildEventListener friendRequestListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //Send broadcast message to notify all receivers of new notification
                    Intent intent = new Intent("rooster.update.NOTIFICATION");
                    sendBroadcast(intent);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            mRequestsReference.addChildEventListener(friendRequestListener);
        }

        return START_STICKY;
    }

    public FirebaseUser getFirebaseUser() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }
}
