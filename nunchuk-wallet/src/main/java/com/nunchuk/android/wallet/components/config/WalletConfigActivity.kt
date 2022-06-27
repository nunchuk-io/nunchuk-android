package com.nunchuk.android.wallet.components.config

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import com.nunchuk.android.wallet.databinding.ActivityWalletConfigBinding
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletConfigActivity : BaseActivity<ActivityWalletConfigBinding>() {

    private val viewModel: WalletConfigViewModel by viewModels()

    private val args: WalletConfigArgs by lazy { WalletConfigArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityWalletConfigBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.walletId)
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: WalletConfigEvent) {
        when (event) {
            UpdateNameSuccessEvent -> showEditWalletSuccess()
            is UpdateNameErrorEvent -> NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun handleState(walletExtended: WalletExtended) {
        val wallet = walletExtended.wallet
        binding.walletName.text = wallet.name

        binding.configuration.bindWalletConfiguration(wallet)

        binding.walletType.text = (if (wallet.escrow) WalletType.ESCROW else WalletType.MULTI_SIG).toReadableString(this)
        binding.addressType.text = wallet.addressType.toReadableString(this)
        binding.shareIcon.isVisible = walletExtended.isShared
        SignersViewBinder(binding.signersContainer, wallet.signers.map(SingleSigner::toModel)).bindItems()
    }

    private fun setupViews() {
        binding.walletName.setOnClickListener { onEditClicked() }
        binding.btnDone.setOnClickListener {
            navigator.openMainScreen(this)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun onEditClicked() {
        val bottomSheet = WalletUpdateBottomSheet.show(
            fragmentManager = supportFragmentManager,
            walletName = binding.walletName.text.toString()
        )

        bottomSheet.setListener(viewModel::handleEditCompleteEvent)
    }

    private fun showEditWalletSuccess() {
        binding.root.post { NCToastMessage(this).show(R.string.nc_text_change_wallet_success) }
    }

    companion object {

        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(WalletConfigArgs(walletId = walletId).buildIntent(activityContext))
        }
    }

}