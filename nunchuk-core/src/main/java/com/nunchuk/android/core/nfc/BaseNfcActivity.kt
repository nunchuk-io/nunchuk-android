package com.nunchuk.android.core.nfc

import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.utils.PendingIntentUtils
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog

abstract class BaseNfcActivity<Binding : ViewBinding> : BaseActivity<Binding>() {
    protected val nfcViewModel: NfcViewModel by viewModels()
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

    private val inputCvcDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        NCInputDialog(this)
            .showDialog(
                title = "Enter CVC",
                onConfirmed = {
                    nfcViewModel.updateInputCvc(it)
                    askToScan(requestCode)
                },
                isMaskedInput = true
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
        ).apply {
            setOnShowListener {
                nfcAdapter?.enableForegroundDispatch(
                    this@BaseNfcActivity,
                    getNfcPendingIntent(requestCode),
                    null,
                    null
                )
            }

            setOnDismissListener {
                nfcAdapter?.disableForegroundDispatch(this@BaseNfcActivity)
            }
        }
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

    fun startNfcFlow(requestCode: Int) {
        this.requestCode = requestCode
        nfcAdapter?.let {
            if (it.isEnabled) {
                if (shouldShowInputCvcFirst(requestCode)) {
                    showInputCvcDialog()
                } else {
                    askToScan(requestCode)
                }
            } else {
                navigateTurnOnNfc()
            }
        } ?: run {
            nfcUnsupportedDialog.show()
        }
    }

    private fun shouldShowInputCvcFirst(requestCode: Int) =
        requestCode != REQUEST_NFC_STATUS && requestCode != REQUEST_NFC_CHANGE_CVC

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
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            askScanNfcDialog.dismiss()
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as? Tag ?: return
            val requestCode = intent.getIntExtra(EXTRA_REQUEST_NFC_CODE, 0)
            if (requestCode == 0) return
            nfcViewModel.updateNfcScanInfo(requestCode, tag)
        }
    }

    private fun showInputCvcDialog() {
        inputCvcDialog.show()
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
        const val REQUEST_NFC_SIGN_TRANSACTION = 4
        const val REQUEST_NFC_VIEW_BACKUP_KEY = 5
        const val REQUEST_NFC_TOPUP_XPUBS = 6
    }
}