package com.nunchuk.android.signer.trezor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.core.util.parseTrezorCallback
import com.nunchuk.android.core.util.TrezorCallbackHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrezorCallbackActivity : AppCompatActivity() {

    @Inject
    lateinit var trezorCallbackHolder: TrezorCallbackHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleCallback(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallback(intent)
        finish()
    }

    private fun handleCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (parseTrezorCallback(uri) != null) {
            trezorCallbackHolder.set(uri)
        }
    }
}
