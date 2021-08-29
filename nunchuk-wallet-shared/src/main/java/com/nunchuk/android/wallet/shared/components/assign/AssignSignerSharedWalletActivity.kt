package com.nunchuk.android.wallet.shared.components.assign

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.shared.R
import com.nunchuk.android.wallet.shared.components.assign.AssignSignerEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.shared.components.assign.AssignSignerEvent.AssignSignerErrorEvent
import com.nunchuk.android.wallet.shared.databinding.ActivityAssignSignerBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AssignSignerSharedWalletActivity : BaseActivity<ActivityAssignSignerBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: AssignSignerArgs by lazy { AssignSignerArgs.deserializeFrom(intent) }

    private val viewModel: AssignSignerViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityAssignSignerBinding.inflate(layoutInflater)

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

    private fun handleEvent(event: AssignSignerEvent) {
        when (event) {
            is AssignSignerCompletedEvent -> openRoom(event.roomId)
            is AssignSignerErrorEvent -> NCToastMessage(this).showError(event.message)
        }
    }

    private fun openRoom(roomId: String) {
        ActivityManager.instance.popUntilRoot()
        navigator.openRoomDetailActivity(this, roomId)
    }

    private fun handleState(state: AssignSignerState) {
        bindSigners(state.masterSigners.map(MasterSigner::toModel) + state.remoteSigners.map(SingleSigner::toModel), state.selectedPFXs)
        val slot = args.requireSigns - state.selectedPFXs.size
        binding.slot.text = getString(R.string.nc_wallet_slots_left_in_the_wallet, slot)
    }

    private fun bindSigners(signers: List<SignerModel>, selectedPFXs: List<String>) {
        val canSelect = args.requireSigns - selectedPFXs.size
        SignersViewBinder(
            container = binding.signersContainer,
            signers = signers,
            canSelect = canSelect > 0,
            selectedXpfs = selectedPFXs,
            onItemSelectedListener = viewModel::updateSelectedXfps
        ).bindItems()
    }

    private fun setupViews() {
        binding.signersContainer.removeAllViews()
        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent(
                walletType = args.walletType,
                addressType = args.addressType
            )
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType,
            totalSigns: Int,
            requireSigns: Int
        ) {
            activityContext.startActivity(
                AssignSignerArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    totalSigns = totalSigns,
                    requireSigns = requireSigns
                ).buildIntent(activityContext)
            )
        }

    }

}
