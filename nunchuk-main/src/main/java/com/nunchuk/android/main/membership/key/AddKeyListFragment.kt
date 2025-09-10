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

package com.nunchuk.android.main.membership.key

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.pullrefresh.PullRefreshIndicator
import com.nunchuk.android.compose.pullrefresh.pullRefresh
import com.nunchuk.android.compose.pullrefresh.rememberPullRefreshState
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.byzantine.addKey.getKeyOptions
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.getButtonText
import com.nunchuk.android.main.membership.model.getLabel
import com.nunchuk.android.main.membership.model.resId
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections.emptyList

@AndroidEntryPoint
class AddKeyListFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel by activityViewModels<AddKeyListViewModel>()

    private var selectedSignerTag: SignerTag? = null

    private val addPortalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.onSelectedExistingHardwareSigner(it)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AddKeyListScreen(viewModel, membershipStepManager, ::handleShowMore)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        setFragmentResultListener(CustomKeyAccountFragment.REQUEST_KEY) { _, bundle ->
            val signer = bundle.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)
            if (signer != null) {
                viewModel.onSelectedExistingHardwareSigner(signer)
            }
            clearFragmentResult(CustomKeyAccountFragment.REQUEST_KEY)
        }
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                when (data.type) {
                    SignerType.NFC -> openCreateBackUpTapSigner(data.signers.first().id)
                    SignerType.PORTAL_NFC -> findNavController().navigate(
                        AddKeyListFragmentDirections.actionAddKeyListFragmentToCustomKeyAccountFragmentFragment(
                            data.signers.first(),
                            walletId = (activity as MembershipActivity).walletId,
                        )
                    )

                    else -> {
                        val signer = data.signers.first()
                        val selectedSignerTag = selectedSignerTag
                        if (signer.type == SignerType.AIRGAP && signer.tags.isEmpty() && selectedSignerTag != null) {
                            viewModel.onUpdateSignerTag(signer, selectedSignerTag)
                        } else {
                            viewModel.onSelectedExistingHardwareSigner(signer.toSingleSigner())
                        }
                    }
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.PORTAL_NFC -> openSetupPortal()
                    SignerType.AIRGAP -> handleSelectAddAirgapType(selectedSignerTag)
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    SignerType.HARDWARE -> selectedSignerTag?.let { openRequestAddDesktopKey(it) }
                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SignerType.NFC.ordinal -> handleShowKeysOrCreate(
                viewModel.getTapSigners(),
                SignerType.NFC,
                ::openSetupTapSigner
            )

            SignerType.PORTAL_NFC.ordinal -> {
                handleShowKeysOrCreate(
                    viewModel.getPortal(),
                    SignerType.PORTAL_NFC,
                    ::openSetupPortal
                )
            }

            SignerType.COLDCARD_NFC.ordinal -> {
                selectedSignerTag = SignerTag.COLDCARD
                handleShowKeysOrCreate(
                    viewModel.getColdcard(),
                    SignerType.COLDCARD_NFC,
                    ::showAddColdcardOptions
                )
            }

            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(requireActivity(), true)
            SheetOptionType.TYPE_ADD_COLDCARD_QR,
            SheetOptionType.TYPE_ADD_COLDCARD_FILE,
                -> navigator.openSetupMk4(
                requireActivity(),
                true,
                ColdcardAction.RECOVER_KEY,
                isScanQRCode = option.type == SheetOptionType.TYPE_ADD_COLDCARD_QR
            )

            SheetOptionType.TYPE_ADD_AIRGAP_JADE,
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
            SheetOptionType.TYPE_ADD_AIRGAP_OTHER,
                -> {
                selectedSignerTag = getSignerTag(option.type)
                handleShowKeysOrCreate(
                    viewModel.getAirgap(getSignerTag(option.type)),
                    SignerType.AIRGAP
                ) { handleSelectAddAirgapType(selectedSignerTag) }
            }

            SheetOptionType.TYPE_ADD_LEDGER -> {
                selectedSignerTag = SignerTag.LEDGER
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.LEDGER),
                    SignerType.HARDWARE
                ) { openRequestAddDesktopKey(SignerTag.LEDGER) }
            }

            SheetOptionType.TYPE_ADD_TREZOR -> {
                selectedSignerTag = SignerTag.TREZOR
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.TREZOR),
                    SignerType.HARDWARE
                ) { openRequestAddDesktopKey(SignerTag.TREZOR) }
            }

            SheetOptionType.TYPE_ADD_COLDCARD_USB -> openRequestAddDesktopKey(SignerTag.COLDCARD)
            SheetOptionType.TYPE_ADD_BITBOX -> {
                selectedSignerTag = SignerTag.BITBOX
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.BITBOX),
                    SignerType.HARDWARE
                ) { openRequestAddDesktopKey(SignerTag.BITBOX) }
            }
        }
    }

    private fun openRequestAddDesktopKey(tag: SignerTag) {
        membershipStepManager.currentStep?.let { step ->
            findNavController().navigate(
                AddKeyListFragmentDirections.actionAddKeyListFragmentToAddDesktopKeyFragment(
                    tag,
                    step
                )
            )
        }
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            isMembershipFlow = true,
            tag = tag,
            step = membershipStepManager.currentStep
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

    private fun getSignerTag(type: Int): SignerTag? {
        return when (type) {
            SheetOptionType.TYPE_ADD_AIRGAP_JADE -> SignerTag.JADE
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER -> SignerTag.SEEDSIGNER
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT -> SignerTag.PASSPORT
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE -> SignerTag.KEYSTONE
            else -> null
        }
    }

    private fun showAirgapOptions() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_what_type_of_airgap_you_have),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_JADE,
                    label = getString(R.string.nc_blockstream_jade),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
                    label = getString(R.string.nc_foudation_passport),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
                    label = getString(R.string.nc_seedsigner),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
                    label = getString(R.string.nc_keystone),
                ),
                SheetOption(
                    type = SignerType.COLDCARD_NFC.ordinal,
                    label = getString(R.string.nc_coldcard)
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun observer() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is AddKeyListEvent.OnAddKey -> handleOnAddKey(event.data)
                is AddKeyListEvent.OnVerifySigner -> {
                    if (event.signer.type == SignerType.NFC) {
                        openVerifyTapSigner(event)
                    } else {
                        openVerifyColdCard(event)
                    }
                }

                AddKeyListEvent.OnAddAllKey -> findNavController().popBackStack()
                is AddKeyListEvent.ShowError -> showError(event.message)
                AddKeyListEvent.SelectAirgapType -> showAirgapOptions()
            }
        }
    }

    private fun handleOnAddKey(data: AddKeyData) {
        when (data.type) {
            MembershipStep.ADD_SEVER_KEY -> {
                navigator.openConfigServerKeyActivity(
                    activityContext = requireActivity(),
                    groupStep = MembershipStage.NONE
                )
            }

            MembershipStep.HONEY_ADD_INHERITANCE_KEY -> {
                findNavController().navigate(AddKeyListFragmentDirections.actionAddKeyListFragmentToInheritanceKeyIntroFragment())
            }

            MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            MembershipStep.IRON_ADD_HARDWARE_KEY_2,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
                -> openSelectHardwareOption()

            else -> Unit
        }
    }

    private fun openSelectHardwareOption() {
        val options = getKeyOptions(
            context = requireContext(),
            isKeyHolderLimited = false,
            isStandard = false,
        )
        BottomSheetOption.newInstance(
            options = options,
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
            showClosedIcon = true,
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun handleShowKeysOrCreate(
        signer: List<SignerModel>,
        type: SignerType,
        onEmptySigner: () -> Unit,
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
            backUpFilePath = event.filePath,
            masterSignerId = event.signer.id,
            walletId = (activity as MembershipActivity).walletId,
        )
    }

    private fun openVerifyColdCard(event: AddKeyListEvent.OnVerifySigner) {
        navigator.openSetupMk4(
            activity = requireActivity(),
            fromMembershipFlow = true,
            backUpFilePath = event.filePath,
            xfp = event.signer.fingerPrint,
            action = if (event.backUpFileName.isNotEmpty()) ColdcardAction.VERIFY_KEY else ColdcardAction.UPLOAD_BACKUP,
            keyName = event.signer.name,
            signerType = event.signer.type,
            backUpFileName = event.backUpFileName,
        )
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
        )
    }

    private fun openSetupPortal() {
        navigator.openPortalScreen(
            launcher = addPortalLauncher,
            activity = requireActivity(),
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = true
            ),
        )
    }

    private fun openCreateBackUpTapSigner(masterSignerId: String) {
        navigator.openCreateBackUpTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            masterSignerId = masterSignerId,
        )
    }
}

