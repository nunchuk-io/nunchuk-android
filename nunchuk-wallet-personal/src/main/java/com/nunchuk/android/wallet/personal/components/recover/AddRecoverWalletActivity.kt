package com.nunchuk.android.wallet.personal.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityAddRecoverWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

// TODO: merge with AddWalletActivity later to avoid duplicate code
class AddRecoverWalletActivity : BaseActivity<ActivityAddRecoverWalletBinding>() {
    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: RecoverWalletViewModel by viewModels { factory }

    private val filePath
        get() = intent.getStringExtra(EXTRAS_FILE_PATH)

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
            is RecoverWalletEvent.ImportWalletErrorEvent -> NCToastMessage(this).show(event.message)
            is RecoverWalletEvent.ImportWalletSuccessEvent -> {
                NCToastMessage(this).show(getString(R.string.nc_txt_import_wallet_success, event.walletName))
                openWalletConfigScreen(event.walletId)
            }
            is RecoverWalletEvent.WalletSetupDoneEvent -> importWallet()
            RecoverWalletEvent.WalletNameRequiredEvent -> binding.walletName.setError(getString(R.string.nc_text_required))
        }
    }

    private fun importWallet() {
        val walletName = viewModel.walletName
        val filePath = filePath
        if (walletName != null && filePath != null) {
            viewModel.importWallet(
                filePath = filePath,
                name = walletName,
                description = ""
            )
        }
    }

    private fun openWalletConfigScreen(walletId: String) {
        navigator.openWalletConfigScreen(this, walletId)
    }


    private fun setupViews() {
        binding.walletName.getEditTextView().filters = arrayOf(LengthFilter(MAX_LENGTH))
        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)
        binding.walletName.addTextChangedCallback(viewModel::updateWalletName)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    private fun bindWalletCounter(walletName: String) {
        val counter = "${walletName.length}/${MAX_LENGTH}"
        binding.walletNameCounter.text = counter
    }

    companion object {
        private const val MAX_LENGTH = 20
        private const val EXTRAS_FILE_PATH = "EXTRAS_FILE_PATH"

        fun start(activityContext: Context, filePath: String) {
            val intent = Intent(activityContext, AddRecoverWalletActivity::class.java).apply {
                putExtra(EXTRAS_FILE_PATH, filePath)
            }
            activityContext.startActivity(intent)
        }
    }
}