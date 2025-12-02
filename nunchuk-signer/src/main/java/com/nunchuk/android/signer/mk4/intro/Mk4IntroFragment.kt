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

package com.nunchuk.android.signer.mk4.intro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isRecommendedMultiSigPath
import com.nunchuk.android.core.util.isRecommendedSingleSigPath
import com.nunchuk.android.core.util.isTestNetSigner
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.isParseAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResult
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import com.nunchuk.android.signer.mk4.recover.ColdcardRecoverFragmentDirections
import com.nunchuk.android.usecase.ResultExistingKey
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import java.util.Locale

@AndroidEntryPoint
class Mk4IntroFragment : MembershipFragment(), BottomSheetOptionListener {

    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<Mk4IntroViewModel>()
    private val mk4ViewModel by activityViewModels<Mk4ViewModel>()
    private val args: Mk4IntroFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val replacedXfp = (activity as Mk4Activity).replacedXfp.orEmpty()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                Mk4IntroScreen(
                    viewModel = viewModel,
                    isMembershipFlow = args.isMembershipFlow,
                    isReplaceKey = replacedXfp.isNotEmpty(),
                    onMoreClicked = ::handleShowMore
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        if (option.type >= WALLET_OFFSET) {
            if (action.isParseAction) {
                parseWalletSuccess(viewModel.state.value.wallets.find { it.id == option.id })
            } else {
                viewModel.createWallet(option.id.orEmpty())
            }
        } else if (option.type >= SIGNER_OFFSET) {
            val signer = viewModel.mk4Signers.getOrNull(option.type - SIGNER_OFFSET) ?: return
            viewModel.checkExistingKey(signer)
        }
    }

