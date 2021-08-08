package com.nunchuk.android.wallet.components.configure

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
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.databinding.ActivityConfigureWalletBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ConfigureWalletActivity : BaseActivity<ActivityConfigureWalletBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: ConfigureWalletArgs by lazy { ConfigureWalletArgs.deserializeFrom(intent) }

    private val viewModel: ConfigureWalletViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityConfigureWalletBinding.inflate(layoutInflater)

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

    private fun handleEvent(event: ConfigureWalletEvent) {
        when (event) {
            is AssignSignerCompletedEvent -> openWalletConfirmScreen(
                totalRequireSigns = event.totalRequireSigns,
                masterSigners = event.masterSigners,
                remoteSigners = event.remoteSigners
            )
        }
    }

    private fun openWalletConfirmScreen(
        totalRequireSigns: Int,
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    ) {
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

    private fun handleState(state: ConfigureWalletState) {
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
            activityContext.startActivity(ConfigureWalletArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }

}