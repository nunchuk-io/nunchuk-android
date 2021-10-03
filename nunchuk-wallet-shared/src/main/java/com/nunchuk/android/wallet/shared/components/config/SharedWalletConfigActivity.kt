package com.nunchuk.android.wallet.shared.components.config

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.RoomWalletData
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.components.config.WalletUpdateBottomSheet
import com.nunchuk.android.wallet.shared.R
import com.nunchuk.android.wallet.shared.components.config.SharedWalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.shared.components.config.SharedWalletConfigEvent.UpdateNameSuccessEvent
import com.nunchuk.android.wallet.shared.databinding.ActivitySharedWalletConfigBinding
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class SharedWalletConfigActivity : BaseActivity<ActivitySharedWalletConfigBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: SharedWalletConfigViewModel by viewModels { factory }

    private val args: SharedWalletConfigArgs by lazy { SharedWalletConfigArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivitySharedWalletConfigBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: SharedWalletConfigEvent) {
        when (event) {
            UpdateNameSuccessEvent -> showEditWalletSuccess()
            is UpdateNameErrorEvent -> NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun handleState(wallet: Wallet) {
        binding.walletName.text = wallet.name

        binding.configuration.bindWalletConfiguration(wallet)

        binding.walletType.text = (if (wallet.escrow) WalletType.ESCROW else WalletType.MULTI_SIG).toReadableString(this)
        binding.addressType.text = wallet.addressType.toReadableString(this)

        SignersViewBinder(binding.signersContainer, wallet.signers.map(SingleSigner::toModel)).bindItems()
    }

    private fun setupViews() {
        binding.walletName.setOnClickListener { onEditClicked() }
        binding.btnDone.setOnClickListener {
            finish()
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        args.roomWalletData?.let(::bindWalletData)
    }

    private fun bindWalletData(roomWalletData: RoomWalletData) {
        binding.walletName.text = roomWalletData.name
        binding.configuration.bindWalletConfiguration(
            totalSigns = roomWalletData.totalSigners,
            requireSigns = roomWalletData.requireSigners
        )

        binding.walletType.text = (if (roomWalletData.isEscrow) WalletType.ESCROW else WalletType.MULTI_SIG).toReadableString(this)
        binding.addressType.text = AddressType.values().firstOrNull { it.name == roomWalletData.addressType }?.toReadableString(this)
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

        fun start(activityContext: Context, roomWalletData: RoomWalletData) {
            activityContext.startActivity(SharedWalletConfigArgs(roomWalletData = roomWalletData).buildIntent(activityContext))
        }
    }

}