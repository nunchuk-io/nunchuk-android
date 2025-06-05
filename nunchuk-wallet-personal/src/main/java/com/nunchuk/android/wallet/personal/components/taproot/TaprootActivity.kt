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

package com.nunchuk.android.wallet.personal.components.taproot

import android.content.Context
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_TOPUP_XPUBS
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.args.ConfigureWalletArgs
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.ConfigureWalletEvent
import com.nunchuk.android.wallet.ConfigureWalletViewModel
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.taproot.configure.configureValueKeySetScreen
import com.nunchuk.android.wallet.personal.components.taproot.configure.navigateConfigureValueKeySet
import com.nunchuk.android.wallet.personal.components.taproot.configure.navigateTaprootConfigScreen
import com.nunchuk.android.wallet.personal.components.taproot.configure.taprootConfigScreen
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.NCWarningVerticalDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class TaprootActivity : BaseComposeNfcActivity(), InputBipPathBottomSheetListener,
    BottomSheetOptionListener {

    private val args: TaprootWarningArgs by lazy { TaprootWarningArgs.deserializeFrom(intent) }
    private val viewModel: ConfigureWalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        viewModel.init(
            ConfigureWalletArgs(
                walletName = args.walletName,
                walletType = args.walletType,
                addressType = AddressType.TAPROOT,
                decoyPin = args.decoyPin,
                quickWalletParam = args.quickWalletParam,
            )
        )
        viewModel.initGroupSandBox(args.groupSandboxId)
        setContent {
            val navController = rememberNavController()

            LaunchedEffect(Unit) {
                viewModel.event.collect {
                    if (it is ConfigureWalletEvent.OpenConfigKeySet) {
                        navController.navigateConfigureValueKeySet()
                    } else if (it is ConfigureWalletEvent.ShowEnableKeySet) {
                        navController.navigateEnableValueKey()
                    }
                }
            }

            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = TaprootIntroScreenRoute
            ) {
                taprootIntroScreen {
                    navController.navigateTaprootWarningScreen()
                }
                taprootWarningScreen {
                    if (viewModel.isSelectedKeyAvailable()) {
                        navController.navigateEnableValueKey()
                    } else {
                        navController.navigateTaprootConfigScreen()
                    }
                }
                taprootConfigScreen(
                    viewModel = viewModel,
                    onContinue = {
                        viewModel.resetKeySet()
                        viewModel.checkShowRiskSignerDialog()
                    },
                    onSelectSigner = { model, checked ->
                        if (model.type == SignerType.SOFTWARE && viewModel.isUnBackedUpSigner(model) && checked) {
                            showUnBackedUpSignerWarning()
                        } else {
                            viewModel.updateSelectedSigner(
                                signer = model,
                                checked = checked,
                            )
                        }
                    },
                    onEditPath = { model ->
                        InputBipPathBottomSheet.show(
                            supportFragmentManager,
                            model.id,
                            model.derivationPath
                        )
                    },
                    onToggleShowPath = {
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
                    }
                )
                configureValueKeySetScreen(viewModel) {
                    viewModel.handleContinueEvent()
                }
                enableValueKeyScreen(
                    onContinue = viewModel::setEnableKeySet
                )
            }
        }

        observeEvent()
    }


    override fun onInputDone(masterSignerId: String, newInput: String) {
        viewModel.changeBip32Path(masterSignerId, newInput)
    }

    override fun onOptionClicked(option: SheetOption) {
        viewModel.toggleShowPath()
    }

    private fun observeEvent() {
        flowObserver(viewModel.event, collector = ::handleEvent)
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
            is ConfigureWalletEvent.AssignSignerCompletedEvent -> openWalletConfirmScreen(
                totalRequireSigns = event.totalRequireSigns,
                signers = event.signers,
            )

            is ConfigureWalletEvent.Loading -> showOrHideLoading(event.loading)
            is ConfigureWalletEvent.PromptInputPassphrase -> requireInputPassphrase(event.signer)
            is ConfigureWalletEvent.ShowError -> NCToastMessage(this).showError(event.message)
            ConfigureWalletEvent.ChangeBip32Success -> NCToastMessage(this).show(getString(R.string.nc_bip_32_updated))
            is ConfigureWalletEvent.ShowRiskSignerDialog -> {
                if (event.isShow) {
                    showRiskSignerDialog()
                } else {
                    continueActionForRiskDialog()
                }
            }

            is ConfigureWalletEvent.RequestCacheTapSignerXpub -> handleCacheXpub(event.signer)
            is ConfigureWalletEvent.CacheTapSignerXpubError -> handleCacheXpubError(event)
            is ConfigureWalletEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading)
            ConfigureWalletEvent.OpenConfigKeySet, ConfigureWalletEvent.ShowEnableKeySet -> Unit
        }
    }

    private fun continueActionForRiskDialog() {
        if (viewModel.isSingleSig()) {
            viewModel.handleContinueEvent()
        } else {
            viewModel.showEnableKeySet()
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
        signers: List<SingleSigner>,
    ) {
        navigator.openReviewWalletScreen(
            activityContext = this,
            args = ReviewWalletArgs(
                walletName = args.walletName,
                walletType = args.walletType,
                addressType = args.addressType,
                decoyPin = args.decoyPin,
                totalRequireSigns = totalRequireSigns,
                signers = signers,
                groupId = args.groupSandboxId,
                isValueKeySetEnable = viewModel.isValueKeySetEnable(),
                quickWalletParam = args.quickWalletParam,
            )
        )
    }

    private fun showUnBackedUpSignerWarning() {
        NCInfoDialog(this).showDialog(
            message = getString(R.string.nc_unbacked_up_signer_warning_desc),
            onYesClick = {}
        )
    }

    private fun showRiskSignerDialog() {
        NCWarningVerticalDialog(this).showDialog(
            message = getString(R.string.nc_risk_signer_key_warning_desc),
            btnYes = getString(R.string.nc_risk_signer_key_warning_button),
            btnNeutral = getString(R.string.nc_text_cancel),
            btnNo = "",
            onYesClick = ::continueActionForRiskDialog)
    }

    companion object {
        fun start(
            activityContext: Context,
            walletName: String,
            walletType: WalletType,
            addressType: AddressType,
            decoyPin: String,
            groupSandboxId: String,
            quickWalletParam: QuickWalletParam?,
        ) {
            activityContext.startActivity(
                TaprootWarningArgs(
                    walletName = walletName,
                    walletType = walletType,
                    addressType = addressType,
                    decoyPin = decoyPin,
                    groupSandboxId = groupSandboxId,
                    quickWalletParam = quickWalletParam,
                ).buildIntent(activityContext)
            )
        }
    }
}
