package com.nunchuk.android.core.nfc

import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.isValidCvc
import com.nunchuk.android.utils.PendingIntentUtils
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NUMBER_TYPE
import timber.log.Timber

abstract class BaseNfcActivity<Binding : ViewBinding> : BaseActivity<Binding>(), NfcActionListener {
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

    private val requestEnableNfc =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (nfcAdapter?.isEnabled == true) {
                askToScan()
            }
        }

    private val askScanNfcDialog: Dialog by lazy(LazyThreadSafetyMode.NONE) {
        NCInfoDialog(this).init(
            title = getString(R.string.nc_ready_to_scan),
            message = getString(R.string.nc_hold_your_device_near_the_nfc),
            btnYes = getString(R.string.nc_text_cancel),
            cancelable = false
        ).apply {
            setOnShowListener {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    nfcAdapter?.enableForegroundDispatch(
                        this@BaseNfcActivity,
                        getNfcPendingIntent(requestCode),
                        null,
                        null
                    )
                }
            }

            setOnDismissListener {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    nfcAdapter?.enableForegroundDispatch(this@BaseNfcActivity, getNfcPendingIntent(0), null, null)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            requestCode = savedInstanceState.getInt(EXTRA_REQUEST_NFC_CODE, 0)
        }
        observer()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putInt(EXTRA_REQUEST_NFC_CODE, requestCode)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    private fun observer() {
        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                nfcViewModel.event.collect {
                    when (it) {
                        NfcState.WrongCvc -> handleWrongCvc()
                        NfcState.LimitCvcInput -> handleLimitCvcInput()
                    }
                }
            }
        }
    }

    private fun handleLimitCvcInput() {
        if (shouldShowInputCvcFirst(requestCode)) {
            showInputCvcDialog(
                errorMessage = getString(R.string.nc_incorrect_cvc_please_try_again),
                descMessage = getString(R.string.nc_cvc_incorrect_3_times)
            )
            nfcViewModel.clearEvent()
        }
    }

    private fun handleWrongCvc() {
        if (shouldShowInputCvcFirst(requestCode)) {
            showInputCvcDialog(errorMessage = getString(R.string.nc_incorrect_cvc_please_try_again))
            nfcViewModel.clearEvent()
        }
    }

    override fun onResume() {
        super.onResume()
        val requestCode = if (askScanNfcDialog.isShowing) requestCode else 0
        nfcAdapter?.enableForegroundDispatch(this, getNfcPendingIntent(requestCode), null, null)
    }

    override fun onPause() {
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun startNfcFlow(requestCode: Int) {
        this.requestCode = requestCode
        nfcAdapter?.let {
            if (it.isEnabled) {
                if (shouldShowInputCvcFirst(requestCode)) {
                    showInputCvcDialog()
                } else {
                    askToScan()
                }
            } else {
                navigateTurnOnNfc()
            }
        } ?: run {
            nfcUnsupportedDialog.show()
        }
    }

    private fun shouldShowInputCvcFirst(requestCode: Int) = requestCode != REQUEST_NFC_STATUS
                && requestCode != REQUEST_NFC_CHANGE_CVC
                && requestCode != REQUEST_AUTO_CARD_STATUS

    private fun askToScan() {
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
            Timber.d("requestCode: $requestCode")
            if (requestCode == 0) return
            nfcViewModel.updateNfcScanInfo(requestCode, tag)
        }
    }

    private fun showInputCvcDialog(errorMessage: String? = null, descMessage: String? = null) {
        NCInputDialog(this)
            .showDialog(
                title = "Enter CVC",
                onConfirmed = { cvc ->
                    if (cvc.isValidCvc()) {
                        nfcViewModel.updateInputCvc(cvc)
                        askToScan()
                    } else {
                        showInputCvcDialog(errorMessage = getString(R.string.nc_required_minimum_6_characters))
                    }
                },
                isMaskedInput = true,
                errorMessage = errorMessage,
                descMessage = descMessage,
                inputType = NUMBER_TYPE
            ).show()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processNfcIntent(intent ?: return)
    }

    companion object {
        private const val EXTRA_REQUEST_NFC_CODE = "EXTRA_REQUEST_NFC_CODE"
        // NFC
        const val REQUEST_NFC_STATUS = 1
        const val REQUEST_NFC_CHANGE_CVC = 2
        const val REQUEST_NFC_ADD_KEY = 3
        const val REQUEST_NFC_SIGN_TRANSACTION = 4
        const val REQUEST_NFC_VIEW_BACKUP_KEY = 5
        const val REQUEST_NFC_TOPUP_XPUBS = 6
        const val REQUEST_NFC_HEALTH_CHECK = 7
        // SATSCARD
        const val REQUEST_AUTO_CARD_STATUS = 8
        const val REQUEST_SATSCARD_SWEEP_SLOT = 9
        const val REQUEST_SATSCARD_SETUP = 10
    }
}