package com.nunchuk.android.signer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.signer.databinding.ActivityAddNfcKeyBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AddNFCKeyActivity : BaseActivity<ActivityAddNfcKeyBinding>() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    @Inject
    lateinit var nativeSdk: NunchukNativeSdk
    override fun initializeBinding() = ActivityAddNfcKeyBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0)
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = intent?.getByteArrayExtra(NfcAdapter.EXTRA_ID)
        val tag: Tag? = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag?

        val card = IsoDep.get(tag)
        try {
            card.timeout = 2500
            card.connect()
            if (card.isConnected) {
                val status = nativeSdk.tapSignerStatus(card)
                binding.desc.isVisible = false
                binding.ivNfc.isVisible = false
                binding.tvKeyInfo.isVisible = true
                binding.tvKeyInfo.text = status
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupViews() {

    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AddNFCKeyActivity::class.java))
        }
    }
}