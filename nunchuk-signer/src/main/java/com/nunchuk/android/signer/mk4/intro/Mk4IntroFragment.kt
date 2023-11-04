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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
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
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
class Mk4IntroFragment : MembershipFragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<Mk4IntroViewModel>()
    private val args: Mk4IntroFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                Mk4IntroScreen(viewModel, args.isMembershipFlow, ::handleShowMore)
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
            viewModel.createWallet(option.id.orEmpty())
        } else if (option.type >= SIGNER_OFFSET) {
            val signer = viewModel.mk4Signers.getOrNull(option.type - SIGNER_OFFSET) ?: return
            findNavController().navigate(
                Mk4IntroFragmentDirections.actionMk4IntroFragmentToAddMk4NameFragment(
                    signer
                )
            )
        }
    }

    private fun observer() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_ADD_KEY }) {
            viewModel.getMk4Signer(it.records, (activity as Mk4Activity).groupId)
            nfcViewModel.clearScanInfo()
        }

        flowObserver(viewModel.event) {
            when (it) {
                is Mk4IntroViewEvent.LoadMk4SignersSuccess -> openSignerSheet(it.signers)
                is Mk4IntroViewEvent.Loading -> showOrHideLoading(it.isLoading)
                is Mk4IntroViewEvent.ShowError -> showError(it.message)
                Mk4IntroViewEvent.OnContinueClicked -> onContinueClicked()
                Mk4IntroViewEvent.OnCreateSignerSuccess -> requireActivity().finish()
                Mk4IntroViewEvent.OnSignerExistInAssistedWallet -> showError(getString(R.string.nc_error_add_same_key))
                Mk4IntroViewEvent.ErrorMk4TestNet -> NCInfoDialog(requireActivity())
                    .showDialog(
                        title = getString(R.string.nc_invalid_network),
                        message = getString(R.string.nc_error_device_in_testnet_msg)
                    )
                is Mk4IntroViewEvent.ImportWalletFromMk4Success -> openRecoverWalletName(it.walletId)
                is Mk4IntroViewEvent.ExtractWalletsFromColdCard -> showWallets(it.wallets)
                is Mk4IntroViewEvent.NfcLoading -> showOrHideNfcLoading(it.isLoading)
            }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4 }) {
            viewModel.importWalletFromMk4(it.records)
            nfcViewModel.clearScanInfo()
        }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4 }) {
            viewModel.getWalletsFromColdCard(it.records)
            nfcViewModel.clearScanInfo()
        }
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
            )
        )
        requireActivity().finish()
    }

    private fun onContinueClicked() {
       val type = when((requireActivity() as Mk4Activity).action) {
            ColdcardAction.RECOVER_MULTI_SIG_WALLET -> BaseNfcActivity.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
            ColdcardAction.RECOVER_SINGLE_SIG_WALLET -> BaseNfcActivity.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4
           else -> BaseNfcActivity.REQUEST_MK4_ADD_KEY
        }
        (requireActivity() as NfcActionListener).startNfcFlow(type)
    }

    private fun openSignerSheet(signer: List<SingleSigner>) {
        if (signer.isNotEmpty()) {
            val fragment = BottomSheetOption.newInstance(signer.mapIndexed { index, singleSigner ->
                SheetOption(
                    type = index + SIGNER_OFFSET, label = singleSigner.derivationPath
                )
            }, title = getString(R.string.nc_mk4_signer_title))
            fragment.show(childFragmentManager, "BottomSheetOption")
        }
    }

    companion object {
        private const val WALLET_OFFSET = 1000000
        private const val SIGNER_OFFSET = 1000
    }
}

@Composable
private fun Mk4IntroScreen(
    viewModel: Mk4IntroViewModel = viewModel(),
    isMembershipFlow: Boolean,
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    Mk4IntroContent(
        remainTime = remainTime,
        isMembershipFlow = isMembershipFlow,
        onMoreClicked = onMoreClicked,
        onContinueClicked = viewModel::onContinueClicked
    )
}

@Composable
private fun Mk4IntroContent(
    remainTime: Int = 0,
    isMembershipFlow: Boolean = true,
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {}
) =
    NunchukTheme {
        NunchukTheme {
            Scaffold { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    NcImageAppBar(
                        backgroundRes = R.drawable.nc_bg_coldcard_intro,
                        title = if (isMembershipFlow) stringResource(
                            id = R.string.nc_estimate_remain_time,
                            remainTime
                        ) else "",
                        actions = {
                            if (isMembershipFlow) {
                                IconButton(onClick = onMoreClicked) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_more),
                                        contentDescription = "More icon"
                                    )
                                }
                            }
                        }
                    )
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