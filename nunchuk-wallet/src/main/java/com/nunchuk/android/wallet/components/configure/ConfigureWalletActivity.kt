/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.components.configure

import android.content.Context
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
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
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ConfigureWalletActivity : BaseNfcActivity<ActivityConfigureWalletBinding>(),
    InputBipPathBottomSheetListener, BottomSheetOptionListener {

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

    override fun onOptionClicked(option: SheetOption) {
        viewModel.toggleShowPath()
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
            is ConfigureWalletEvent.RequestCacheTapSignerXpub -> handleCacheXpub(event.signer)
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
            runBlocking {
                viewModel.mapSigners(
                    masterSigners = state.masterSigners,
                    remoteSigners = state.remoteSigners
                )
            },
            state.selectedSigners,
            state.isShowPath
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

    // TODO Hai Recycler view
    private fun bindSigners(
        signers: List<SignerModel>,
        selectedPFXs: Set<SignerModel>,
        isShowPath: Boolean
    ) {
        SignersViewBinder(
            container = binding.signersContainer,
            signers = signers,
            selectedSigners = selectedPFXs,
            isShowPath = isShowPath,
            onItemSelectedListener = { model, checked ->
                viewModel.updateSelectedSigner(
                    signer = model,
                    checked = checked,
                )
            }, onEditPath = { model ->
                InputBipPathBottomSheet.show(
                    supportFragmentManager,
                    model.id,
                    model.derivationPath
                )
            }
        ).bindItems()
    }

    private fun setupViews() {
        binding.toolbar.setOnMenuItemClickListener {
            BottomSheetOption.newInstance(
                options = listOf(
                    SheetOption(
                        0,
                        label = if (viewModel.isShowPath())
                            getString(R.string.nc_hide_bip_32_path)
                        else
                            getString(R.string.nc_show_bip_32_path)
                    ),
                )
            ).show(supportFragmentManager, "BottomSheetOption")
            true
        }
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
