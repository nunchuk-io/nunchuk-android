package com.nunchuk.android.wallet.assign

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.assign.AssignSignerEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.databinding.ActivityWalletAssignSignerBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AssignSignerActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: AssignSignerArgs by lazy { AssignSignerArgs.deserializeFrom(intent) }

    private val viewModel: AssignSignerViewModel by viewModels { factory }

    private lateinit var binding: ActivityWalletAssignSignerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityWalletAssignSignerBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            is AssignSignerCompletedEvent -> openWalletConfirmScreen(
                totalRequireSigns = event.totalRequireSigns,
                masterSigners = event.masterSigners,
                remoteSigners = event.remoteSigners
            )
        }
    }

    private fun openWalletConfirmScreen(totalRequireSigns: Int, masterSigners: List<MasterSigner>, remoteSigners: List<SingleSigner>) {
        navigator.openWalletConfirmScreen(
            activityContext = this,
            walletName = args.walletName,
            walletType = args.walletType,
            addressType = args.addressType,
            totalRequireSigns = totalRequireSigns,
            masterSigners = masterSigners,
            remoteSigners = remoteSigners
        )
    }

    private fun handleState(state: AssignSignerState) {
        bindSigners(state.masterSigners.map(MasterSigner::toModel) + state.remoteSigners.map(SingleSigner::toModel), state.selectedPFXs)
        bindTotalRequireSigns(state.totalRequireSigns)
        val totalRequireSignsValue = "${state.totalRequireSigns}/${state.selectedPFXs.size} ${getString(R.string.nc_wallet_multisig)}"
        binding.totalRequireSigns.text = totalRequireSignsValue
    }

    private fun bindTotalRequireSigns(totalRequireSigns: Int) {
        binding.requiredSingerCounter.text = "$totalRequireSigns"
    }

    private fun bindSigners(signers: List<SignerModel>, selectedPFXs: List<String>) {
        SignersViewBinder(binding.signersContainer, signers, selectedPFXs, viewModel::updateSelectedXfps).bindItems()
    }

    private fun setupViews() {
        binding.signersContainer.removeAllViews()
        binding.iconPlus.setOnClickListener { viewModel.handleIncreaseRequiredSigners() }
        binding.iconMinus.setOnClickListener { viewModel.handleDecreaseRequiredSigners() }
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {

        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(AssignSignerArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }

}