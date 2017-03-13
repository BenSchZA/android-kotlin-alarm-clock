/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roostermornings.android.R;

public class NewAudioFriendsListHolder extends RecyclerView.ViewHolder {
    private static final String TAG = NewAudioFriendsListHolder.class.getSimpleName();
    public ImageView imgProfilePic;
    public TextView txtName;
    public Button btnAdd;

    public NewAudioFriendsListHolder(View itemView) {
        super(itemView);
        imgProfilePic = (ImageView) itemView.findViewById(R.id.new_audio_friend_profile_pic);
        txtName = (TextView) itemView.findViewById(R.id.new_audio_friend_profile_name);
        btnAdd = (Button) itemView.findViewById(R.id.new_audio_friend_add);
    }
}
