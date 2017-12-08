package com.roostermornings.android.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.OnClick
import com.facebook.*
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.ImageRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.iid.FirebaseInstanceId
import com.mobsandgeeks.saripaar.Validator
import com.mobsandgeeks.saripaar.annotation.NotEmpty
import com.mobsandgeeks.saripaar.annotation.Password
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.R
import com.roostermornings.android.auth.AuthManager
import com.roostermornings.android.dagger.RoosterApplicationComponent
import com.roostermornings.android.firebase.FirebaseNetwork
import com.roostermornings.android.firebase.FirebaseNetwork.createOrUpdateRoosterUser
import com.roostermornings.android.fragment.base.BaseFragment
import com.roostermornings.android.util.Toaster
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_onboarding_profile_creation.*
import kotlinx.android.synthetic.main.fragment_onboarding_profile_creation.view.*
import java.util.*
import javax.inject.Inject

/**
 * Created by bscholtz on 2017/12/04.
 */
class ProfileCreationFragment : BaseFragment(), FragmentInterface, Validator.ValidationListener {
    private var mHostInterface: HostInterface? = null

    private val RC_SIGN_IN = 7

    private var validator: Validator = Validator(this)
    private var mAlreadyUser = false

    private var facebookCallbackManager: CallbackManager? = null
    private var mGoogleApiClient: GoogleApiClient? = null

    @BindView(R.id.already_user_textview)
    lateinit var mTextViewAlreadyUser: TextView

    @BindView(R.id.sign_up_email_textview)
    lateinit var mTextViewSignUpEmail: TextView

    @BindView(R.id.login_button)
    lateinit var facebookLoginButton: LoginButton

    @BindView(R.id.progressBar)
    lateinit var progressBar: ProgressBar

    @NotEmpty
    @BindView(R.id.signup_username_edittext)
    lateinit var mUserName: EditText

    @BindView(R.id.signup_email_address_edittext)
    lateinit var mEmailAddress: EditText

    @Password(min = 6, scheme = Password.Scheme.ALPHA_NUMERIC)
    @BindView(R.id.signup_password_edittext)
    lateinit var mPassword: EditText

    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var authManager: AuthManager

    companion object {

        val TAG = ProfileCreationFragment::class.java.simpleName?:"nullSimpleName"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(): ProfileCreationFragment {
            val fragment = ProfileCreationFragment()
//            val args = Bundle()
//            args.putParcelable(ARG_WINDOW_SIZE, windowSize)
//            fragment.arguments = args
            return fragment
        }
    }

    init {
        BaseApplication.getRoosterApplicationComponent().inject(this)
    }