@Composable
fun AddKeyListScreen(
    viewModel: AddKeyListViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    onMoreClicked: () -> Unit = {},
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    AddKeyListContent(
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = viewModel::onAddKeyClicked,
        onVerifyClicked = viewModel::onVerifyClicked,
        keys = keys,
        remainingTime = remainingTime,
        onMoreClicked = onMoreClicked,
        refresh = viewModel::refresh,
        isRefreshing = uiState.isRefresh,
        missingBackupKeys = uiState.missingBackupKeys,
    )
}

@Composable
fun AddKeyListContent(
    isRefreshing: Boolean = false,
    remainingTime: Int,
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    keys: List<AddKeyData> = emptyList(),
    missingBackupKeys: List<AddKeyData> = emptyList(),
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    onAddClicked: (data: AddKeyData) -> Unit = {},
    refresh: () -> Unit = { },
) {
    val state = rememberPullRefreshState(isRefreshing, refresh)

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    },
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                    enabled = keys.all { it.isVerifyOrAddKey } && missingBackupKeys.isEmpty()
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pullRefresh(state)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.nc_let_add_your_keys),
                            style = NunchukTheme.typography.heading
                        )
                        Text(
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            text = buildAnnotatedString {
                                append(
                                    stringResource(
                                        id = R.string.nc_add_key_list_desc_one,
                                        keys.size
                                    )
                                )
                                append(" ")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.W700)) {
                                    append(stringResource(id = R.string.nc_add_key_list_desc_two))
                                }

                                if (keys.size > 3) {
                                    append("\n\n")
                                    append(stringResource(R.string.nc_among_three_key_select_inheritance))
                                }

                                append("\n\nPull to refresh the key statuses.")
                            },
                            style = NunchukTheme.typography.body
                        )
                    }

                    items(keys) { key ->
                        AddKeyCard(
                            item = key,
                            onAddClicked = onAddClicked,
                            onVerifyClicked = onVerifyClicked,
                            isMissingBackup = missingBackupKeys.contains(key) && key.signer?.type != SignerType.NFC
                        )
                    }
                }

                PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
