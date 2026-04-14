package com.nunchuk.android.core.verification

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nunchuk.android.core.network.VerificationEventBus
import org.json.JSONObject
import timber.log.Timber

class VerificationActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var tokenReceived = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.userAgentString = settings.userAgentString

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    return !isAllowedUrl(url)
                }
            }
            addJavascriptInterface(VerificationBridge(), JS_INTERFACE_NAME)
        }
        enableEdgeToEdge()
        setContentView(webView)
        ViewCompat.setOnApplyWindowInsetsListener(webView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val theme = if (isDark) "dark" else "light"
        webView.loadUrl("$VERIFICATION_URL?theme=$theme")
    }

    override fun onDestroy() {
        if (!tokenReceived) {
            VerificationEventBus.instance().onVerificationCancelled()
        }
        webView.removeJavascriptInterface(JS_INTERFACE_NAME)
        webView.destroy()
        super.onDestroy()
    }

    private fun isAllowedUrl(url: String): Boolean {
        return url.startsWith(VERIFICATION_URL) ||
                url.startsWith("https://challenges.cloudflare.com") ||
                url.startsWith("https://hcaptcha.com") ||
                url.contains(".hcaptcha.com") ||
                url.startsWith("about:blank") ||
                url.startsWith("about:srcdoc")
    }

    inner class VerificationBridge {
        @JavascriptInterface
        fun onMessage(json: String) {
            runOnUiThread {
                try {
                    val message = JSONObject(json)
                    if (message.optString("event") == "token") {
                        tokenReceived = true
                        VerificationEventBus.instance()
                            .onTokenReceived(message.optString("payload"))
                        finish()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing verification message")
                }
            }
        }
    }

    companion object {
        private const val VERIFICATION_URL = "https://verification.nunchuk.io/"
        private const val JS_INTERFACE_NAME = "NunchukVerification"

        fun start(context: Context) {
            context.startActivity(Intent(context, VerificationActivity::class.java))
        }
    }
}
