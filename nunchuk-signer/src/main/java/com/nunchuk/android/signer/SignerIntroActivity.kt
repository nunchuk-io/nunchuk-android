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
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import com.nunchuk.android.signer.nfc.NfcSetupActivity
import com.nunchuk.android.signer.util.showAddNfcKey
import com.nunchuk.android.signer.util.showNfcAlreadyAdded
import com.nunchuk.android.signer.util.showSetupNfc
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SignerIntroActivity : BaseNfcActivity<ActivitySignerIntroBinding>() {
    private val viewModel : SignerIntroViewModel by viewModels()

    override fun initializeBinding() = ActivitySignerIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observer()
    }

    private fun setupViews() {
        binding.btnAddNFC.setOnClickListener { startNfcFlow(REQUEST_NFC_STATUS) }
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
                viewModel.tapSignerStatus.collect {
                    handleTapSignerStatus(it)
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

    private fun handleTapSignerStatus(status: TapSignerStatus) {
        if (status.isSetup) {
            if (status.isCreateSigner) {
                showNfcAlreadyAdded()
            } else {
                showAddNfcKey {
                    navigateToAddNfcKeySigner()
                }
            }
        } else {
            showSetupNfc {
                navigateToSetupNfc()
            }
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignerIntroActivity::class.java))
        }
    }
}