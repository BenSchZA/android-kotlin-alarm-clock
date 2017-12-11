package com.roostermornings.android.firebase

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.facebook.AccessToken
import com.facebook.Profile
import com.facebook.internal.ImageRequest
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.iid.FirebaseInstanceId
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.firebase.FirebaseNetwork.createOrUpdateRoosterUser
import com.roostermornings.android.onboarding.OnboardingActivity
import com.roostermornings.android.onboarding.ProfileCreationFragment
import com.roostermornings.android.util.ConnectivityUtils
import com.roostermornings.android.util.Constants
import javax.inject.Inject

/**
 * Created by bscholtz on 2017/12/05.
 */
class AuthManager(val context: Context) {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    @Inject
    lateinit var conUtils: ConnectivityUtils

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    companion object {
        val TAG = AuthManager::class.java.simpleName?:"nullSimpleName"

        interface AuthInterface {
            fun onAuthSuccess(task: Task<AuthResult>)
            fun onAuthFailure()
        }
    }

    private interface AnonymousLinkingListener {
        fun onLinkSuccess(task: Task<AuthResult>)
        fun onLinkFailure(exception: Exception)
    }

    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
                && firebaseAuth.currentUser?.isAnonymous == false
    }

    fun signOut() {
        firebaseAuth.signOut()
        signInAnonymouslyIfNecessary()
    }

    fun signInAnonymouslyIfNecessary() {
        // If current user is anonymous, or no current user, then login anonymously
        if(firebaseAuth.currentUser?.isAnonymous == true || firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously().addOnCompleteListener {
                clearPersistedAnonymousUID()
                persistAnonymousUID(firebaseAuth.currentUser)
            }.addOnFailureListener {
                Log.e(it.cause.toString(),it.message)
            }
        }
    }

    fun getPersistedAnonymousUID(): String? {
        return sharedPrefs.getString(Constants.ANONYMOUS_USER_UID, null)
    }

    private fun persistAnonymousUID(firebaseUser: FirebaseUser?) {
        if(firebaseUser?.isAnonymous == true) {
            sharedPrefs.edit()
                    .putString(Constants.ANONYMOUS_USER_UID, firebaseUser.uid)
                    .apply()
        }
    }

    private fun clearPersistedAnonymousUID() {
        sharedPrefs.edit()
                .remove(Constants.ANONYMOUS_USER_UID)
                .apply()
    }

    //Facebook
    fun firebaseAuthWithFacebook(token: AccessToken, listener: AuthInterface) {
        if (!conUtils.isConnected()) return

        Log.d(TAG, "firebaseAuthWithFacebook:" + token)

        val credential = FacebookAuthProvider.getCredential(token.token)

        attemptAnonymousLinking(credential, object: AnonymousLinkingListener {
            override fun onLinkSuccess(task: Task<AuthResult>) {
                onSuccessfulFacebookAuth()
                listener.onAuthSuccess(task)
            }

            override fun onLinkFailure(exception: Exception) {
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                    Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (task.isSuccessful) {
                        onSuccessfulFacebookAuth()
                        listener.onAuthSuccess(task)
                    } else {
                        Log.d(TAG, "firebaseAuthWithFacebook:" + exception)
                        listener.onAuthFailure()
                    }
                }
            }
        })
    }

    private fun onSuccessfulFacebookAuth() {
        val deviceToken = FirebaseInstanceId.getInstance().token

        val photoURL = try {
            //photoURLString = firebaseAuth.getCurrentUser().getPhotoUrl().toString();
            val dimensionPixelSize = context.resources.getDimensionPixelSize(com.facebook.R.dimen.com_facebook_profilepictureview_preset_size_normal)
            val profile = Profile.getCurrentProfile()
            ImageRequest.getProfilePictureUri(profile.id, dimensionPixelSize, dimensionPixelSize).toString()
        } catch (e: java.lang.NullPointerException) {
            e.printStackTrace()
            null
        }

        createOrUpdateRoosterUser(deviceToken, photoURL, false)
    }

    fun firebaseAuthWithGoogle(result: GoogleSignInResult, listener: AuthInterface) {
        if (!conUtils.isConnected()) return

        val account = result.signInAccount
        Log.d(TAG, "firebaseAuthWithGoogle:" + account?.id)

        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)

        attemptAnonymousLinking(credential, object: AnonymousLinkingListener {
            override fun onLinkSuccess(task: Task<AuthResult>) {
                onSuccessfulGoogleAuth(account)
                listener.onAuthSuccess(task)
            }

            override fun onLinkFailure(exception: Exception) {
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                    Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)

                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (task.isSuccessful) {
                        onSuccessfulGoogleAuth(account)
                        listener.onAuthSuccess(task)
                    } else {
                        Log.d(TAG, "firebaseAuthWithGoogle:" + exception)
                        listener.onAuthFailure()
                    }
                }
            }
        })
    }

    fun performMigration(activity: Activity) {
        if(activity is OnboardingActivity) {
            FirebaseNetwork.migrateOnboardingJourney(getPersistedAnonymousUID(), firebaseAuth.currentUser?.uid)
        } else {
            FirebaseNetwork.migrateUserUID(getPersistedAnonymousUID(), firebaseAuth.currentUser?.uid)
        }
    }

    private fun onSuccessfulGoogleAuth(account: GoogleSignInAccount?) {
        val deviceToken = FirebaseInstanceId.getInstance().token
        val photoURL = account?.photoUrl?.toString()

        createOrUpdateRoosterUser(deviceToken, photoURL, false)
    }

    private fun attemptAnonymousLinking(credential: AuthCredential, resultListener: AnonymousLinkingListener) {
        firebaseAuth.currentUser
                ?.linkWithCredential(credential)
                ?.addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        resultListener.onLinkSuccess(task)
                    }
        }?.addOnFailureListener {exception ->
            resultListener.onLinkFailure(exception)
        }
    }
}