fun AddKeyCard(
    item: AddKeyData,
    modifier: Modifier = Modifier,
    isMissingBackup: Boolean = false,
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    isDisabled: Boolean = false,
    isStandard: Boolean = false
) {
    if (item.signer != null) {
        Box(
            modifier = modifier.background(
                color = if (item.verifyType != VerifyType.NONE) {
                    colorResource(id = R.color.nc_fill_slime)
                } else if (isDisabled) {
                    colorResource(id = R.color.nc_grey_dark_color)
                } else {
                    colorResource(id = R.color.nc_fill_beewax)
                },
                shape = RoundedCornerShape(8.dp)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                NcCircleImage(
                    resId = item.signer.toReadableDrawableResId(),
                )
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = item.signer.name,
                        style = NunchukTheme.typography.body
                    )
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        NcTag(
                            label = item.signer.toReadableSignerType(context = LocalContext.current),
                            backgroundColor = colorResource(
                                id = R.color.nc_bg_mid_gray
                            ),
                        )
                        if (item.signer.isShowAcctX()) {
                            NcTag(
                                modifier = Modifier.padding(start = 4.dp),
                                label = stringResource(R.string.nc_acct_x, item.signer.index),
                                backgroundColor = colorResource(
                                    id = R.color.nc_bg_mid_gray
                                ),
                            )
                        }
                    }
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = item.signer.getXfpOrCardIdLabel(),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
                if (item.verifyType != VerifyType.NONE) {
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
                } else if (item.signer.isVisible) {
                    NcOutlineButton(
                        modifier = Modifier.height(36.dp),
                        onClick = { onVerifyClicked(item) },
                    ) {
                        Text(
                            text = if (isMissingBackup.not()) stringResource(R.string.nc_verify_backup) else stringResource(
                                R.string.nc_upload_backup
                            )
                        )
                    }
                }
            }
        }
    } else {
        if (item.verifyType != VerifyType.NONE) {
            Box(
                modifier = modifier.background(
                    colorResource(id = R.color.nc_fill_slime),
                    shape = RoundedCornerShape(8.dp)
                ),
                contentAlignment = Alignment.Center,
            ) {
                ConfigItem(item, isDisabled = isDisabled)
            }
        } else {
            NcDashLineBox(modifier = modifier) {
                ConfigItem(item = item, onAddClicked = onAddClicked, isDisabled = isDisabled, isStandard = isStandard)
            }
        }
    }
}

