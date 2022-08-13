package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import com.nunchuk.android.signer.nfc.NfcSetupActivity
import com.nunchuk.android.signer.nfc.SetUpNfcOptionSheet
import com.nunchuk.android.signer.util.showNfcAlreadyAdded
import com.nunchuk.android.signer.util.showSetupNfc
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SignerIntroActivity : BaseNfcActivity<ActivitySignerIntroBinding>(), SetUpNfcOptionSheet.OptionClickListener {
    private val viewModel : SignerIntroViewModel by viewModels()
    private val tapSignerStatus : TapSignerStatus? by lazy { intent.getParcelableExtra(EXTRA_TAP_SIGNER_STATUS) }

    override fun initializeBinding() = ActivitySignerIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observer()
        handleTapSignerStatus(tapSignerStatus)
    }

    override fun onOptionClickListener(option: SetUpNfcOptionSheet.SetUpNfcOption) {
        when(option) {
            SetUpNfcOptionSheet.SetUpNfcOption.ADD_NEW -> startNfcFlow(REQUEST_NFC_STATUS)
            SetUpNfcOptionSheet.SetUpNfcOption.RECOVER -> NfcSetupActivity.navigate(this, NfcSetupActivity.RECOVER_NFC)
        }
    }

    private fun setupViews() {
        binding.btnAddNFC.setOnClickListener {
            SetUpNfcOptionSheet.newInstance().show(supportFragmentManager, "SetUpNfcOptionSheet")
        }
        binding.btnAddAirSigner.setOnClickListener { openAddAirSignerIntroScreen() }
        binding.btnAddSSigner.setOnClickListener { openAddSoftwareSignerScreen() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observer() {
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_STATUS }
                    .collect {
                        viewModel.getTapSignerStatus(IsoDep.get(it.tag))
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect {
                    showOrHideLoading(it is SignerIntroState.Loading, message = getString(R.string.nc_keep_holding_nfc))
                    when(it) {
                        is SignerIntroState.GetTapSignerStatusSuccess -> handleTapSignerStatus(it.status)
                        is SignerIntroState.GetTapSignerStatusError -> {
                            val message = it.e?.message.orEmpty()
                            if (message.isNotEmpty()) {
                                NCToastMessage(this@SignerIntroActivity).showError(message)
                            }
                        }
                        else -> {}
                    }
                    viewModel.clearTapSignerStatus()
                }
            }
        }
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
        finish()
        NfcSetupActivity.navigate(this, NfcSetupActivity.SETUP_NFC)
    }

    private fun navigateToAddNfcKeySigner() {
        finish()
        NfcSetupActivity.navigate(this, NfcSetupActivity.ADD_KEY)
    }

    private fun handleTapSignerStatus(status: TapSignerStatus?) {
        status ?: return
        if (status.isNeedSetup.not()) {
            if (status.isCreateSigner) {
                showNfcAlreadyAdded()
            } else {
                navigateToAddNfcKeySigner()
            }
        } else {
            showSetupNfc {
                navigateToSetupNfc()
            }
        }
    }

    companion object {
        private const val EXTRA_TAP_SIGNER_STATUS = "EXTRA_TAP_SIGNER_STATUS"
        fun start(activityContext: Context, tapSignerStatus: TapSignerStatus? = null) {
            activityContext.startActivity(Intent(activityContext, SignerIntroActivity::class.java).apply {
                putExtra(EXTRA_TAP_SIGNER_STATUS, tapSignerStatus)
            })
        }
    }
}