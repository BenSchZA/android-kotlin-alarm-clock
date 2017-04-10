/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class ProfileActivity extends BaseActivity {

    @BindView(R.id.settings_profile_pic)
    ImageButton profilePic;
    @BindView(R.id.settings_profile_name)
    EditText profileName;
    @BindView(R.id.settings_delete_profile)
    Button deleteProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_profile);

        profileName.setText(mCurrentUser.getUser_name());
        setProfilePicFromURL(mCurrentUser.getProfile_pic());
    }

    @OnClick(R.id.settings_profile_pic)
    public void onClickProfilePic(View v) {

        String pickTitle = "Select image or take a new picture";

        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        Intent chooserIntent = Intent.createChooser(pickPhoto, pickTitle);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {takePicture});

        startActivityForResult(chooserIntent , 0);
    }

    @OnTextChanged(R.id.settings_profile_name)
    public void onTextChangedProfileName() {
        String profileNameText = profileName.getText().toString();

        DatabaseReference profileNameReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("user_name", profileNameText);

        profileNameReference.updateChildren(childUpdates);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    setProfilePicFromURI(selectedImage);
                }

                break;
            case 1:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    setProfilePicFromURI(selectedImage);
                }
                break;
        }
    }

    protected void setProfilePicFromURI(Uri uri) {

        Picasso.with(ProfileActivity.this).load(uri)
                .resize(400, 400)
                .into(profilePic, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap imageBitmap = ((BitmapDrawable) profilePic.getDrawable()).getBitmap();
                        RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                        imageDrawable.setCircular(true);
                        imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                        profilePic.setImageDrawable(imageDrawable);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    protected void setProfilePicFromURL(String url) {

        Picasso.with(ProfileActivity.this).load(url)
                .resize(400, 400)
                .into(profilePic, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap imageBitmap = ((BitmapDrawable) profilePic.getDrawable()).getBitmap();
                        RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                        imageDrawable.setCircular(true);
                        imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                        profilePic.setImageDrawable(imageDrawable);
                    }

                    @Override
                    public void onError() {

                    }
                });
    }
}
