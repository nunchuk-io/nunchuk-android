package com.nunchuk.android.main.membership.key

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.getButtonText
import com.nunchuk.android.main.membership.model.getLabel
import com.nunchuk.android.main.membership.model.resId
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddKeyListFragment : MembershipFragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by activityViewModels<AddKeyListViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AddKeyListScreen(viewModel, membershipStepManager)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                when (data.type) {
                    SignerType.NFC -> openCreateBackUpTapSigner(data.signers.first().id)
                    SignerType.AIRGAP,
                    SignerType.COLDCARD_NFC -> viewModel.onSelectedExistingHardwareSigner(data.signers.first())
                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.AIRGAP -> openAddAirgap()
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SignerType.NFC.ordinal -> handleShowKeysOrCreate(
                viewModel.getTapSigners(),
                SignerType.NFC,
                ::openSetupTapSigner
            )
            SignerType.COLDCARD_NFC.ordinal -> handleShowKeysOrCreate(
                viewModel.getColdcard(),
                SignerType.COLDCARD_NFC,
                ::showAddColdcardOptions
            )
            SignerType.AIRGAP.ordinal -> handleShowKeysOrCreate(
                viewModel.getAirgap(),
                SignerType.AIRGAP,
                ::openAddAirgap
            )
            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(requireActivity(), true)
            SheetOptionType.TYPE_ADD_COLDCARD_FILE -> navigator.openSetupMk4(
                requireActivity(),
                true,
                ColdcardAction.RECOVER
            )
        }
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
                    type = SheetOptionType.TYPE_ADD_COLDCARD_FILE,
                    label = getString(R.string.nc_add_coldcard_via_file),
                    resId = R.drawable.ic_import
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun openAddAirgap() {
        navigator.openAddAirSignerScreen(requireActivity(), true)
    }

    private fun observer() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is AddKeyListEvent.OnAddKey -> handleOnAddKey(event.data)
                AddKeyListEvent.OnAddSameKey -> showSameSignerAdded()
                is AddKeyListEvent.OnVerifySigner -> openVerifyTapSigner(event)
                AddKeyListEvent.OnAddAllKey -> findNavController().popBackStack()
            }
        }
    }

    private fun handleOnAddKey(data: AddKeyData) {
        when (data.type) {
            MembershipStep.ADD_TAP_SIGNER_1,
            MembershipStep.ADD_TAP_SIGNER_2 -> handleShowKeysOrCreate(
                viewModel.getTapSigners(),
                SignerType.NFC,
                ::openSetupTapSigner
            )
            MembershipStep.ADD_SEVER_KEY -> {
                findNavController().navigate(AddKeyListFragmentDirections.actionAddKeyListFragmentToConfigureServerKeyIntroFragment())
            }
            MembershipStep.HONEY_ADD_TAP_SIGNER -> {
                findNavController().navigate(AddKeyListFragmentDirections.actionAddKeyListFragmentToTapSignerInheritanceIntroFragment())
            }
            MembershipStep.SETUP_KEY_RECOVERY,
            MembershipStep.SETUP_INHERITANCE,
            MembershipStep.CREATE_WALLET -> throw IllegalArgumentException("handleOnAddKey")
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> openSelectHardwareOption()
        }
    }

    private fun openSelectHardwareOption() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SignerType.NFC.ordinal,
                    label = getString(R.string.nc_tapsigner)
                ),
                SheetOption(
                    type = SignerType.COLDCARD_NFC.ordinal,
                    label = getString(R.string.nc_coldcard)
                ),
                SheetOption(
                    type = SignerType.AIRGAP.ordinal,
                    label = getString(R.string.nc_signer_air_gapped)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun handleShowKeysOrCreate(
        signer: List<SignerModel>,
        type: SignerType,
        onEmptySigner: () -> Unit
    ) {
        if (signer.isNotEmpty()) {
            findNavController().navigate(
                AddKeyListFragmentDirections.actionAddKeyListFragmentToTapSignerListBottomSheetFragment(
                    signer.toTypedArray(),
                    type
                )
            )
        } else {
            onEmptySigner()
        }
    }

    private fun openVerifyTapSigner(event: AddKeyListEvent.OnVerifySigner) {
        navigator.openVerifyBackupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            masterSignerId = event.signer.id,
            backUpFilePath = event.filePath
        )
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true
        )
    }

    private fun openCreateBackUpTapSigner(masterSignerId: String) {
        if (viewModel.isSignerExist(masterSignerId).not()) {
            navigator.openCreateBackUpTapSigner(
                activity = requireActivity(),
                fromMembershipFlow = true,
                masterSignerId = masterSignerId
            )
        } else {
            showSameSignerAdded()
        }
    }

    private fun showSameSignerAdded() {
        showError(getString(R.string.nc_error_add_same_tap_signer))
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun AddKeyListScreen(
    viewModel: AddKeyListViewModel = viewModel(),
    membershipStepManager: MembershipStepManager
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    AddKeyListContent(
        onBackClicked = { onBackPressedDispatcher?.onBackPressed() },
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = viewModel::onAddKeyClicked,
        onVerifyClicked = viewModel::onVerifyClicked,
        keys = keys,
        remainingTime = remainingTime,
    )
}

@Composable
fun AddKeyListContent(
    onBackClicked: () -> Unit = {},
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    keys: List<AddKeyData> = emptyList(),
    remainingTime: Int,
) {
    NunchukTheme {
        Scaffold(modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
            topBar = {
                TopAppBar(title = {
                    Text(
                        text = stringResource(id = R.string.nc_estimate_remain_time, remainingTime),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }, navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back Icon"
                        )
                    }
                }, backgroundColor = MaterialTheme.colors.surface)
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_let_add_your_keys),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = buildAnnotatedString {
                        append(stringResource(id = R.string.nc_add_key_list_desc_one))
                        append(" ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                            append(stringResource(id = R.string.nc_add_key_list_desc_two))
                        }
                        append(stringResource(id = R.string.nc_add_key_list_desc_three))
                    },
                    style = NunchukTheme.typography.body
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(keys) { key ->
                        AddKeyCard(
                            item = key,
                            onAddClicked = onAddClicked,
                            onVerifyClicked = onVerifyClicked,
                        )
                    }
                }
                if (keys.all { it.isVerifyOrAddKey }) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_text_continue))
                    }
                }
            }
        }
    }
}

