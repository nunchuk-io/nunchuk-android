package com.nunchuk.android.main.membership.authentication.message

import android.app.Activity
import android.content.Intent
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationEvent
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationViewModel
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
class CheckSignMessageFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val walletAuthenticationViewModel: WalletAuthenticationViewModel by activityViewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CheckSignMessageScreen(walletAuthenticationViewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            walletAuthenticationViewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is WalletAuthenticationEvent.WalletAuthenticationSuccess -> {
                            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(
                                    GlobalResultKey.SIGNATURE_EXTRA,
                                    event.signatures
                                )
                            })
                            requireActivity().finish()
                        }
                        is WalletAuthenticationEvent.Loading -> showOrHideLoading(event.isLoading)
                        is WalletAuthenticationEvent.ScanTapSigner -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                        )
                        WalletAuthenticationEvent.ScanColdCard -> (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG)
                        is WalletAuthenticationEvent.ProcessFailure -> showError(event.message)
                        WalletAuthenticationEvent.GenerateColdcardHealthMessagesSuccess -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE
                        )
                        is WalletAuthenticationEvent.NfcLoading -> showOrHideNfcLoading(event.isLoading, event.isColdCard)
                        is WalletAuthenticationEvent.ShowError -> showError(event.message)
                        WalletAuthenticationEvent.ShowAirgapOption -> {}
                    }
                }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }) { info ->
            walletAuthenticationViewModel.getInteractSingleSigner()?.let {
                walletAuthenticationViewModel.handleTapSignerSignCheckMessage(it, info, nfcViewModel.inputCvc.orEmpty())
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG }) { scanInfo ->
            walletAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                walletAuthenticationViewModel.generateColdcardHealthMessages(
                    Ndef.get(scanInfo.tag),
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


@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun CheckSignMessageScreen(
    viewModel: WalletAuthenticationViewModel = viewModel()
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
        NcCircleImage(resId = signer.type.toReadableDrawableResId())
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
private fun CheckSignMessageScreenPreview() {
    NunchukTheme {
        CheckSignMessageContent(
            signers = listOf(
                SignerModel(
                    "123", "Tom’s TAPSIGNER", fingerPrint = "79EB35F4", derivationPath = ""
                ),
                SignerModel(
                    "123", "Tom’s TAPSIGNER 2", fingerPrint = "79EB35F4", derivationPath = ""
                ),
            )
        )
    }
}