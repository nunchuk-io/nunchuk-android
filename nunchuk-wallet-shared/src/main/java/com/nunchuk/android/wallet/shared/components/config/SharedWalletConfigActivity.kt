package com.nunchuk.android.wallet.shared.components.config

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.isPendingKeys
import com.nunchuk.android.core.util.isReadyFinalize
import com.nunchuk.android.messages.components.detail.bindWalletStatus
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.RoomWalletData
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.shared.R
import com.nunchuk.android.wallet.shared.components.config.SharedWalletConfigEvent.CreateSharedWalletSuccess
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
            CreateSharedWalletSuccess -> NCToastMessage(this).showMessage(getString(R.string.nc_message_wallet_created))
        }
    }

    private fun handleState(state: SharedWalletConfigState) {
        SignersViewBinder(binding.signersContainer, state.signerModels).bindItems()
        state.roomWallet?.let(::bindRoomWallet)
    }

    private fun bindRoomWallet(roomWallet: RoomWallet) {
        binding.status.bindWalletStatus(roomWallet)
        when {
            roomWallet.isPendingKeys() -> {
                binding.btnDone.text = getString(R.string.nc_wallet_continue_assign_key)
                binding.btnDone.isVisible = true
                binding.btnDone.setOnClickListener { openAssignSignerSharedWalletScreen() }
            }
            roomWallet.isReadyFinalize() -> {
                binding.btnDone.text = getString(R.string.nc_wallet_finalize_wallet)
                binding.btnDone.isVisible = true
                binding.btnDone.setOnClickListener { viewModel.finalizeWallet() }
            }
            else -> {
                binding.btnDone.isVisible = false
            }
        }
    }

    private fun openAssignSignerSharedWalletScreen() {
        args.roomWalletData?.let {
            navigator.openAssignSignerSharedWalletScreen(
                this,
                walletName = it.name,
                walletType = if (it.isEscrow) WalletType.MULTI_SIG else WalletType.ESCROW,
                addressType = AddressType.valueOf(it.addressType),
                totalSigns = it.totalSigners,
                requireSigns = it.requireSigners
            )
        }
    }

    private fun setupViews() {
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

    companion object {
        fun start(activityContext: Context, roomWalletData: RoomWalletData) {
            activityContext.startActivity(SharedWalletConfigArgs(roomWalletData = roomWalletData).buildIntent(activityContext))
        }
    }

}