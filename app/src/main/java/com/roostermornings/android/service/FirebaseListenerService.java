/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.util.Constants;

public class FirebaseListenerService extends Service {
    protected DatabaseReference mDatabase;
    protected FirebaseAuth mAuth;
    public static boolean mRunning = false;

    public FirebaseListenerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mRunning) {
            mRunning = true;
            //Reset flags - NB onChildChanged called once for each child when first called
            BaseApplication.setNotificationFlag(0, Constants.FLAG_FRIENDREQUESTS);
            //Listen for changes to Firebase user friend requests, display notification
            if (getFirebaseUser() != null) {
                mDatabase = FirebaseDatabase.getInstance().getReference();

                DatabaseReference mRequestsReference = mDatabase
                        .child("friend_requests_received").child(getFirebaseUser().getUid());

                ChildEventListener friendRequestListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        //Send broadcast message to notify all receivers of new notification
                        Intent intent = new Intent(Constants.ACTION_REQUESTNOTIFICATION);
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

                //TODO: implement badge count
//            DatabaseReference mRoosterReference = mDatabase
//                    .child("social_rooster_queue").child(getFirebaseUser().getUid());
//
//            ChildEventListener roosterListener = new ChildEventListener() {
//                @Override
//                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                    BaseActivity.setBadge(getApplicationContext(), ((BaseApplication)getApplication()).getNotificationFlag(Constants.FLAG_ROOSTERCOUNT));
//                }
//
//                @Override
//                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
//                    BaseActivity.setBadge(getApplicationContext(), ((BaseApplication)getApplication()).getNotificationFlag(Constants.FLAG_ROOSTERCOUNT));
//                }
//
//                @Override
//                public void onChildRemoved(DataSnapshot dataSnapshot) {
//                    BaseActivity.setBadge(getApplicationContext(), ((BaseApplication)getApplication()).getNotificationFlag(Constants.FLAG_ROOSTERCOUNT));
//                }
//
//                @Override
//                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
//                    BaseActivity.setBadge(getApplicationContext(), ((BaseApplication)getApplication()).getNotificationFlag(Constants.FLAG_ROOSTERCOUNT));
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//
//                }
//            };
//            mRoosterReference.addChildEventListener(roosterListener);
            }
        }

        return START_STICKY;
    }

    public FirebaseUser getFirebaseUser() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
        return mAuth.getCurrentUser();
    }
}
