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

package com.nunchuk.android.main.membership.authentication.message

import android.app.Activity
import android.content.Intent
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.core.domain.data.SignTransaction
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.BasePortalActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationEvent
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationViewModel
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.widget.NCInputDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CheckSignMessageFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val walletAuthenticationViewModel: WalletAuthenticationViewModel by activityViewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                CheckSignMessageScreen(walletAuthenticationViewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            walletAuthenticationViewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is WalletAuthenticationEvent.SignDummyTxSuccess -> {
                            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(
                                    GlobalResultKey.SIGNATURE_EXTRA,
                                    HashMap(event.signatures)
                                )
                            })
                            requireActivity().finish()
                        }

                        is WalletAuthenticationEvent.ScanTapSigner -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                        )

                        WalletAuthenticationEvent.ScanColdCard -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG
                        )

                        is WalletAuthenticationEvent.ProcessFailure -> showError(event.message)
                        WalletAuthenticationEvent.GenerateColdcardHealthMessagesSuccess -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE
                        )

                        is WalletAuthenticationEvent.NfcLoading -> showOrHideNfcLoading(
                            event.isLoading,
                            event.isColdCard
                        )

                        WalletAuthenticationEvent.ShowAirgapOption -> Unit
                        WalletAuthenticationEvent.ExportTransactionToColdcardSuccess -> Unit
                        WalletAuthenticationEvent.CanNotSignDummyTx -> showError(getString(R.string.nc_can_not_sign_please_try_again))
                        WalletAuthenticationEvent.CanNotSignHardwareKey -> showError("Please use the desktop app to sign with this key")
                        WalletAuthenticationEvent.PromptPassphrase -> NCInputDialog(requireActivity()).showDialog(
                            title = getString(com.nunchuk.android.transaction.R.string.nc_transaction_enter_passphrase),
                            onConfirmed = {
                                walletAuthenticationViewModel.handlePassphrase(it)
                            }
                        )
                        is WalletAuthenticationEvent.RequestSignPortal -> {
                            (activity as BasePortalActivity<*>).handlePortalAction(
                               SignTransaction(
                                   fingerPrint = event.fingerprint,
                                   psbt = event.psbt
                                )
                            )
                        }
                        is WalletAuthenticationEvent.ForceSyncSuccess,
                        is WalletAuthenticationEvent.Loading,
                        is WalletAuthenticationEvent.FinalizeDummyTxSuccess,
                        is WalletAuthenticationEvent.ShowError,
                        is WalletAuthenticationEvent.SignFailed,
                        is WalletAuthenticationEvent.UploadSignatureSuccess,
                        is WalletAuthenticationEvent.NoInternetConnectionToSign,
                        is WalletAuthenticationEvent.NoInternetConnectionForceSync,
                        is WalletAuthenticationEvent.NoSignatureDetected,
                        -> Unit
                    }
                }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }) { info ->
            walletAuthenticationViewModel.getInteractSingleSigner()?.let {
                walletAuthenticationViewModel.handleTapSignerSignCheckMessage(
                    it,
                    info,
                    nfcViewModel.inputCvc.orEmpty()
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG }) { scanInfo ->
            walletAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                walletAuthenticationViewModel.generateColdcardHealthMessages(
                    Ndef.get(scanInfo.tag) ?: return@flowObserver,
                    signer.derivationPath
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE }) {
            walletAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                walletAuthenticationViewModel.healthCheckColdCard(signer, it.records)
            }
            nfcViewModel.clearScanInfo()
        }
    }
}


@Composable
private fun CheckSignMessageScreen(
    viewModel: WalletAuthenticationViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CheckSignMessageContent(
        state.walletSigner,
        onSignerSelected = {
            viewModel.onSignerSelect(it)
        }
    )
}

@Composable
private fun CheckSignMessageContent(
    signers: List<SignerModel> = emptyList(),
    onSignerSelected: (signer: SignerModel) -> Unit = {},
) = NunchukTheme {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            NcTopAppBar(title = "")
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_finalize_changes),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(R.string.nc_finalize_changes_desc),
                style = NunchukTheme.typography.body,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(signers) { signer ->
                    SignerCard(signer, onSignerSelected)
                }
            }
        }
    }
}

@Composable
private fun SignerCard(
    signer: SignerModel,
    onSignerSelected: (signer: SignerModel) -> Unit = {},
) {
    Row(
        modifier = Modifier.clickable { onSignerSelected(signer) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcCircleImage(resId = signer.toReadableDrawableResId())
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.0f)
        ) {
            Text(text = signer.name, style = NunchukTheme.typography.body)
            NcTag(
                modifier = Modifier
                    .padding(top = 4.dp),
                label = stringResource(id = R.string.nc_nfc),
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = signer.getXfpOrCardIdLabel(),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = colorResource(
                        id = R.color.nc_grey_dark_color
                    )
                ),
            )
        }
        NcPrimaryDarkButton(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            height = 44.dp,
            onClick = { onSignerSelected(signer) },
        ) {
            Text(text = stringResource(id = R.string.nc_sign))
        }
    }
}

@Preview
@Composable
private fun CheckSignMessageScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    NunchukTheme {
        CheckSignMessageContent(signers = signers)
    }
}