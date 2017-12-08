package com.roostermornings.android.firebase

import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.util.Constants
import javax.inject.Inject

/**
 * Created by bscholtz on 2017/12/05.
 */
class AuthManager {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    fun isUserSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
                && firebaseAuth.currentUser?.isAnonymous == false
    }

    fun signOut() {
        firebaseAuth.signOut()
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
}