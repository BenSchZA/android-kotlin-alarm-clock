/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

import com.google.firebase.storage.FirebaseStorage
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.firebase.FirebaseNetwork
import com.roostermornings.android.onboarding.CustomCommandInterface
import com.roostermornings.android.onboarding.InterfaceCommands
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.MyContactsController
import com.roostermornings.android.util.StrUtils
import com.roostermornings.android.util.Toaster
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Date

import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import com.roostermornings.android.domain.database.User
import com.roostermornings.android.onboarding.ProfileCreationFragment
import com.roostermornings.android.keys.PrefsKey
import com.roostermornings.android.keys.RequestCode
import kotlinx.android.synthetic.main.custom_toolbar.*
import java.text.DateFormat.getDateTimeInstance

class ProfileActivity : BaseActivity(), CustomCommandInterface {

    @BindView(R.id.settings_profile_pic)
    lateinit var profilePic: ImageButton
    @BindView(R.id.settings_profile_name)
    lateinit var profileName: EditText
    @BindView(R.id.settings_profile_mobile_number)
    lateinit var profileMobileNumber: EditText
    @BindView(R.id.toolbar_title)
    lateinit var toolbarTitle: TextView

    private var mCurrentPhotoPath = ""
    private var profileMobileNumberText = ""

    private var mCurrentUser: User? = null

    private var profileCreationFragment: ProfileCreationFragment? = null

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(R.layout.activity_profile)
        BaseApplication.roosterApplicationComponent.inject(this)

        FirebaseNetwork.getRoosterUser(firebaseUser?.uid) {
            it?.let {
                mCurrentUser = it
                profileName.setText(it.user_name)
                profileMobileNumber.setText(it.cell_number)
                updateProfilePicFromCurrentUser()
            }
        }