@Composable
fun AddKeyCard(
    item: AddKeyData,
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
) {
    if (item.signer != null) {
        Box(
            modifier = Modifier.background(
                color = if (item.isVerify)
                    colorResource(id = R.color.nc_green_color)
                else
                    colorResource(id = R.color.nc_beeswax_tint),
                shape = RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                NcCircleImage(
                    resId = item.type.resId,
                    color = colorResource(id = R.color.nc_white_color)
                )
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = item.type.getLabel(LocalContext.current),
                        style = NunchukTheme.typography.body
                    )
                    NcTag(
                        modifier = Modifier.padding(top = 4.dp),
                        label = stringResource(R.string.nc_verified_backup),
                        backgroundColor = colorResource(
                            id = R.color.nc_white_color
                        ),
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = "CardID: ...${item.signer.cardIdShorten()}",
                        style = NunchukTheme.typography.bodySmall
                    )
                }
                if (item.isVerify) {
                    Icon(
                        painter = painterResource(id = R.drawable.nc_circle_checked),
                        contentDescription = "Checked icon"
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        style = NunchukTheme.typography.body,
                        text = stringResource(
                            R.string.nc_added
                        )
                    )
                } else {
                    NcOutlineButton(
                        modifier = Modifier.height(36.dp),
                        onClick = { onVerifyClicked(item) }) {
                        Text(text = stringResource(R.string.nc_verify_backup))
                    }
                }
            }
        }
    } else {
        if (item.isVerify) {
            Box(
                modifier = Modifier.background(
                    colorResource(id = R.color.nc_green_color),
                    shape = RoundedCornerShape(8.dp)
                ),
                contentAlignment = Alignment.Center,
            ) {
                ConfigItem(item)
            }
        } else {
            NcDashLineBox {
                ConfigItem(item, onAddClicked)
            }
        }
    }
}

@Composable
private fun ConfigItem(
    item: AddKeyData,
    onAddClicked: ((data: AddKeyData) -> Unit)? = null
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcCircleImage(resId = item.type.resId)
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = item.type.getLabel(LocalContext.current),
            style = NunchukTheme.typography.body
        )
        Spacer(modifier = Modifier.weight(1.0f))
        if (onAddClicked != null) {
            NcOutlineButton(
                modifier = Modifier.height(36.dp),
                onClick = { onAddClicked(item) },
            ) {
                Text(
                    text = item.type.getButtonText(LocalContext.current),
                    style = NunchukTheme.typography.caption,
                )
            }
        } else {
            Icon(
                painter = painterResource(id = R.drawable.nc_circle_checked),
                contentDescription = "Checked icon"
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                style = NunchukTheme.typography.body,
                text = stringResource(R.string.nc_configured)
            )
        }
    }
}

@Preview
@Composable
fun AddKeyListScreenPreview() {
    AddKeyListContent(
        keys = listOf(
            AddKeyData(
                type = MembershipStep.ADD_TAP_SIGNER_1,
                SignerModel("123", "My Key", "", fingerPrint = "123456")
            ),
            AddKeyData(
                type = MembershipStep.ADD_TAP_SIGNER_2,
                signer = SignerModel("123", "My Key", "", fingerPrint = "123456"),
                isVerify = true
            ),
            AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
        ),
        remainingTime = 0,
    )
}