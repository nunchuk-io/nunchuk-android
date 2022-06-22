package com.nunchuk.android.signer

import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import com.nunchuk.android.signer.ui.nfc.NfcSetupActivity
import com.nunchuk.android.utils.PendingIntentUtils
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SignerIntroActivity : BaseActivity<ActivitySignerIntroBinding>() {
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
    private val nfcPendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntentUtils.getFlagCompat()
        )
    }

    override fun initializeBinding() = ActivitySignerIntroBinding.inflate(layoutInflater)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            processNfcIntent(intent)
        }
    }

    override fun onPause() {
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }

    private fun setupViews() {
        binding.btnAddNFC.setOnClickListener { moveToNFCScreen() }
        binding.btnAddAirSigner.setOnClickListener { openAddAirSignerIntroScreen() }
        binding.btnAddSSigner.setOnClickListener { openAddSoftwareSignerScreen() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun moveToNFCScreen() {
        navigateToSetupNfc()
        // TODO uncomment
//        nfcAdapter?.let {
//            if (it.isEnabled) {
//                askToScan()
//            } else {
//                requestEnableNfc.launch(Intent(this, TurnOnNfcActivity::class.java))
//            }
//        } ?: run {
//            nfcUnsupportedDialog.show()
//        }
    }

    private fun openAddAirSignerIntroScreen() {
        finish()
        navigator.openAddAirSignerIntroScreen(this)
    }

    private fun openAddSoftwareSignerScreen() {
        finish()
        navigator.openAddSoftwareSignerScreen(this)
    }

    private fun navigateToSetupNfc() {
        NfcSetupActivity.navigate(this)
    }

    private fun askToScan() {
        NCInfoDialog(this).init(
            title = "Ready to Scan",
            message = "Hold your device near the NFC key."
        ).show()
    }

    private fun processNfcIntent(intent: Intent) {
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMsgs ->
            (rawMsgs[0] as NdefMessage).apply {
                Timber.d(String(records[0].payload))
            }
            // TODO check setup or not
            val isSetup = true
            if (isSetup) {
                NCWarningDialog(this).showDialog(
                    title = getString(R.string.nc_add_nfc_key),
                    message = getString(R.string.nc_add_nfc_key_desc),
                    onYesClick = {

                    }
                ).show()
            } else {
                NCWarningDialog(this).showDialog(
                    title = getString(R.string.nc_set_up_nfc_key),
                    message = getString(R.string.nc_setup_nfc_key_desc),
                    onYesClick = {
                        navigateToSetupNfc()
                    }
                ).show()
            }
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignerIntroActivity::class.java))
        }
    }
}