        if(!authManager.isUserSignedIn()) {
            appbar.visibility = View.GONE

            profileCreationFragment = ProfileCreationFragment.newInstance(ProfileCreationFragment.Companion.Source.PROFILE)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fillerContainer, profileCreationFragment)
                    .commit()
        }

        //Set toolbar title
        val toolbar = setupToolbar(toolbarTitle, "My Profile")
        toolbar?.setNavigationIcon(R.drawable.md_nav_back)
        toolbar?.setNavigationOnClickListener { startHomeActivity() }

        //Set mobile number to last valid persisted entry, or to current user's number if that fails
        val mobileNumberEntry = sharedPreferences.getString(PrefsKey.MOBILE_NUMBER_ENTRY.name, mCurrentUser?.cell_number)
        profileMobileNumber.setText(mobileNumberEntry)
    }

    public override fun onPause() {
        super.onPause()

        if (profileMobileNumberText.isNotBlank()) {
            FirebaseNetwork.updateProfileCellNumber(this, profileMobileNumberText)
            //Persist last valid mobile number entry
            sharedPreferences
                    .edit()
                    .putString(PrefsKey.MOBILE_NUMBER_ENTRY.name, profileMobileNumberText)
                    .apply()
        }
    }

    override fun onCustomCommand(command: InterfaceCommands.Companion.Command) {
        when(command) {
            // When sign-in complete, remove fragment
            InterfaceCommands.Companion.Command.PROCEED -> {
                if(authManager.isUserSignedIn()) {
                    appbar.visibility = View.VISIBLE

                    profileCreationFragment?.let {
                        supportFragmentManager
                                .beginTransaction()
                                .remove(it)
                                .commit()
                    }

                    recreate()
                } else {
                    finish()
                }
            }
            else -> {}
        }
    }

    private fun updateProfilePicFromCurrentUser() {
        if (mCurrentUser?.profile_pic?.isNotBlank() == true)
            setProfilePicFromURL(mCurrentUser?.profile_pic as String)
    }

    @OnClick(R.id.settings_profile_pic)
    fun onClickProfilePic(v: View) {

        //Ensure read write external permission has been granted
        requestPermission()
        if (!checkPermission()) {
            requestPermission()
            return
        }

        val pickTitle = "Select image or take a new picture"

        val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePicture.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (e: IOException) {
                // Error occurred while creating the File
                e.printStackTrace()
                Toaster.makeToast(this, "Image capture failed.", Toast.LENGTH_SHORT).checkTastyToast()
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(this,
                        "com.roostermornings.android.fileprovider",
                        photoFile)
                //If no EXTRA_OUTPUT defined, then a low res thumbnail bitmap is returned
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
        }

        val pickPhoto = Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        val chooserIntent = Intent.createChooser(pickPhoto, pickTitle)
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePicture))

        startActivityForResult(chooserIntent, 0)
    }

    @OnTextChanged(R.id.settings_profile_name)
    fun onTextChangedProfileName() {
        if(!authManager.isUserSignedIn()) return

        val profileNameText = profileName.text.toString()
        FirebaseNetwork.updateProfileUserName(profileNameText)
    }

    @OnTextChanged(R.id.settings_profile_mobile_number)
    fun onTextChangedProfileMobileNumber() {
        profileMobileNumberText = profileMobileNumber.text.toString()

        if(!authManager.isUserSignedIn()) return

        if (profileMobileNumberText.isBlank()) {
            Toaster.makeToast(this, "Mobile number can't be empty.", Toast.LENGTH_SHORT).checkTastyToast()
        } else if (MyContactsController.containsInvalidCharacters(profileMobileNumberText)) {
            profileMobileNumberText = MyContactsController.clearInvalidCharacters(profileMobileNumberText)
            profileMobileNumber.setText(profileMobileNumberText)
            //Place cursor at end of EditText
            profileMobileNumber.setSelection(profileMobileNumberText.length)
            Toaster.makeToast(this, "Entry not valid.", Toast.LENGTH_SHORT).checkTastyToast()
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, imageReturnedIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent)
        when (requestCode) {
            0 -> if (resultCode == Activity.RESULT_OK) {
                var returnedImageURI: Uri? = null
                returnedImageURI = when {
                    imageReturnedIntent != null -> imageReturnedIntent.data
                    StrUtils.notNullOrEmpty(mCurrentPhotoPath) -> {
                        galleryAddPic()
                        val file = File(mCurrentPhotoPath)
                        Uri.fromFile(file)
                    }
                    else -> {
                        Toaster.makeToast(this, "Load image failed.", Toast.LENGTH_SHORT).checkTastyToast()
                        return
                    }
                }
                if (returnedImageURI != null) {
                    setProfilePicFromURI(returnedImageURI)
                    uploadProfilePicture(returnedImageURI)
                }
            }
            else -> {}
        }
    }

    //Note: If you saved your photo to the directory provided by getExternalFilesDir(), the media scanner cannot access the files because they are private to your app.
    private fun galleryAddPic() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(mCurrentPhotoPath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        this.sendBroadcast(mediaScanIntent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = getDateTimeInstance().format(Date())
        val imageFileName = "Rooster_Profile_Pic_" + timeStamp
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */)

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun uploadProfilePicture(returnedImage: Uri) {
        var byteArray: ByteArray
        try {
            val imageStream = contentResolver.openInputStream(returnedImage)
            var selectedImage = BitmapFactory.decodeStream(imageStream)
            byteArray = ByteArray(selectedImage.byteCount)
            //Resize bitmap
            selectedImage = getResizedBitmap(selectedImage, 250)
            //Convert to byte array
            val stream = ByteArrayOutputStream()
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
            byteArray = stream.toByteArray()
            //Upload to firebase with putBytes
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Toaster.makeToast(this, "Image upload failed.", Toast.LENGTH_SHORT).checkTastyToast()
            return
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toaster.makeToast(this, "Image upload failed.", Toast.LENGTH_SHORT).checkTastyToast()
            return
        }

        val mStorageRef = FirebaseStorage.getInstance().reference
        val photoFileRef = mStorageRef.child(Constants.STORAGE_USER_PROFILE_PICTURE + mCurrentUser?.uid)

        val uploadTask = photoFileRef.putBytes(byteArray)

        uploadTask
                .addOnSuccessListener { taskSnapshot ->
                    // Get a URL to the uploaded content
                    val firebaseStorageURL = taskSnapshot.downloadUrl
                    FirebaseNetwork.updateProfileProfilePic(firebaseStorageURL!!)
                }
                .addOnFailureListener {
                    // Handle unsuccessful upload
                    Toaster.makeToast(applicationContext, "Error uploading.", Toast.LENGTH_LONG).checkTastyToast()
                }
    }

    private fun setProfilePicFromURI(uri: Uri) {

        try {
            Picasso.with(this@ProfileActivity).load(uri)
                    .resize(400, 400)
                    .centerCrop()
                    .into(profilePic, object : Callback {
                        override fun onSuccess() {
                            val imageBitmap = (profilePic.drawable as BitmapDrawable).bitmap
                            val imageDrawable = RoundedBitmapDrawableFactory.create(resources, imageBitmap)
                            imageDrawable.isCircular = true
                            imageDrawable.cornerRadius = Math.max(imageBitmap.width, imageBitmap.height) / 2.0f
                            profilePic.setImageDrawable(imageDrawable)
                        }

                        override fun onError() {

                        }
                    })
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Toaster.makeToast(this, "Load image failed.", Toast.LENGTH_SHORT).checkTastyToast()
        }

    }

    private fun setProfilePicFromURL(url: String) {
        try {
            Picasso.with(this@ProfileActivity).load(url)
                    .resize(400, 400)
                    .centerCrop()
                    .into(profilePic, object : Callback {
                        override fun onSuccess() {
                            val imageBitmap = (profilePic.drawable as BitmapDrawable).bitmap
                            val imageDrawable = RoundedBitmapDrawableFactory.create(resources, imageBitmap)
                            imageDrawable.isCircular = true
                            imageDrawable.cornerRadius = Math.max(imageBitmap.width, imageBitmap.height) / 2.0f
                            profilePic.setImageDrawable(imageDrawable)
                        }

                        override fun onError() {

                        }
                    })
        } catch (e: NullPointerException) {
            e.printStackTrace()
            Toaster.makeToast(this, "Load image failed.", Toast.LENGTH_SHORT).checkTastyToast()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), RequestCode.PERMISSIONS_CHANGE_PROFILE_PIC.ordinal)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            RequestCode.PERMISSIONS_CHANGE_PROFILE_PIC.ordinal -> {
                if (grantResults.isNotEmpty()) {
                    val readPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (!readPermission && !writePermission) {
                        Toaster.makeToast(this, "Permission denied. Please reconsider?", Toast.LENGTH_LONG).checkTastyToast()
                    }
                }
            }
            else -> {}
        }
    }

    private fun checkPermission(): Boolean {
        val result1 = ContextCompat.checkSelfPermission(applicationContext,
                READ_EXTERNAL_STORAGE)
        val result2 = ContextCompat.checkSelfPermission(applicationContext,
                WRITE_EXTERNAL_STORAGE)
        return result1 and result2 == PackageManager.PERMISSION_GRANTED
    }
}
