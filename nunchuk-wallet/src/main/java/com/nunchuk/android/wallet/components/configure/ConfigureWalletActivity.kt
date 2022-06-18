package com.nunchuk.android.wallet.components.configure

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.Loading
import com.nunchuk.android.wallet.databinding.ActivityConfigureWalletBinding
import com.nunchuk.android.wallet.util.bindWalletConfiguration
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigureWalletActivity : BaseActivity<ActivityConfigureWalletBinding>() {

    private val args: ConfigureWalletArgs by lazy { ConfigureWalletArgs.deserializeFrom(intent) }

    private val viewModel: ConfigureWalletViewModel by viewModels()

    override fun initializeBinding() = ActivityConfigureWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()

        viewModel.init(args.addressType.isTaproot())
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
            is Loading -> showOrHideLoading(event.loading)
            is ConfigureWalletEvent.PromptInputPassphrase -> requireInputPassphrase(event.func)
            is ConfigureWalletEvent.InputPassphraseError -> NCToastMessage(this).showError(event.message)
        }
    }

    private fun requireInputPassphrase(func: (String) -> Unit) {
        NCInputDialog(this).showDialog(
            title = getString(R.string.nc_transaction_enter_passphrase),
            onConfirmed = func
        )
    }

    private fun openWalletConfirmScreen(
        totalRequireSigns: Int,
        masterSigners: List<MasterSigner>,
        remoteSigners: List<SingleSigner>
    ) {
        navigator.openReviewWalletScreen(
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
        val requireSigns = state.totalRequireSigns
        val totalSigns = state.selectedSigners.size
        bindSigners(
            state.masterSigners,
            state.masterSigners.map(MasterSigner::toModel) + state.remoteSigners.map(SingleSigner::toModel),
            state.selectedSigners
        )
        bindTotalRequireSigns(requireSigns)
        binding.totalRequireSigns.bindWalletConfiguration(
            totalSigns = totalSigns,
            requireSigns = requireSigns
        )
    }

    private fun bindTotalRequireSigns(totalRequireSigns: Int) {
        binding.requiredSingerCounter.text = "$totalRequireSigns"
    }

    private fun bindSigners(masterSigners: List<MasterSigner>, signers: List<SignerModel>, selectedPFXs: List<SignerModel>) {
        SignersViewBinder(
            binding.signersContainer,
            signers,
            selectedPFXs
        ) { model, checked ->
            val device = masterSigners.find { it.device.masterFingerprint == model.fingerPrint }?.device
            viewModel.updateSelectedSigner(
                signer = model,
                checked = checked,
                needPassPhraseSent = checked && device?.needPassPhraseSent.orFalse()
            )
        }.bindItems()
    }

    private fun setupViews() {
        binding.signersContainer.removeAllViews()

        if (args.addressType.isTaproot()) {
            setupViewVisibility(enabled = false, alpha = 0.5F)
        } else {
            setupViewVisibility(enabled = true, alpha = 1F)
        }

        binding.iconPlus.setOnClickListener { viewModel.handleIncreaseRequiredSigners() }
        binding.iconMinus.setOnClickListener { viewModel.handleDecreaseRequiredSigners() }
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViewVisibility(enabled: Boolean, alpha: Float) {
        binding.iconPlus.isEnabled = enabled
        binding.iconPlus.alpha = alpha
        binding.iconMinus.isEnabled = enabled
        binding.iconMinus.alpha = alpha
        binding.requiredSingerCounter.isEnabled = enabled
    }

    companion object {

        fun start(activityContext: Context, walletName: String, walletType: WalletType, addressType: AddressType) {
            activityContext.startActivity(ConfigureWalletArgs(walletName, walletType, addressType).buildIntent(activityContext))
        }
    }

}
