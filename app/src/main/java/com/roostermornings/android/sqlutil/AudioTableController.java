/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.sqlutil;

import android.content.Context;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import static com.roostermornings.android.activity.base.BaseActivity.mCurrentUser;

public final class AudioTableController {

    private Context context;

    public AudioTableController(Context context) {
        this.context = context;
    }

    //Ensure used on social roosters
    public void setListened(String senderId, String queueId) {
        DatabaseReference socialRoosterUploadsReference = FirebaseDatabase.getInstance().getReference()
                .child("social_rooster_uploads").child(senderId).child(queueId);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("listened", true);

        socialRoosterUploadsReference.updateChildren(childUpdates);
    }
}