@Composable
private fun ConfigItem(
    item: AddKeyData,
    onAddClicked: ((data: AddKeyData) -> Unit)? = null,
    isDisabled: Boolean = false,
    isStandard: Boolean = false
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcCircleImage(resId = item.type.resId)
        Column(
            modifier = Modifier
                .weight(1.0f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = item.type.getLabel(context = LocalContext.current, isStandard = isStandard),
                style = NunchukTheme.typography.body
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                if (item.type.isAddInheritanceKey) {
                    NcTag(
                        label = stringResource(R.string.nc_inheritance),
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                }
                if (item.signer?.isShowAcctX() == true) {
                    NcTag(
                        modifier = Modifier.padding(start = if (item.type == MembershipStep.HONEY_ADD_INHERITANCE_KEY) 4.dp else 0.dp),
                        label = stringResource(R.string.nc_acct_x, item.signer.index),
                        backgroundColor = colorResource(
                            id = R.color.nc_bg_mid_gray
                        ),
                    )
                }
            }
        }
        if (onAddClicked != null) {
            NcOutlineButton(
                modifier = Modifier.height(36.dp),
                enabled = isDisabled.not(),
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

@PreviewLightDark
@Composable
fun AddKeyListScreenIronHandPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    AddKeyListContent(
        keys = listOf(
            AddKeyData(
                type = MembershipStep.IRON_ADD_HARDWARE_KEY_1,
                signer = signer,
                verifyType = VerifyType.APP_VERIFIED
            ),
            AddKeyData(
                type = MembershipStep.IRON_ADD_HARDWARE_KEY_2,
                signer = signer,
                verifyType = VerifyType.NONE
            ),
            AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
        ),
        remainingTime = 0,
    )
}

@PreviewLightDark
@Composable
fun AddKeyListScreenHoneyBadgerPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    AddKeyListContent(
        keys = listOf(
            AddKeyData(
                type = MembershipStep.HONEY_ADD_INHERITANCE_KEY,
                verifyType = VerifyType.NONE
            ),
            AddKeyData(
                type = MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                signer = signer,
                verifyType = VerifyType.NONE
            ),
            AddKeyData(
                type = MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
            ),
            AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
        ),
        remainingTime = 0,
    )
}