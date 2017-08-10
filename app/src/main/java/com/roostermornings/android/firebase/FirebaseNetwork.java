/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.MyContactsController;
import com.roostermornings.android.util.StrUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class FirebaseNetwork {

    public static void updateLastSeen() {
        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        Calendar calendar = Calendar.getInstance();

        Map<String, Object> childUpdates = new HashMap<>();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        if(fUser != null && StrUtils.notNullOrEmpty(fUser.getUid())) {
            childUpdates.put(String.format("users/%s/%s",
                    fUser.getUid(), "last_seen"), calendar.getTimeInMillis());
            fDB.updateChildren(childUpdates);
        }
    }

    public static void updateProfileUserName(String userName) {
        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        Map<String, Object> childUpdates = new HashMap<>();

        if(fUser != null && StrUtils.notNullOrEmpty(fUser.getUid())) {
            childUpdates.put(String.format("users/%s/%s",
                    fUser.getUid(), "user_name"), userName);
            fDB.updateChildren(childUpdates);
        }
    }

    public static void updateProfileCellNumber(Context context, String cellNumberString) {
        MyContactsController myContactsController = new MyContactsController(context);
        String NSNNumber;
        NSNNumber = StrUtils.notNullOrEmpty(cellNumberString) ? myContactsController.processUserContactNumber(cellNumberString) : "";

        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        Map<String, Object> childUpdates = new HashMap<>();

        if (fUser != null && StrUtils.notNullOrEmpty(fUser.getUid())) {
            childUpdates.put(String.format("users/%s/%s",
                    fUser.getUid(), "cell_number"), NSNNumber);
            fDB.updateChildren(childUpdates);

            if (StrUtils.notNullOrEmpty(NSNNumber)) {
                SharedPreferences.Editor editor = context.getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit();
                editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, true);
                editor.apply();
            } else {
                SharedPreferences.Editor editor = context.getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit();
                editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, false);
                editor.apply();
            }
        }
    }

    private static OnFlagValidMobileNumberCompleteListener onFlagValidMobileNumberCompleteListener;

    public interface OnFlagValidMobileNumberCompleteListener {
        void onEvent(boolean valid);
    }

    public static void setOnFlagValidMobileNumberCompleteListener(OnFlagValidMobileNumberCompleteListener listener) {
        onFlagValidMobileNumberCompleteListener = listener;
    }

    public static void flagValidMobileNumber(final Context context, final boolean notify) {
        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        if(fUser != null && StrUtils.notNullOrEmpty(fUser.getUid())) {
            DatabaseReference mMobileNumberReference = fDB
                    .child("users").child(fUser.getUid()).child("cell_number");

            mMobileNumberReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String userCellNumber = dataSnapshot.getValue(String.class);

                    boolean valid;
                    SharedPreferences.Editor editor = context.getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit();
                    if(!StrUtils.notNullOrEmpty(userCellNumber)) {
                        editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, false);
                        editor.apply();
                        valid = false;
                    } else {
                        editor.putBoolean(Constants.MOBILE_NUMBER_VALIDATED, true);
                        editor.apply();
                        valid = true;

                        FA.Log(FA.Event.onboarding_number_provided.class, null, null);
                    }

                    if(notify && onFlagValidMobileNumberCompleteListener != null)
                        onFlagValidMobileNumberCompleteListener.onEvent(valid);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public static void updateProfileProfilePic(Uri url) {
        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        Map<String, Object> childUpdates = new HashMap<>();

        if(fUser != null && StrUtils.notNullOrEmpty(fUser.getUid())) {
            childUpdates.put(String.format("users/%s/%s",
                    fUser.getUid(), "profile_pic"), url.toString());
            fDB.updateChildren(childUpdates);
        }
    }

    public static void updateFirebaseAlarmEnabled(String setId, boolean enabled) {
        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        Map<String, Object> childUpdates = new HashMap<>();

        if(fUser != null && StrUtils.notNullOrEmpty(fUser.getUid()) && StrUtils.notNullOrEmpty(setId)) {
            childUpdates.put(String.format("alarms/%s/%s/%s",
                    fUser.getUid(), setId, "enabled"), enabled);
            fDB.updateChildren(childUpdates);
        }
    }

    public static void removeFirebaseAlarm(String setId) {
        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

        if(fUser != null && StrUtils.notNullOrEmpty(fUser.getUid()) && StrUtils.notNullOrEmpty(setId)) {
            fDB.child("alarms").child(fUser.getUid()).child(setId).removeValue();
        }
    }

    //Ensure used on social roosters
    public static void setListened(String senderId, String queueId) {
        DatabaseReference fDB = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> childUpdates = new HashMap<>();

        if(StrUtils.notNullOrEmpty(senderId) && StrUtils.notNullOrEmpty(queueId)) {
            childUpdates.put(String.format("social_rooster_uploads/%s/%s/%s",
                    senderId, queueId, "listened"), true);
            fDB.updateChildren(childUpdates);
        }
    }
}