    private fun observer() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_ADD_KEY }) {
            viewModel.getMk4Signer(
                records = it.records,
                groupId = (activity as Mk4Activity).groupId,
                newIndex = (activity as Mk4Activity).newIndex,
                xfp = (activity as Mk4Activity).xfp,
                replacedXfp = (activity as Mk4Activity).replacedXfp,
                walletId = (activity as Mk4Activity).walletId,
                onChainAddSignerParam = (activity as Mk4Activity).onChainAddSignerParam
            )
            nfcViewModel.clearScanInfo()
        }

        flowObserver(viewModel.event) {
            when (it) {
                is Mk4IntroViewEvent.LoadMk4SignersSuccess -> openSignerSheet(it.signers)

                is Mk4IntroViewEvent.Loading -> showOrHideLoading(it.isLoading)
                is Mk4IntroViewEvent.ShowError -> showError(it.message)
                Mk4IntroViewEvent.OnContinueClicked -> onContinueClicked()
                is Mk4IntroViewEvent.OnCreateSignerSuccess -> {
                    val onChainAddSignerParam = (activity as Mk4Activity).onChainAddSignerParam
                    if (args.isAddInheritanceKey || onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) {
                        if (onChainAddSignerParam != null) {
                            if (onChainAddSignerParam.isClaiming) {
                                requireActivity().finish()
                            } else if (onChainAddSignerParam.currentSigner?.fingerPrint?.isNotEmpty() == true && onChainAddSignerParam.isVerifyBackupSeedPhrase()) {
                                if (it.signer.masterFingerprint == onChainAddSignerParam.currentSigner?.fingerPrint) {
                                    if (onChainAddSignerParam.isReplaceKeyFlow()) {
                                        viewModel.setReplaceKeyVerified(
                                            keyId = it.signer.masterFingerprint,
                                            groupId = (activity as Mk4Activity).groupId,
                                            walletId = (activity as Mk4Activity).walletId.orEmpty()
                                        )
                                    } else {
                                        viewModel.setKeyVerified(
                                            groupId = (activity as Mk4Activity).groupId,
                                            masterSignerId = it.signer.masterFingerprint
                                        )
                                    }
                                } else {
                                    requireActivity().setResult(Activity.RESULT_OK)
                                    navigator.returnMembershipScreen()
                                }
                            } else {
                                requireActivity().setResult(Activity.RESULT_OK)
                                navigator.returnMembershipScreen()
                            }
                        } else {
                            mk4ViewModel.setOrUpdate(
                                mk4ViewModel.coldCardBackUpParam.copy(
                                    xfp = it.signer.masterFingerprint,
                                    keyType = it.signer.type,
                                    keyName = it.signer.name
                                )
                            )
                            findNavController().navigate(
                                ColdcardRecoverFragmentDirections.actionColdcardRecoverFragmentToColdCardBackUpIntroFragment()
                            )
                        }
                    } else {
                        requireActivity().setResult(Activity.RESULT_OK)
                        requireActivity().finish()
                    }
                }

                Mk4IntroViewEvent.OnSignerExistInAssistedWallet -> showError(getString(R.string.nc_error_add_same_key))
                Mk4IntroViewEvent.ErrorMk4TestNet -> NCInfoDialog(requireActivity())
                    .showDialog(
                        title = getString(R.string.nc_error),
                        message = getString(R.string.nc_error_device_in_testnet_msg_v2)
                    )

                is Mk4IntroViewEvent.ImportWalletFromMk4Success -> openRecoverWalletName(it.walletId)
                is Mk4IntroViewEvent.ExtractWalletsFromColdCard -> showWallets(it.wallets)
                is Mk4IntroViewEvent.NfcLoading -> showOrHideNfcLoading(it.isLoading)
                is Mk4IntroViewEvent.ParseWalletFromMk4Success -> parseWalletSuccess(it.wallet)
                Mk4IntroViewEvent.NewIndexNotMatchException -> {
                    requireActivity().apply {
                        setResult(GlobalResult.RESULT_INDEX_NOT_MATCH)
                        finish()
                    }
                }

                Mk4IntroViewEvent.XfpNotMatchException -> {
                    showError(getString(R.string.nc_coldcard_xfp_does_not_match))
                }

                is Mk4IntroViewEvent.CheckExistingKey -> {
                    when (it.type) {
                        ResultExistingKey.Software -> NCInfoDialog(requireActivity())
                            .showDialog(
                                message = String.format(
                                    getString(R.string.nc_existing_key_is_software_key_delete_key),
                                    it.signer.masterFingerprint.uppercase(Locale.getDefault())
                                ),
                                btnYes = getString(R.string.nc_text_yes),
                                btnInfo = getString(R.string.nc_text_no),
                                onYesClick = {
                                    findNavController().navigate(
                                        Mk4IntroFragmentDirections.actionMk4IntroFragmentToAddMk4NameFragment(
                                            isReplaceKey = true,
                                            signer = it.signer
                                        )
                                    )
                                },
                                onInfoClick = {}
                            )

                        ResultExistingKey.Hardware -> {
                            NCInfoDialog(requireActivity())
                                .showDialog(
                                    message = String.format(
                                        getString(R.string.nc_existing_key_change_key_type),
                                        it.signer.masterFingerprint.uppercase(Locale.getDefault())
                                    ),
                                    btnYes = getString(R.string.nc_text_yes),
                                    btnInfo = getString(R.string.nc_text_no),
                                    onYesClick = {
                                        findNavController().navigate(
                                            Mk4IntroFragmentDirections.actionMk4IntroFragmentToAddMk4NameFragment(
                                                isReplaceKey = true,
                                                signer = it.signer
                                            )
                                        )
                                    },
                                    onInfoClick = {}
                                )
                        }

                        ResultExistingKey.None -> findNavController().navigate(
                            Mk4IntroFragmentDirections.actionMk4IntroFragmentToAddMk4NameFragment(
                                it.signer
                            )
                        )
                    }
                }

                Mk4IntroViewEvent.KeyVerifiedSuccess -> {
                    requireActivity().setResult(Activity.RESULT_OK)
                    navigator.openBackUpSeedPhraseActivity(
                        requireActivity(),
                        BackUpSeedPhraseArgs(
                            type = BackUpSeedPhraseType.SUCCESS,
                            signer = null,
                            groupId = (activity as Mk4Activity).groupId,
                            walletId = (activity as Mk4Activity).walletId.orEmpty()
                        )
                    )
                }
            }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4 }) {
            if (action.isParseAction) {
                viewModel.parseWalletFromMk4(it.records)
            } else {
                viewModel.importWalletFromMk4(it.records)
            }
            nfcViewModel.clearScanInfo()
        }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4 }) {
            viewModel.getWalletsFromColdCard(it.records)
            nfcViewModel.clearScanInfo()
        }
    }

    private fun parseWalletSuccess(wallet: Wallet?) {
        requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(GlobalResultKey.WALLET, wallet)
        })
        requireActivity().finish()
    }

    private fun showWallets(wallets: List<Wallet>) {
        BottomSheetOption.newInstance(wallets.mapIndexed { index, wallet ->
            SheetOption(
                type = index + WALLET_OFFSET,
                label = wallet.name,
                id = wallet.id,
            )
        }, title = getString(R.string.nc_sellect_wallet_type))
            .show(childFragmentManager, "BottomSheetOption")
    }

    private fun openRecoverWalletName(walletId: String) {
        navigator.openAddRecoverWalletScreen(
            requireActivity(), RecoverWalletData(
                type = RecoverWalletType.COLDCARD,
                walletId = walletId
            ),
            quickWalletParam = (requireActivity() as Mk4Activity).quickWalletParam
        )
        requireActivity().finish()
    }

    private fun onContinueClicked() {
        val mk4Activity = requireActivity() as Mk4Activity
        val type = when (action) {
            ColdcardAction.RECOVER_MULTI_SIG_WALLET,
            ColdcardAction.PARSE_MULTISIG_WALLET,
                -> BaseNfcActivity.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4

            ColdcardAction.RECOVER_SINGLE_SIG_WALLET,
            ColdcardAction.PARSE_SINGLE_SIG_WALLET,
                -> BaseNfcActivity.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4

            else -> BaseNfcActivity.REQUEST_MK4_ADD_KEY
        }
        val customHint = if (type == BaseNfcActivity.REQUEST_MK4_ADD_KEY) {
            mk4Activity.onChainAddSignerParam?.let {
                val accountIndex = it.keyIndex.takeIf { index -> index >= 0 } ?: 0
                getString(R.string.nc_hint_add_mk4_with_account, accountIndex)
            }
        } else {
            null
        }
        mk4Activity.setMk4HintOverride(customHint)
        (mk4Activity as NfcActionListener).startNfcFlow(type)
    }

    private fun openSignerSheet(signer: List<SingleSigner>) {
        if (signer.isNotEmpty()) {
            val fragment = BottomSheetOption.newInstance(signer.mapIndexed { index, singleSigner ->
                SheetOption(
                    type = index + SIGNER_OFFSET,
                    label = if (singleSigner.derivationPath.isTestNetSigner) {
                        "${singleSigner.derivationPath} (${getString(R.string.nc_testnet)})"
                    } else if (singleSigner.derivationPath.isRecommendedMultiSigPath) {
                        "${singleSigner.derivationPath} (${
                            getString(
                                R.string.nc_recommended_for_multisig
                            )
                        })"
                    } else if (singleSigner.derivationPath.isRecommendedSingleSigPath) {
                        "${singleSigner.derivationPath} (${
                            getString(
                                R.string.nc_recommended_for_single_sig
                            )
                        })"
                    } else {
                        singleSigner.derivationPath
                    }
                )
            }, title = getString(R.string.nc_mk4_signer_title))
            fragment.show(childFragmentManager, "BottomSheetOption")
        }
    }

    private val action: ColdcardAction
        get() = (requireActivity() as Mk4Activity).action

    companion object {
        private const val WALLET_OFFSET = 1000000
        private const val SIGNER_OFFSET = 1000
    }
}

@Composable
private fun Mk4IntroScreen(
    viewModel: Mk4IntroViewModel = viewModel(),
    isMembershipFlow: Boolean,
    isReplaceKey: Boolean,
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    Mk4IntroContent(
        remainTime = remainTime,
        isMembershipFlow = isMembershipFlow,
        onMoreClicked = onMoreClicked,
        isReplaceKey = isReplaceKey,
        onContinueClicked = viewModel::onContinueClicked
    )
}

@Composable
private fun Mk4IntroContent(
    remainTime: Int = 0,
    isMembershipFlow: Boolean = true,
    isReplaceKey: Boolean = false,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) =
    NunchukTheme {
        NunchukTheme {
            Scaffold(topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_coldcard_intro,
                    title = if (isMembershipFlow && !isReplaceKey && remainTime > 0) stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ) else "",
                    actions = {
                        if (isMembershipFlow && !isReplaceKey) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        }
                    }
                )
            }) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_coldcard_nfc_tip),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.nc_coldcard_nfc_intro_desc),
                        style = NunchukTheme.typography.body
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            }
        }
    }

@Preview
@Composable
private fun Mk4IntroScreenPreview() {
    Mk4IntroContent()
}