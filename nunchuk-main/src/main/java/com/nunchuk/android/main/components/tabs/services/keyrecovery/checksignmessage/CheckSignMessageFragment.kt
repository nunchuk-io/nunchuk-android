package com.nunchuk.android.main.components.tabs.services.keyrecovery.checksignmessage

import android.app.Activity
import android.content.Intent
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
class CheckSignMessageFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: CheckSignMessageViewModel by viewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                CheckSignMessageScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is CheckSignMessageEvent.CheckSignMessageSuccess -> {
                            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(CheckSignMessageActivity.SIGNATURE_EXTRA, event.signatures)
                            })
                            requireActivity().finish()
                        }
                        is CheckSignMessageEvent.ContinueClick -> {

                        }
                        is CheckSignMessageEvent.GetSignersSuccess -> {

                        }
                        is CheckSignMessageEvent.Loading -> showOrHideLoading(event.isLoading)
                        is CheckSignMessageEvent.OpenScanDataTapsigner -> {
                            (requireActivity() as CheckSignMessageActivity).startNfcFlow(
                                BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                            )
                        }
                        is CheckSignMessageEvent.ProcessFailure -> showError(event.message)
                    }
                }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }) { info ->
            viewModel.getInteractSingleSigner()?.let {
                viewModel.handleSignCheckMessage(it, info, nfcViewModel.inputCvc.orEmpty())
            }
        }
    }
}


@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun CheckSignMessageScreen(
    viewModel: CheckSignMessageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CheckSignMessageContent(
        state.signerModels,
        onSignerSelected = {
            viewModel.onSignerSelect(it)
        }
    )
}

@Composable
private fun CheckSignMessageContent(
    signers: List<SignerModel> = emptyList(),
    onSignerSelected: (signer: SignerModel) -> Unit = {},
    selectedSignerId: String = "",
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
                text = stringResource(com.nunchuk.android.signer.R.string.nc_finalize_changes),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(com.nunchuk.android.signer.R.string.nc_finalize_changes_desc),
                style = NunchukTheme.typography.body,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(signers) { signer ->
                    SignerCard(signer, signer.id == selectedSignerId, onSignerSelected)
                }
            }
        }
    }
}

@Composable
private fun SignerCard(
    signer: SignerModel,
    isSelected: Boolean,
    onSignerSelected: (signer: SignerModel) -> Unit = {},
) {
    Row(
        modifier = Modifier.clickable { onSignerSelected(signer) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcCircleImage(resId = com.nunchuk.android.signer.R.drawable.ic_nfc_card)
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.0f)
        ) {
            Text(text = signer.name, style = NunchukTheme.typography.body)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "XFP: ${signer.fingerPrint}",
                style = NunchukTheme.typography.bodySmall.copy(
                    color = colorResource(
                        id = com.nunchuk.android.signer.R.color.nc_grey_dark_color
                    )
                ),
            )
            NcTag(
                modifier = Modifier
                    .padding(top = 6.dp),
                label = stringResource(id = com.nunchuk.android.signer.R.string.nc_nfc),
            )
        }
        NcPrimaryDarkButton(
            modifier = Modifier
                .padding(16.dp),
            onClick = { onSignerSelected(signer) },
        ) {
            Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_sign))
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