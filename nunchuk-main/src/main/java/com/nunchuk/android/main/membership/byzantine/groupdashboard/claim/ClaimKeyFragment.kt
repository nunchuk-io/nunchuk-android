package com.nunchuk.android.main.membership.byzantine.groupdashboard.claim

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ClaimKeyFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ClaimKeyViewModel by viewModels()
    private val args: ClaimKeyFragmentArgs by navArgs()

    private val signLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                showSuccess(getString(R.string.nc_the_key_has_been_claimed))
                findNavController().navigate(
                    ClaimKeyFragmentDirections.actionClaimKeyFragmentToWalletConfigIntroFragment(
                        true
                    )
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ClaimKeyScreen(
                    viewModel = viewModel,
                    onSelectHardwareSigner = {
                        NCInfoDialog(requireActivity()).showDialog(
                            message = getString(R.string.nc_select_hardware_signer_desc),
                            btnInfo = getString(R.string.nc_get_the_desktop_app),
                            onInfoClick = {
                                requireActivity().openExternalLink("https://nunchuk.io")
                            }
                        )
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is ClaimKeyEvent.GetHealthCheckPayload -> {
                            hideLoading()
                            navigator.openWalletAuthentication(
                                activityContext = requireActivity(),
                                walletId = args.walletId,
                                requiredSignatures = event.payload.requiredSignatures,
                                type = VerificationType.SIGN_DUMMY_TX,
                                groupId = args.groupId,
                                dummyTransactionId = event.payload.dummyTransactionId,
                                launcher = signLauncher,
                                action = TargetAction.CLAIM_KEY.name
                            )
                        }

                        is ClaimKeyEvent.Loading -> showOrHideLoading(event.isLoading)
                    }
                }
        }
    }
}

@Composable
private fun ClaimKeyScreen(
    viewModel: ClaimKeyViewModel = viewModel(),
    onSelectHardwareSigner: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClaimKeyContent(
        signers = state.signers,
        onClaimKey = viewModel::onHealthCheck,
        onSelectHardwareSigner = onSelectHardwareSigner
    )
}

@Composable
private fun ClaimKeyContent(
    signers: List<SignerModel> = emptyList(),
    onClaimKey: (signer: SignerModel) -> Unit = {},
    onSelectHardwareSigner: () -> Unit = {},
) = NunchukTheme {
    var selectedSigner by rememberSaveable {
        mutableStateOf<SignerModel?>(null)
    }
    Scaffold(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            NcTopAppBar(title = "", isBack = false)
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = { (onClaimKey(selectedSigner!!)) }, enabled = selectedSigner != null
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_claim_your_key),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(R.string.nc_claim_your_key_desc),
                style = NunchukTheme.typography.body,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(signers) { signer ->
                    SignerCard(
                        signer = signer,
                        isSelected = selectedSigner == signer,
                        onSignerSelected = {
                            if (signer.type == SignerType.HARDWARE) {
                                onSelectHardwareSigner()
                            } else {
                                selectedSigner = signer
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SignerCard(
    signer: SignerModel,
    onSignerSelected: (signer: SignerModel) -> Unit = {},
    isSelected: Boolean = false,
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
        RadioButton(selected = isSelected, onClick = { onSignerSelected(signer) })
    }
}

@Preview
@Composable
private fun ClaimKeyScreenPreview() {
    ClaimKeyContent(
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