    override fun inject(component: RoosterApplicationComponent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mHostInterface = context as HostInterface
        } catch (e: ClassCastException) {
            TODO("HostInterface not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    override fun onPause() {
        super.onPause()
        mGoogleApiClient?.stopAutoManage(activity)
        mGoogleApiClient?.disconnect()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return initiate(inflater, R.layout.fragment_onboarding_profile_creation, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(authManager.isUserSignedIn()) {
            view?.signUpLayout?.visibility = View.INVISIBLE
            view?.emailLayout?.visibility = View.INVISIBLE
            signedInLayout.visibility = View.VISIBLE
        }

        initializeAuthProviders()

        //instantiate saripaar validator to validate fields with NotEmpty annotations
        validator.setValidationListener(this)

        view?.let {
            val animationCloud = TranslateAnimation(0f, 0f, -10f, 10f)
            animationCloud.duration = 2000
            animationCloud.fillAfter = true
            animationCloud.repeatCount = -1
            animationCloud.repeatMode = Animation.REVERSE

            view.roosterCloud?.animation = animationCloud
        }
    }

    override fun fragmentVisible(position: Int) {}

    override fun customCommand(command: InterfaceCommands.Companion.Command) {
        when(command) {
            InterfaceCommands.Companion.Command.SCROLL_RIGHT -> {
                mHostInterface?.scrollViewPager(View.FOCUS_RIGHT)
            }
            else -> {}
        }
    }

    private fun initializeAuthProviders() {
        //Facebook
        facebookLoginButton.setReadPermissions("email", "public_profile")
        facebookCallbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(
                facebookCallbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        Log.d(TAG, "facebook:onSuccess:" + loginResult)
                        firebaseAuthWithFacebook(loginResult.accessToken)
                    }

                    override fun onCancel() {
                        Log.e("Facebook: ", "onCancel")
                    }
                    override fun onError(exception: FacebookException) {
                        Log.e("Facebook: ", "onError")
                    }
                }
        )

        //Google
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = GoogleApiClient.Builder(context)
                .enableAutoManage(this.activity, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    @OnClick(R.id.already_user_textview)
    fun onAlreadyUserClicked() {
        mAlreadyUser = true
        mUserName.visibility = View.INVISIBLE
        view?.signUpLayout?.visibility = View.INVISIBLE
        view?.emailLayout?.visibility = View.VISIBLE
        signedInLayout.visibility = View.INVISIBLE
    }

    @OnClick(R.id.sign_up_email_textview)
    fun onSignUpEmailClicked() {
        mAlreadyUser = false
        mUserName.visibility = View.VISIBLE
        view?.signUpLayout?.visibility = View.INVISIBLE
        view?.emailLayout?.visibility = View.VISIBLE
        signedInLayout.visibility = View.INVISIBLE
    }

    @OnClick(R.id.cancel_button)
    fun onClickCancel() {
        view?.signUpLayout?.visibility = View.VISIBLE
        view?.emailLayout?.visibility = View.INVISIBLE
        signedInLayout.visibility = View.INVISIBLE
    }

    @OnClick(R.id.sign_out_button)
    fun onClickSignOut() {
        signUpLayout.visibility = View.VISIBLE
        emailLayout.visibility = View.INVISIBLE
        signedInLayout.visibility = View.INVISIBLE
        authManager.signOut()
    }

    @OnClick(R.id.terms_and_conditions_link)
    fun onTermsAndConditionsClicked() {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(getString(R.string.rooster_website_t_and_c_url))
        startActivity(i)
    }

    @OnClick(R.id.signin_button_facebook)
    fun onFacebookSigninButtonClick(v: View) {
        LoginManager.getInstance().logInWithReadPermissions(
                this,
                Arrays.asList("public_profile", "email"))
    }

    @OnClick(R.id.signin_button_google)
    fun onGoogleSigninButtonClick(v: View) {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @OnClick(R.id.signin_button_notnow)
    fun onNotNowSigninButtonClick() {
        mHostInterface?.scrollViewPager(View.FOCUS_RIGHT)
    }

    //Receive auth activity result and start callback
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!checkInternetConnection()) return

        //Result returned from launching Facebook authentication
        if (requestCode == CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode()) {
            progressBar.visibility = View.VISIBLE
            facebookCallbackManager?.onActivityResult(requestCode, resultCode, data)
        }

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            progressBar.visibility = View.VISIBLE
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleGoogleSignInResult(result)
        }
    }

    //Facebook
    private fun firebaseAuthWithFacebook(token: AccessToken) {
        if (!checkInternetConnection()) return

        Log.d(TAG, "firebaseAuthWithFacebook:" + token)

        val credential = FacebookAuthProvider.getCredential(token.token)

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)

