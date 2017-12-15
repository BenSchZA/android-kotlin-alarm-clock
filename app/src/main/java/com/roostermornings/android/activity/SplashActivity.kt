/*
 * Rooster Mornings Android.
 * Copyright (c)  2017 Roosta Media. All rights reserved.
 */

package com.roostermornings.android.activity

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.firebase.AuthManager
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.domain.MinimumRequirements
import com.roostermornings.android.domain.OnboardingJourneyEvent
import com.roostermornings.android.firebase.UserMetrics
import com.roostermornings.android.onboarding.OnboardingActivity
import com.roostermornings.android.util.Constants
import com.roostermornings.android.util.FileUtils
import com.roostermornings.android.util.InternetHelper
import com.roostermornings.android.util.RoosterUtils
import com.roostermornings.android.util.Toaster

import java.io.File
import java.io.IOException
import java.util.Date

import java.text.DateFormat.getDateTimeInstance
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    private var receivedIntent: Intent? = null
    private var receivedAction: String? = ""
    private var receivedType: String? = ""

    @Inject
    lateinit var authManager: AuthManager

    override fun inject(component: RoosterApplicationComponent) {
        component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BaseApplication.getRoosterApplicationComponent().inject(this)

        // Authenticate client to give access to DB
        authManager.signInAnonymouslyIfNecessary{ uid ->
            UserMetrics.generateNewUserMetricsEntry()
            // Log last seen in user metrics, to enable clearing stagnant data
            UserMetrics.updateLastSeen()
            // Log onboarding journey activityContentView event
            UserMetrics.logOnboardingEvent(
                    OnboardingJourneyEvent(subject = "Splash UI")
                            .setType(OnboardingJourneyEvent.Companion.Event.VIEW))
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        receivedIntent = intent
        receivedAction = intent.action
        receivedType = intent.type

        receivedIntent?.let {

            if (Intent.ACTION_SEND == receivedAction) {
                if (receivedType?.startsWith("audio/") == true) {
                    val receivedUri = (receivedIntent as Intent).getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

                    receivedUri?.let {

                        val shareFilePath = File(cacheDir, "Cache")
                        val timeStamp = getDateTimeInstance().format(Date())
                        val randomUID = RoosterUtils.createRandomUID(5)
                        val shareFileName = "Rooster_Shared_Audio_$randomUID-$timeStamp.3gp"
                        val shareFile = File(shareFilePath, shareFileName)

                        if (shareFile.parentFile.exists() || shareFile.parentFile.mkdirs()) {

                            try {
                                val inputStream = contentResolver.openInputStream(receivedUri)
                                //Copy file contents to new public file
                                FileUtils.copyFromStream(inputStream, shareFile)

                                //If file is valid, open a share dialog
                                if (shareFile.isFile && validMimeType(shareFile) && shareFile.length() < Constants.MAX_ROOSTER_FILE_SIZE) {
                                    //Uri shareFileUri = FileProvider.getUriForFile(this, "com.roostermornings.android.fileprovider", shareFile);
                                    //Send audio file to friends selection activity
                                    val intent = Intent(this@SplashActivity, NewAudioFriendsActivity::class.java)
                                    val bun = Bundle()
                                    bun.putString(Constants.EXTRA_LOCAL_FILE_STRING, shareFile.path)
                                    intent.putExtras(bun)
                                    startActivity(intent)
                                    return
                                } else {
                                    Toaster.makeToast(this, "File is greater than 8 MB, or is corrupt.", Toast.LENGTH_LONG)
                                    finish()
                                    return
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                                Toaster.makeToast(this, "Error loading file.", Toast.LENGTH_LONG)
                                finish()
                                return
                            }

                        }
                    }
                }
            } else if (Intent.ACTION_MAIN == receivedAction) {

            }
            startMain()
        }
    }

    private fun getAudioDuration(audioFile: File?): Long {
        var duration: Long = 0

        if (audioFile != null) {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(audioFile.path)
            val durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durationStr != null) {
                duration = java.lang.Long.parseLong(durationStr)
            }
        }
        return duration
    }

    private fun validMimeType(audioFile: File?): Boolean {
        if (audioFile != null) {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(audioFile.path)
            val mimeTypeStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (mimeTypeStr != null && (mimeTypeStr.contains("audio/mpeg")
                    || mimeTypeStr.contains("audio/mp4")
                    || mimeTypeStr.contains("audio/3gpp"))) {
                return true
            } else if (mimeTypeStr != null) {
                Toaster.makeToast(this, "Invalid audio mime-type " + mimeTypeStr, Toast.LENGTH_SHORT)
            }
        }
        return false
    }

    private fun startMain() {
        if (!InternetHelper.noInternetConnection(this)) {
            checkMinimumRequirements()
        } else {
            chooseActivity(true, null)
        }
    }

    private fun navigateToActivity(activityClass: Class<out Activity>) {
        val i = Intent(this@SplashActivity, activityClass)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        startActivity(i)
        finish()
    }

    private fun checkMinimumRequirements() {
        val minReqRef = FirebaseDatabase.getInstance().reference
                .child("minimum_requirements_android")
        minReqRef.keepSynced(true)

        val minReqListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var aboveMinReq = true
                val minimumRequirements = dataSnapshot.getValue(MinimumRequirements::class.java)
                if (minimumRequirements == null) {
                    chooseActivity(true, null)
                    return
                }
                if (minimumRequirements.isInvalidate_user) {
                    aboveMinReq = RoosterUtils.isAboveVersion(minimumRequirements)
                }
                chooseActivity(aboveMinReq, minimumRequirements)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                chooseActivity(true, null)
            }
        }
        minReqRef.addListenerForSingleValueEvent(minReqListener)
    }

    private fun chooseActivity(aboveMinimumRequirements: Boolean, minimumRequirements: MinimumRequirements?) {
        if (!aboveMinimumRequirements) {
            val i = Intent(this@SplashActivity, InvalidateVersion::class.java)
            if (minimumRequirements != null) {
                i.putExtra(Constants.FORCE_UPDATE_TITLE, minimumRequirements.getUpdate_title())
                i.putExtra(Constants.FORCE_UPDATE_DESCRIPTION, minimumRequirements.getUpdate_description())
            }
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            startActivity(i)
            finish()
        } else {
            // If the user is signed-in, skip onboarding and open app
            if (authManager.isUserSignedIn()) {
                navigateToActivity(MyAlarmsFragmentActivity::class.java)
            } else {
                navigateToActivity(OnboardingActivity::class.java)
            }
        }
    }
}
