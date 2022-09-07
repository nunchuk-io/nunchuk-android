package com.nunchuk.android.wallet.personal.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityAddRecoverWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddRecoverWalletActivity : BaseActivity<ActivityAddRecoverWalletBinding>() {

    private val viewModel: RecoverWalletViewModel by viewModels()

    private val recoverWalletData: RecoverWalletData
        get() = intent.getParcelableExtra(EXTRAS_DATA)!!

    override fun initializeBinding() = ActivityAddRecoverWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()
        viewModel.init()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: RecoverWalletState) {
        bindWalletCounter(state.walletName)
    }


    private fun handleEvent(event: RecoverWalletEvent) {
        when (event) {
            is RecoverWalletEvent.ImportWalletErrorEvent -> NCToastMessage(this).showError(event.message)
            is RecoverWalletEvent.ImportWalletSuccessEvent -> handleSuccessRecoverEvent(walletName = event.walletName, walletId = event.walletId)
            is RecoverWalletEvent.UpdateWalletErrorEvent -> NCToastMessage(this).showError(event.message)
            is RecoverWalletEvent.UpdateWalletSuccessEvent -> handleSuccessRecoverEvent(walletName = event.walletName, walletId = event.walletId)
            is RecoverWalletEvent.WalletSetupDoneEvent -> handleWalletSetupDoneEvent()
            RecoverWalletEvent.WalletNameRequiredEvent -> binding.walletName.setError(getString(R.string.nc_text_required))
        }
    }

    private fun handleWalletSetupDoneEvent() {
        val walletName = viewModel.walletName
        if (recoverWalletData.type == RecoverWalletType.FILE) {
            val filePath = recoverWalletData.filePath
            if (walletName != null && filePath != null) {
                importWallet(walletName, filePath)
            }
        } else {
            val walletId = recoverWalletData.walletId
            if (walletName != null && walletId != null) {
                updateWallet(walletName, walletId)
            }
        }
    }

    private fun handleSuccessRecoverEvent(walletId: String, walletName: String) {
        NCToastMessage(this).show(
            getString(
                R.string.nc_txt_import_wallet_success,
                walletName
            )
        )
        openWalletConfigScreen(walletId)
    }

    private fun importWallet(walletName: String, filePath: String) {
        viewModel.importWallet(
            filePath = filePath,
            name = walletName,
            description = ""
        )
    }

    private fun updateWallet(name: String, walletId: String) {
        viewModel.updateWallet(
            walletId = walletId,
            walletName = name
        )
    }

    private fun openWalletConfigScreen(walletId: String) {
        navigator.openWalletConfigScreen(this, walletId)
        finish()
    }


    private fun setupViews() {
        binding.walletName.getEditTextView().filters = arrayOf(LengthFilter(MAX_LENGTH))
        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)
        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    private fun bindWalletCounter(walletName: String) {
        val counter = "${walletName.length}/${MAX_LENGTH}"
        binding.walletNameCounter.text = counter
    }

    override fun onBackPressed() {
        if (recoverWalletData.walletId.isNullOrEmpty().not()) {
            recoverWalletData.walletId?.let {
                openWalletConfigScreen(it)
            }
        }
        super.onBackPressed()
    }

    companion object {
        private const val MAX_LENGTH = 20
        private const val EXTRAS_DATA = "EXTRAS_DATA"

        fun start(activityContext: Context, data: RecoverWalletData) {
            val intent = Intent(activityContext, AddRecoverWalletActivity::class.java).apply {
                putExtra(EXTRAS_DATA, data)
            }
            activityContext.startActivity(intent)
        }
    }
}