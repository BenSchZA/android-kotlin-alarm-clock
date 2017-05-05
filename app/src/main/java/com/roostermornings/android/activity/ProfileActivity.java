/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import com.roostermornings.android.BaseApplication;
import com.roostermornings.android.R;
import com.roostermornings.android.activity.base.BaseActivity;
import com.roostermornings.android.dagger.RoosterApplicationComponent;
import com.roostermornings.android.util.Constants;
import com.roostermornings.android.util.MyContactsController;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static java.text.DateFormat.getDateTimeInstance;

public class ProfileActivity extends BaseActivity {

    @BindView(R.id.settings_profile_pic)
    ImageButton profilePic;
    @BindView(R.id.settings_profile_name)
    EditText profileName;
    @BindView(R.id.settings_profile_mobile_number)
    EditText profileMobileNumber;
    @BindView(R.id.toolbar_title)
    TextView toolbarTitle;

    String mCurrentPhotoPath;

    @Override
    protected void inject(RoosterApplicationComponent component) {
        component.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_profile);
        inject(((BaseApplication)getApplication()).getRoosterApplicationComponent());

        setDayNightTheme();

        //Set toolbar title
        setupToolbar(toolbarTitle, "My Profile");

        profileName.setText(mCurrentUser.getUser_name());
        profileMobileNumber.setText(mCurrentUser.getCell_number());
        if(!TextUtils.isEmpty(mCurrentUser.getProfile_pic()))
            setProfilePicFromURL(mCurrentUser.getProfile_pic());
    }

    @OnClick(R.id.settings_profile_pic)
    public void onClickProfilePic(View v) {

        //Ensure read write external permission has been granted
        requestPermission();
        if(!checkPermission()) {
            requestPermission();
            return;
        }

        String pickTitle = "Select image or take a new picture";

        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                // Error occurred while creating the File
                e.printStackTrace();
                Toast.makeText(this, "Image capture failed.", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.roostermornings.android.fileprovider",
                        photoFile);
                //If no EXTRA_OUTPUT defined, then a low res thumbnail bitmap is returned
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }
        }

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
                    Uri returnedImageURI = null;
                    if(imageReturnedIntent != null) {
                        returnedImageURI = imageReturnedIntent.getData();
                    } else if(mCurrentPhotoPath != null){
                        galleryAddPic();
                        File file = new File(mCurrentPhotoPath);
                        returnedImageURI = Uri.fromFile(file);
                    } else{
                        Toast.makeText(this, "Load image failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(returnedImageURI != null) {
                        setProfilePicFromURI(returnedImageURI);
                        uploadProfilePicture(returnedImageURI);
                    }
                }
                break;
            default:
                break;
        }
    }

    //Note: If you saved your photo to the directory provided by getExternalFilesDir(), the media scanner cannot access the files because they are private to your app.
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = getDateTimeInstance().format(new Date());
        String imageFileName = "Rooster_Profile_Pic_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
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
            selectedImage = getResizedBitmap(selectedImage, 250);
            //Convert to byte array
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();
            //Upload to firebase with putBytes
        } catch(NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
            return;
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
            return;
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

        try {
            Picasso.with(ProfileActivity.this).load(uri)
                    .resize(400, 400)
                    .centerCrop()
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
        } catch(NullPointerException e){
            e.printStackTrace();
            Toast.makeText(this, "Load image failed.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void setProfilePicFromURL(String url) {

        try {
        Picasso.with(ProfileActivity.this).load(url)
                .resize(400, 400)
                .centerCrop()
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
    } catch(NullPointerException e){
        e.printStackTrace();
        Toast.makeText(this, "Load image failed.", Toast.LENGTH_SHORT).show();
    }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new
                String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, Constants.MY_PERMISSIONS_REQUEST_CHANGE_PROFILE_PIC);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_CHANGE_PROFILE_PIC:
                if (grantResults.length > 0) {
                    boolean ReadPermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean WritePermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;
                    if (ReadPermission && WritePermission) {
                    } else {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            default:
                break;
        }
    }

    public boolean checkPermission() {
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                READ_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        return (result1 & result2) == PackageManager.PERMISSION_GRANTED;
    }
}
