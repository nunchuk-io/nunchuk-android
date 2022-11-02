package com.nunchuk.android.wallet.shared.components.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.shared.R
import com.nunchuk.android.wallet.shared.databinding.ActivityAddRecoverSharedWalletBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddRecoverSharedWalletActivity : BaseActivity<ActivityAddRecoverSharedWalletBinding>() {

    private val viewModel: RecoverSharedWalletViewModel by viewModels()

    private val recoverWalletData: String?
        get() = intent.getStringExtra(EXTRAS_DATA)

    override fun initializeBinding() = ActivityAddRecoverSharedWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }


    private fun handleEvent(event: RecoverSharedWalletEvent) {
        when (event) {
            is RecoverSharedWalletEvent.RecoverSharedWalletSuccess -> {
                val walletName = viewModel.walletName.orEmpty()
                navigator.openReviewSharedWalletScreen(
                    activityContext = this,
                    walletName = walletName,
                    walletType = if (event.wallet.escrow) WalletType.ESCROW else WalletType.MULTI_SIG,
                    addressType = event.wallet.addressType,
                    totalSigns = event.wallet.signers.size,
                    requireSigns = event.wallet.totalRequireSigns,
                    signers = event.wallet.signers
                )
            }
            is RecoverSharedWalletEvent.WalletSetupDoneEvent -> handleWalletSetupDoneEvent()
            RecoverSharedWalletEvent.WalletNameRequiredEvent -> binding.walletName.setError(
                getString(
                    R.string.nc_text_required
                )
            )
            is RecoverSharedWalletEvent.ShowError -> NCToastMessage(this).showError(event.message)
        }
    }

    private fun handleState(state: RecoverSharedWalletState) {
        bindWalletCounter(state.walletName)
    }

    private fun handleWalletSetupDoneEvent() {
        recoverWalletData?.let {
            viewModel.parseWalletDescriptor(it)
        }
    }

    private fun setupViews() {
        binding.walletName.getEditTextView().filters = arrayOf(LengthFilter(MAX_LENGTH))
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
        private const val EXTRAS_DATA = "EXTRAS_DATA"

        fun start(activityContext: Context, data: String) {
            val intent = Intent(activityContext, AddRecoverSharedWalletActivity::class.java).apply {
                putExtra(EXTRAS_DATA, data)
            }
            activityContext.startActivity(intent)
        }
    }
}