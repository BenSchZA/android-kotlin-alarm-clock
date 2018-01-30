package com.roostermornings.android.activity

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

        // Possible to have a JavaScriptInterface to enable comms
        faq_webview.settings.javaScriptEnabled = true
        faq_webview.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        faq_webview.webChromeClient = WebChromeClient()
        
        faq_webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                webview_progressbar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                webview_progressbar.visibility = View.GONE
            }
        }

        faq_webview.loadUrl("http://faqs-android.s3-website-us-east-1.amazonaws.com/")
    }

    override fun onBackPressed() {
        if (faq_webview.canGoBack()) {
            faq_webview.goBack()
        } else {
            super.onBackPressed()
        }
    }
}