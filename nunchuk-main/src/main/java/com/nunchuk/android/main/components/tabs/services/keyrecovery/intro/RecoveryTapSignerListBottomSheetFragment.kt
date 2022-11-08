package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.base.BaseComposeBottomSheet
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecoveryTapSignerListBottomSheetFragment : BaseComposeBottomSheet() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<RecoveryTapSignerListBottomSheetViewModel>()
    private val args: RecoveryTapSignerListBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NunchukTheme {
                    RecoveryTapSignerListScreen(viewModel, args)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                RecoveryTapSignerListBottomSheetEvent.ContinueClick -> {
                    findNavController().navigate(
                        RecoveryTapSignerListBottomSheetFragmentDirections.actionRecoverTapSignerListBottomSheetFragmentToAnswerSecurityQuestionFragment(viewModel.selectedSigner!!, args.verifyToken)
                    )
                }
            }
            dismissAllowingStateLoss()
        }
    }

    companion object {
        const val REQUEST_KEY = "RecoveryTapSignerListBottomSheetFragment"
        const val EXTRA_SELECTED_SIGNER_ID = "EXTRA_SELECTED_SIGNER_ID"
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun RecoveryTapSignerListScreen(
    viewModel: RecoveryTapSignerListBottomSheetViewModel,
    args: RecoveryTapSignerListBottomSheetFragmentArgs,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    RecoveryTapSignerListContent(
        signers = args.signers.toList(),
        selectedSignerId = state.selectedSignerId.orEmpty(),
        onSignerSelected = viewModel::onSignerSelected,
        onCloseClick = {
            onBackPressedDispatcher?.onBackPressed()
        },
        onContinueClick = viewModel::onContinueClicked
    )
}

@Composable
private fun RecoveryTapSignerListContent(
    signers: List<SignerModel> = emptyList(),
    selectedSignerId: String = "",
    onSignerSelected: (signer: SignerModel) -> Unit = {},
    onCloseClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier.background(
            color = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ).padding(bottom = 21.dp)
    ) {
        IconButton(
            modifier = Modifier.padding(top = 28.dp), onClick = onCloseClick
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Back Icon"
            )
        }
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = stringResource(R.string.nc_select_tapsigner_recover),
            style = NunchukTheme.typography.title,
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(signers) { signer ->
                SignerCard(signer, signer.id == selectedSignerId, onSignerSelected)
            }
        }
        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = onContinueClick,
            enabled = selectedSignerId.isNotEmpty(),
        ) {
            Text(
                text = stringResource(R.string.nc_text_continue),
            )
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
        NcCircleImage(resId = R.drawable.ic_nfc_card)
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1.0f)
        ) {
            Text(text = signer.name, style = NunchukTheme.typography.body)
            NcTag(
                modifier = Modifier
                    .padding(top = 6.dp),
                label = stringResource(id = R.string.nc_signer_type_air_gapped),
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "XFP: ${signer.fingerPrint}",
                style = NunchukTheme.typography.bodySmall.copy(
                    color = colorResource(
                        id = R.color.nc_grey_dark_color
                    )
                ),
            )
        }
        Checkbox(checked = isSelected, onCheckedChange = {
            onSignerSelected(signer)
        })
    }
}

@Preview
@Composable
fun RecoveryTapSignerListContentPreview() {
    NunchukTheme {
        RecoveryTapSignerListContent(
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