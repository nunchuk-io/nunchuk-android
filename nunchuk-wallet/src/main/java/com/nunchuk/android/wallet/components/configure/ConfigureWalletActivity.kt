package com.nunchuk.android.wallet.components.configure

import android.content.Context
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.AssignSignerCompletedEvent
import com.nunchuk.android.wallet.components.configure.ConfigureWalletEvent.Loading
import com.nunchuk.android.wallet.databinding.ActivityConfigureWalletBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.NCWarningVerticalDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class ConfigureWalletActivity : BaseNfcActivity<ActivityConfigureWalletBinding>(),
    InputBipPathBottomSheetListener {

    private val args: ConfigureWalletArgs by lazy { ConfigureWalletArgs.deserializeFrom(intent) }

    private val viewModel: ConfigureWalletViewModel by viewModels()

    override fun initializeBinding() = ActivityConfigureWalletBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        setupViews()
        observeEvent()

        viewModel.init(args)
    }

    override fun onInputDone(masterSignerId: String, newInput: String) {
        viewModel.changeBip32Path(masterSignerId, newInput)
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }) {
            viewModel.cacheTapSignerXpub(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
            )
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleEvent(event: ConfigureWalletEvent) {
        when (event) {
            is AssignSignerCompletedEvent -> openWalletConfirmScreen(
                totalRequireSigns = event.totalRequireSigns,
                masterSigners = event.masterSigners,
                remoteSigners = event.remoteSigners
            )
            is Loading -> showOrHideLoading(event.loading)
            is ConfigureWalletEvent.PromptInputPassphrase -> requireInputPassphrase(event.signer)
            is ConfigureWalletEvent.ShowError -> NCToastMessage(this).showError(event.message)
            ConfigureWalletEvent.ChangeBip32Success -> NCToastMessage(this).show(getString(R.string.nc_bip_32_updated))
            is ConfigureWalletEvent.ShowRiskSignerDialog -> {
                if (event.isShow) {
                    showRiskSignerDialog()
                } else {
                    viewModel.handleContinueEvent()
                }
            }
            is ConfigureWalletEvent . RequestCacheTapSignerXpub -> handleCacheXpub(event.signer)
            is ConfigureWalletEvent.CacheTapSignerXpubError -> handleCacheXpubError(event)
            is ConfigureWalletEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
        }
    }

    private fun handleCacheXpub(signer: SignerModel) {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_info),
            message = getString(R.string.nc_new_xpub_need),
            btnYes = getString(R.string.nc_ok),
            btnNo = getString(R.string.nc_cancel),
            onYesClick = {
                startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)
            },
            onNoClick = {
                viewModel.cancelVerifyPassphrase(signer)
            }
        )
    }

    private fun handleCacheXpubError(event: ConfigureWalletEvent.CacheTapSignerXpubError) {
        if (nfcViewModel.handleNfcError(event.error).not()) {
            val message = event.error?.message.orUnknownError()
            NCToastMessage(this).showError(message)
        }
    }

    private fun requireInputPassphrase(signer: SignerModel) {
        NCInputDialog(this).showDialog(
            title = getString(R.string.nc_transaction_enter_passphrase),
            onConfirmed = {
                viewModel.verifyPassphrase(signer, it)
            },
            onCanceled = {
                viewModel.cancelVerifyPassphrase(signer)
            }
        )
    }

    private fun openWalletConfirmScreen(
        totalRequireSigns: Int,
        masterSigners: List<SingleSigner>,
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
            viewModel.mapSigners(),
            state.selectedSigners,
            state.masterSignerMap,
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

    private fun bindSigners(
        signers: List<SignerModel>,
        selectedPFXs: Set<SignerModel>,
        masterSignerMap: Map<String, SingleSigner>
    ) {
        SignersViewBinder(
            container = binding.signersContainer,
            signers = signers,
            selectedSigners = selectedPFXs,
            onItemSelectedListener = { model, checked ->
                viewModel.updateSelectedSigner(
                    signer = model,
                    checked = checked,
                )
            }, onEditPath = { model ->
                InputBipPathBottomSheet.show(
                    supportFragmentManager,
                    model.id,
                    masterSignerMap[model.id]?.derivationPath.orEmpty()
                )
            }
        ).bindItems()
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
        binding.btnContinue.setOnClickListener { viewModel.checkShowRiskSignerDialog() }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun showRiskSignerDialog() {
        NCWarningVerticalDialog(this).showDialog(
            message = getString(R.string.nc_risk_signer_key_warning_desc),
            btnYes = getString(R.string.nc_risk_signer_key_warning_button),
            btnNeutral = getString(R.string.nc_text_cancel),
            btnNo = "",
            onYesClick = {
                viewModel.handleContinueEvent()
            })
    }

    private fun setupViewVisibility(enabled: Boolean, alpha: Float) {
        binding.iconPlus.isEnabled = enabled
        binding.iconPlus.alpha = alpha
        binding.iconMinus.isEnabled = enabled
        binding.iconMinus.alpha = alpha
        binding.requiredSingerCounter.isEnabled = enabled
    }

    companion object {

        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType
        ) {
            activityContext.startActivity(
                ConfigureWalletArgs(
                    walletName,
                    walletType,
                    addressType
                ).buildIntent(activityContext)
            )
        }
    }

}
