package com.roostermornings.android.activity

import android.content.Context
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import com.roostermornings.android.R
import com.roostermornings.android.activity.base.BaseActivity
import com.roostermornings.android.dagger.RoosterApplicationComponent
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_faqs.*
import android.webkit.JavascriptInterface
import com.crashlytics.android.Crashlytics
import com.roostermornings.android.BaseApplication
import com.roostermornings.android.keys.PrefsKey


/**
 * Created by bscholtz on 2018/01/29.
 */
class FAQActivity : BaseActivity() {

    override fun inject(component: RoosterApplicationComponent) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(R.layout.activity_faqs)
        BaseApplication.roosterApplicationComponent.inject(this)

        // Possible to have a JavaScriptInterface to enable comms
        faq_webview.addJavascriptInterface(WebViewJavaScriptInterface(this), "app")
        faq_webview.settings.javaScriptEnabled = true
        faq_webview.settings.cacheMode = WebSettings.LOAD_DEFAULT
        faq_webview.webChromeClient = WebChromeClient()
        
        faq_webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webview_progressbar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                webview_progressbar.visibility = View.GONE
                // Show that user has viewed FAQs page
                sharedPreferences.edit().putBoolean(PrefsKey.USER_VIEWED_FAQS.name, true).apply()
            }
        }

        faq_webview.loadUrl("http://faqs-android.s3-website-us-east-1.amazonaws.com/")
    }

    /*
     * JavaScript Interface. Web code can access methods in here
     * (as long as they have the @JavascriptInterface annotation)
     */
    inner class WebViewJavaScriptInterface/*
         * Need a reference to the context in order to sent a post message
         */
    (private val context: Context) {

        /*
         * This method can be called from Android. @JavascriptInterface
         * required after SDK version 17.
         */
        @JavascriptInterface
        fun contactUs() {
            lifeCycle.sendFeedback(mCurrentUser?.user_name ?: "Anonymous")
        }

        @JavascriptInterface
        fun useful() {
            Crashlytics.log(1, "Useful: "," FAQs were voted useful.")
            Crashlytics.logException(Throwable("FAQs Page Rating"))
        }

        @JavascriptInterface
        fun notUseful() {
            Crashlytics.log(1, "Not Useful: "," FAQs were voted not useful.")
            Crashlytics.logException(Throwable("FAQs Page Rating"))
        }
    }

    override fun onBackPressed() {
        if (faq_webview.canGoBack()) {
            faq_webview.goBack()
        } else {
            super.onBackPressed()
        }
    }
}