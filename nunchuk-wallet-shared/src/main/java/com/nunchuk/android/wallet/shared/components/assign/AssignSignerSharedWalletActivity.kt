package com.nunchuk.android.wallet.shared.components.assign

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssignSignerSharedWalletActivity : BaseActivity<ActivityAssignSignerBinding>() {

    private val args: AssignSignerArgs by lazy { AssignSignerArgs.deserializeFrom(intent) }

    private val viewModel: AssignSignerViewModel by viewModels()

    private var emptyStateView: View? = null

    override fun initializeBinding() = ActivityAssignSignerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()

        if (args.signers.isNotEmpty()) {
            args.signers.forEach { viewModel.filterSigners(it) }
        } else {
            viewModel.init()
        }
    }

    private fun setEmptyState() {
        emptyStateView = binding.viewStubEmptyState.inflate()
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
        ActivityManager.popUntilRoot()
        navigator.openRoomDetailActivity(this, roomId)
    }

    private fun handleState(state: AssignSignerState) {
        val signers = if (args.signers.isNotEmpty()) {
            state.masterSigners.map(MasterSigner::toModel) + state.filterRecSigners.map(SingleSigner::toModel)
        } else {
            state.masterSigners.map(MasterSigner::toModel) + state.remoteSigners.map(SingleSigner::toModel)
        }

        bindSigners(signers, state.selectedPFXs)

        emptyStateView?.isVisible = signers.isEmpty()

        val slot = args.totalSigns - state.selectedPFXs.size
        binding.slot.text = getString(R.string.nc_wallet_slots_left_in_the_wallet, slot)
    }

    private fun bindSigners(signers: List<SignerModel>, selectedPFXs: List<String>) {
        val canSelect = args.totalSigns - selectedPFXs.size
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

        setEmptyState()
        emptyStateView?.findViewById<View>(R.id.btnExit)?.setOnClickListener {
            navigator.returnRoomDetailScreen()
        }
        emptyStateView?.isVisible = true
    }

    companion object {

        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType,
            totalSigns: Int,
            requireSigns: Int,
            signers: List<SingleSigner>
        ) {
            activityContext.startActivity(
                AssignSignerArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    totalSigns = totalSigns,
                    requireSigns = requireSigns,
                    signers = signers
                ).buildIntent(activityContext)
            )
        }

    }

}
