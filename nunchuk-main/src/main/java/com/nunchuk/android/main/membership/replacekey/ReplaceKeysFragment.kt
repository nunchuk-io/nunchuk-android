package com.nunchuk.android.main.membership.replacekey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.byzantine.addKey.getKeyOptions
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReplaceKeysFragment : Fragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ReplaceKeysViewModel by viewModels()

    private val args by navArgs<ReplaceKeysFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ReplaceKeysScreen(
                    onReplaceKeyClicked = { signer ->
                        viewModel.setReplacingXfp(signer.fingerPrint)
                        openSelectHardwareOption()
                    },
                    onCreateNewWalletSuccess = { walletId ->
                        findNavController().navigate(
                            ReplaceKeysFragmentDirections.actionReplaceKeysFragmentToCreateWalletSuccessFragment(
                                walletId = walletId,
                                isReplaceWallet = true
                            )
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getReplaceWalletStatus()
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type != SheetOptionType.TYPE_ADD_COLDCARD_NFC) {
            viewModel.initReplaceKey()
        }
        when (option.type) {
            SignerType.NFC.ordinal -> openSetupTapSigner()

            SignerType.COLDCARD_NFC.ordinal -> showAddColdcardOptions()

            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(
                activity = requireActivity(),
                fromMembershipFlow = true,
                groupId = args.groupId
            )

            SheetOptionType.TYPE_ADD_COLDCARD_QR,
            SheetOptionType.TYPE_ADD_COLDCARD_FILE,
            -> navigator.openSetupMk4(
                activity = requireActivity(),
                fromMembershipFlow = true,
                action = ColdcardAction.RECOVER_KEY,
                groupId = args.groupId,
                isScanQRCode = option.type == SheetOptionType.TYPE_ADD_COLDCARD_QR
            )

            SheetOptionType.TYPE_ADD_AIRGAP_JADE -> handleSelectAddAirgapType(SignerTag.JADE)
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER -> handleSelectAddAirgapType(SignerTag.SEEDSIGNER)
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT -> handleSelectAddAirgapType(SignerTag.PASSPORT)
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE -> handleSelectAddAirgapType(SignerTag.KEYSTONE)
            SheetOptionType.TYPE_ADD_AIRGAP_OTHER -> handleSelectAddAirgapType(null)

            SheetOptionType.TYPE_ADD_LEDGER -> TODO()

            SheetOptionType.TYPE_ADD_TREZOR -> TODO()

            SheetOptionType.TYPE_ADD_COLDCARD_USB -> TODO()
            SheetOptionType.TYPE_ADD_BITBOX -> TODO()

            SheetOptionType.TYPE_ADD_SOFTWARE_KEY -> openAddSoftwareKey()

            else -> Unit
        }
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            isMembershipFlow = true,
            tag = tag,
            groupId = args.groupId
        )
    }

    private fun openAddSoftwareKey() {
        navigator.openAddSoftwareSignerScreen(
            activityContext = requireActivity(),
            groupId = args.groupId
        )
    }

    private fun showAddColdcardOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_NFC,
                    label = getString(R.string.nc_add_coldcard_via_nfc),
                    resId = R.drawable.ic_nfc_indicator_small
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_QR,
                    label = getString(R.string.nc_add_coldcard_via_qr),
                    resId = R.drawable.ic_qr
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_USB,
                    label = getString(R.string.nc_add_coldcard_via_usb),
                    resId = R.drawable.ic_usb
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_COLDCARD_FILE,
                    label = getString(R.string.nc_add_coldcard_via_file),
                    resId = R.drawable.ic_import
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            groupId = (activity as MembershipActivity).groupId,
            replacedXfp = viewModel.replacedXfp,
            walletId = args.walletId
        )
    }

    private fun openSelectHardwareOption() {
        val isKeyHolderLimited =
            viewModel.uiState.value.myRole == AssistedWalletRole.KEYHOLDER_LIMITED
        val isStandard =
            viewModel.uiState.value.group?.walletConfig?.toGroupWalletType()?.isStandard == true

        val options = getKeyOptions(
            context = requireContext(),
            isKeyHolderLimited = isKeyHolderLimited,
            isStandard = isStandard
        )
        BottomSheetOption.newInstance(
            options = options,
            desc = getString(R.string.nc_key_limit_desc).takeIf { isKeyHolderLimited },
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
        ).show(childFragmentManager, "BottomSheetOption")
    }
}

