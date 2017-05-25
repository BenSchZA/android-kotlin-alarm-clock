/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.firebase;

import android.content.Context;
import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

        if(fUser != null && StrUtils.notNullOrEmpty(fUser.getUid())) {
            childUpdates.put(String.format("users/%s/%s",
                    fUser.getUid(), "cell_number"), NSNNumber);
            fDB.updateChildren(childUpdates);
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
}
