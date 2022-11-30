package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownconfirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyLockdownConfirmFragment : Fragment() {
    private val viewModel: EmergencyLockdownConfirmViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LockdownConfirmScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when(event) {
                is LockdownConfirmEvent.ContinueClick -> {
                    findNavController().navigate(EmergencyLockdownConfirmFragmentDirections.actionLockdownConfirmFragmentToLockdownSuccessFragment())
                }
                is LockdownConfirmEvent.Loading -> TODO()
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun LockdownConfirmScreen(
    viewModel: EmergencyLockdownConfirmViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LockdownConfirmContent(
        state.signers,
        onContinueClicked = viewModel::onContinueClicked
    )
}

@Composable
private fun LockdownConfirmContent(
    signers: List<SignerModel> = emptyList(),
    onSignerSelected: (signer: SignerModel) -> Unit = {},
    selectedSignerId: String = "",
    onContinueClicked: () -> Unit = {}
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
                text = stringResource(R.string.nc_emergency_lockdown_confirm_title),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(R.string.nc_emergency_lockdown_confirm_desc),
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

@Composable
private fun SignerCard(
    signer: SignerModel,
    isSelected: Boolean,
    onSignerSelected: (signer: SignerModel) -> Unit = {},
) {
    Row(
        modifier = Modifier.clickable {  onSignerSelected(signer) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcCircleImage(resId = R.drawable.ic_nfc_card)
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
                        id = R.color.nc_grey_dark_color
                    )
                ),
            )
            NcTag(
                modifier = Modifier
                    .padding(top = 6.dp),
                label = stringResource(id = R.string.nc_nfc),
            )
        }
        NcPrimaryDarkButton(
            modifier = Modifier
                .padding(16.dp),
            onClick = { onSignerSelected(signer) },
        ) {
            Text(text = stringResource(id = R.string.nc_sign))
        }
    }
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreenPreview() {
    NunchukTheme {
        LockdownConfirmContent(
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