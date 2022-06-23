package com.nunchuk.android.signer

import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.utils.PendingIntentUtils
import com.nunchuk.android.widget.NCInfoDialog

abstract class BaseNfcActivity<Binding : ViewBinding> : BaseActivity<Binding>() {
    private val nfcViewModel : NfcViewModel by viewModels()
    private var requestCode: Int = 0

    private val nfcAdapter: NfcAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        NfcAdapter.getDefaultAdapter(this)
    }

    private val nfcUnsupportedDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        NCInfoDialog(this).init(
            title = getString(R.string.nc_no_nfc_title),
            message = getString(R.string.no_nfc_message)
        )
    }

    private val requestEnableNfc =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (nfcAdapter?.isEnabled == true) {
                askToScan(requestCode)
            }
        }

    private val askScanNfcDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        NCInfoDialog(this).init(
            title = "Ready to Scan",
            message = "Hold your device near the NFC key."
        )
    }

    override fun onResume() {
        super.onResume()
        if (askScanNfcDialog.isShowing) {
            nfcAdapter?.enableForegroundDispatch(this, getNfcPendingIntent(requestCode), null, null)
        }
    }

    override fun onPause() {
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    protected fun startNfcFlow(requestCode: Int) {
        this.requestCode = requestCode
        nfcAdapter?.let {
            if (it.isEnabled) {
                askToScan(requestCode)
            } else {
                navigateTurnOnNfc()
            }
        } ?: run {
            nfcUnsupportedDialog.show()
        }
    }

    private fun askToScan(requestCode: Int) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            nfcAdapter?.enableForegroundDispatch(this, getNfcPendingIntent(requestCode), null, null)
        }
        askScanNfcDialog.show()
    }

    private fun navigateTurnOnNfc() {
        requestEnableNfc.launch(Intent(this, TurnOnNfcActivity::class.java))
    }

    private fun getNfcPendingIntent(requestCode: Int) = PendingIntent.getActivity(
        this,
        0,
        Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).apply {
            putExtra(EXTRA_REQUEST_NFC_CODE, requestCode)
        },
        PendingIntentUtils.getFlagCompat()
    )

    private fun processNfcIntent(intent: Intent) {
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag ?: return
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_NFC_CODE, 0)
        if (requestCode == 0) return
        nfcViewModel.updateNfcScanInfo(requestCode, tag)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processNfcIntent(intent ?: return)
    }

    companion object {
        private const val EXTRA_REQUEST_NFC_CODE = "EXTRA_REQUEST_NFC_CODE"
        const val REQUEST_NFC_STATUS = 1
        const val REQUEST_NFC_CHANGE_CVC = 2
        const val REQUEST_NFC_ADD_KEY = 3
    }
}