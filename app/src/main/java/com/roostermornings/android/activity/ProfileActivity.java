/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.domain.User;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.MyContactsController;
import com.roostermornings.android.util.RoosterUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
//    @BindView(R.id.settings_delete_profile)
//    Button deleteProfile;
    @BindView(R.id.settings_profile_mobile_number)
    EditText profileMobileNumber;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_profile);

        //Set toolbar title
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbarTitle.setText("My Profile");

        profileName.setText(mCurrentUser.getUser_name());
        profileMobileNumber.setText(mCurrentUser.getCell_number());
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

        DatabaseReference profileReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("user_name", profileNameText);

        profileReference.updateChildren(childUpdates);
    }

    @OnTextChanged(R.id.settings_profile_mobile_number)
    public void onTextChangedProfileMobileNumber() {
        String profileMobileNumberText = profileMobileNumber.getText().toString();
        MyContactsController myContactsController = new MyContactsController(this);
        String NSNNumber;
        if(profileMobileNumberText.length() > 0) {
            NSNNumber = myContactsController.processContactCountry(profileMobileNumberText);
        } else{
            NSNNumber = profileMobileNumberText;
        }

        DatabaseReference profileReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("cell_number", NSNNumber);

        profileReference.updateChildren(childUpdates);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){
                    Uri returnedImage = imageReturnedIntent.getData();
                    setProfilePicFromURI(returnedImage);
                    uploadProfilePicture(returnedImage);
                }

                break;
            case 1:
                if(resultCode == RESULT_OK){
                    Uri returnedImage = imageReturnedIntent.getData();
                    setProfilePicFromURI(returnedImage);
                    uploadProfilePicture(returnedImage);
                }
                break;
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void uploadProfilePicture(Uri returnedImage) {
        byte[] byteArray = new byte[0];
        try {
            InputStream imageStream = getContentResolver().openInputStream(returnedImage);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            byteArray = new byte[selectedImage.getByteCount()];
            //Resize bitmap
            selectedImage = getResizedBitmap(selectedImage, 400);
            //Convert to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();
            //Upload to firebase with putBytes
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }

        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference();
        StorageReference photoFileRef = mStorageRef.child(Constants.STORAGE_USER_PROFILE_PICTURE + mCurrentUser.getUid());

        UploadTask uploadTask = photoFileRef.putBytes(byteArray);

        uploadTask
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri firebaseStorageURL = taskSnapshot.getDownloadUrl();
                        updateProfilePictureEntry(firebaseStorageURL);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful upload
                        Toast.makeText(getApplicationContext(), "Error uploading!", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateProfilePictureEntry(Uri url) {
        DatabaseReference profileReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(mCurrentUser.getUid());

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("profile_pic", url.toString());

        profileReference.updateChildren(childUpdates);
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