@Composable
private fun ReplaceKeysScreen(
    viewModel: ReplaceKeysViewModel = hiltViewModel(),
    onReplaceKeyClicked: (SignerModel) -> Unit = {},
    onCreateNewWalletSuccess: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.createWalletSuccess) {
        if (uiState.createWalletSuccess is StateEvent.String) {
            onCreateNewWalletSuccess((uiState.createWalletSuccess as StateEvent.String).data)
            viewModel.markOnCreateWalletSuccess()
        }
    }

    ReplaceKeysContent(
        uiState = uiState,
        onReplaceKeyClicked = onReplaceKeyClicked,
        onCreateWalletClicked = viewModel::onCreateWallet,
    )
}

@Composable
private fun ReplaceKeysContent(
    uiState: ReplaceKeysUiState = ReplaceKeysUiState(),
    onReplaceKeyClicked: (SignerModel) -> Unit = {},
    onCreateWalletClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onCreateWalletClicked,
                    enabled = uiState.replaceSigners.isNotEmpty()
                ) {
                    Text(text = stringResource(R.string.nc_continue_to_create_a_new_wallet))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.nc_which_key_would_you_like_to_replace),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_replace_one_or_multiple_keys),
                    style = NunchukTheme.typography.body
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.signers) { item ->
                        ReplaceKeyCard(
                            modifier = Modifier.padding(top = 16.dp),
                            item = uiState.replaceSigners[item.fingerPrint] ?: item,
                            onReplaceClicked = { onReplaceKeyClicked(it) },
                            isReplaced = uiState.replaceSigners.containsKey(item.fingerPrint)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReplaceKeyCard(
    item: SignerModel,
    modifier: Modifier = Modifier,
    isReplaced: Boolean = false,
    onReplaceClicked: (data: SignerModel) -> Unit = {},
    onVerifyClicked: (data: SignerModel) -> Unit = {},
) {
    Box(
        modifier = modifier.background(
            color = colorResource(id = R.color.nc_beeswax_tint),
            shape = RoundedCornerShape(8.dp)
        ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            NcCircleImage(
                resId = item.toReadableDrawableResId(),
                color = colorResource(id = R.color.nc_white_color)
            )
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.name,
                    style = NunchukTheme.typography.body
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    NcTag(
                        label = item.toReadableSignerType(context = LocalContext.current),
                        backgroundColor = colorResource(
                            id = R.color.nc_whisper_color
                        ),
                    )
                    if (item.isShowAcctX()) {
                        NcTag(
                            modifier = Modifier.padding(start = 4.dp),
                            label = stringResource(R.string.nc_acct_x, item.index),
                            backgroundColor = colorResource(
                                id = R.color.nc_whisper_color
                            ),
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = item.getXfpOrCardIdLabel(),
                    style = NunchukTheme.typography.bodySmall
                )
            }
            if (isReplaced) {
                if (item.type == SignerType.NFC)
                    NcOutlineButton(
                        modifier = Modifier.height(36.dp),
                        onClick = { onVerifyClicked(item) },
                    ) {
                        Text(text = stringResource(R.string.nc_verify_backup))
                    }
            } else {
                NcOutlineButton(
                    modifier = Modifier.height(36.dp),
                    onClick = { onReplaceClicked(item) },
                ) {
                    Text(text = stringResource(R.string.nc_replace))
                }
            }
        }
    }
}

@Composable
@Preview
private fun ReplaceKeysContentPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    ReplaceKeysContent(
        uiState = ReplaceKeysUiState(signers = signers)
    )
}