            // If sign in fails, display a message to the user. If sign in succeeds
            // the auth state listener will be notified and logic to handle the
            // signed in user can be handled in the listener.
            if (task.isSuccessful) {
                view?.signUpLayout?.visibility = View.INVISIBLE
                view?.emailLayout?.visibility = View.INVISIBLE
                signedInLayout.visibility = View.VISIBLE

                FirebaseNetwork.migrateUserMetrics(authManager.getPersistedAnonymousUID(), firebaseAuth.currentUser?.uid)

                val deviceToken = FirebaseInstanceId.getInstance().token

                val photoURL = try {
                    //photoURLString = firebaseAuth.getCurrentUser().getPhotoUrl().toString();
                    val dimensionPixelSize = resources.getDimensionPixelSize(com.facebook.R.dimen.com_facebook_profilepictureview_preset_size_normal)
                    val profile = Profile.getCurrentProfile()
                    ImageRequest.getProfilePictureUri(profile.id, dimensionPixelSize, dimensionPixelSize).toString()
                } catch (e: java.lang.NullPointerException) {
                    e.printStackTrace()
                    null
                }

                createOrUpdateRoosterUser(deviceToken, photoURL, false)
                proceedToNextPage()

                Toaster.makeToast(context,
                        "Facebook sign-in successful.",
                        Toast.LENGTH_LONG)

            } else {
                //Remove progress bar on failure
                progressBar.visibility = View.GONE

                Log.w(TAG, "Facebook: signInWithCredential", task.exception)
                Toaster.makeToast(context,
                        "Facebook sign-in failed.",
                        Toast.LENGTH_LONG)
            }
        }
    }

    //Google
    private fun handleGoogleSignInResult(result: GoogleSignInResult) {
        Log.d(TAG, "handleGoogleSignInResult:" + result.isSuccess)
        if (result.isSuccess) {
            // Google Sign In was successful, authenticate with Firebase
            firebaseAuthWithGoogle(result)
        } else {
            //Remove progress bar on failure
            progressBar.visibility = View.GONE

            // Signed out, show unauthenticated UI.
            Toaster.makeToast(context,
                    "Google sign-in failed.",
                    Toast.LENGTH_LONG)
        }
    }

    private fun firebaseAuthWithGoogle(result: GoogleSignInResult) {
        if (!checkInternetConnection()) return

        val account = result.signInAccount
        Log.d(TAG, "firebaseAuthWithGoogle:" + account?.id)

        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)

            // If sign in fails, display a message to the user. If sign in succeeds
            // the auth state listener will be notified and logic to handle the
            // signed in user can be handled in the listener.
            if (task.isSuccessful) {
                view?.signUpLayout?.visibility = View.INVISIBLE
                view?.emailLayout?.visibility = View.INVISIBLE
                signedInLayout.visibility = View.VISIBLE

                FirebaseNetwork.migrateUserMetrics(authManager.getPersistedAnonymousUID(), firebaseAuth.currentUser?.uid)

                val deviceToken = FirebaseInstanceId.getInstance().token
                val photoURL = account?.photoUrl?.toString()

                createOrUpdateRoosterUser(deviceToken, photoURL, false)
                proceedToNextPage()

                Toaster.makeToast(context,
                        "Google sign-in successful.",
                        Toast.LENGTH_LONG)
            } else {
                //Remove progress bar on failure
                progressBar.visibility = View.GONE

                Log.w(TAG, "Google: signInWithCredential", task.exception)
                Toaster.makeToast(context,
                        "Google sign-in failed.",
                        Toast.LENGTH_LONG)
            }
        }
    }

    //On successful authentication, proceed to alarms activity
    private fun proceedToNextPage() {
        //Remove progress bar on success
        progressBar.visibility = View.GONE

        mHostInterface?.scrollViewPager(View.FOCUS_RIGHT)
    }

    @OnClick(R.id.signup_button_email)
    fun onSignupButtonClicked() {
        validator.validate()
    }

    override fun onValidationSucceeded() {
        super.onValidationSucceeded()

        if (!checkInternetConnection()) return

        val email = mEmailAddress.text.toString().trim({ it <= ' ' })
        val password = mPassword.text.toString().trim({ it <= ' ' })
        val name = mUserName.text.toString().trim({ it <= ' ' })

        if (mAlreadyUser) {

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener({ task ->
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful)

                        if (!task.isSuccessful) {
                            Toast.makeText(context, R.string.signup_auth_failed,
                                    Toast.LENGTH_LONG).show()
                        } else {
                            view?.signUpLayout?.visibility = View.INVISIBLE
                            view?.emailLayout?.visibility = View.INVISIBLE
                            signedInLayout.visibility = View.VISIBLE

                            FirebaseNetwork.migrateUserMetrics(authManager.getPersistedAnonymousUID(), firebaseAuth.currentUser?.uid)

                            val deviceToken = FirebaseInstanceId.getInstance().token

                            firebaseAuth.currentUser?.updateProfile(
                                    UserProfileChangeRequest.Builder().setDisplayName(name).build())

                            FirebaseNetwork.createOrUpdateRoosterUser(deviceToken, "", true)

                            Toaster.makeToast(context,
                                    "Email sign-in successful.",
                                    Toast.LENGTH_LONG)

                            proceedToNextPage()
                        }
                    })


        } else {

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener({ task ->
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful)

                        if (!task.isSuccessful) {
                            Toaster.makeToast(context, task.exception?.message?:"",
                                    Toast.LENGTH_LONG)
                        } else {
                            view?.signUpLayout?.visibility = View.INVISIBLE
                            view?.emailLayout?.visibility = View.INVISIBLE
                            signedInLayout.visibility = View.VISIBLE

                            FirebaseNetwork.migrateUserMetrics(authManager.getPersistedAnonymousUID(), firebaseAuth.currentUser?.uid)

                            val deviceToken = FirebaseInstanceId.getInstance().token

                            FirebaseNetwork.createOrUpdateRoosterUser(deviceToken, "", false)

                            Toaster.makeToast(context,
                                    "Email sign-in successful.",
                                    Toast.LENGTH_LONG)

                            proceedToNextPage()
                        }
                    })
        }
    }
}