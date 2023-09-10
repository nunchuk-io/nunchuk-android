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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import androidx.compose.ui.tooling.preview.Preview
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
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.AssistedWalletBottomSheet
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.getButtonText
import com.nunchuk.android.main.membership.model.getLabel
import com.nunchuk.android.main.membership.model.resId
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.isByzantine
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections.emptyList
import javax.inject.Inject

@AndroidEntryPoint
class AddKeyListFragment : MembershipFragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by activityViewModels<AddKeyListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (membershipStepManager.assistedWallets.any { !it.plan.isByzantine() } && membershipStepManager.isNotConfig()) {
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_key_resuse),
                message = getString(R.string.nc_key_reuse_desc),
                onYesClick = {
                    AssistedWalletBottomSheet.show(
                        childFragmentManager,
                        membershipStepManager.assistedWallets.map { it.localId },
                    )
                }
            )
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
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                when (data.type) {
                    SignerType.NFC -> openCreateBackUpTapSigner(data.signers.first().id)
                    SignerType.AIRGAP,
                    SignerType.COLDCARD_NFC,
                    -> viewModel.onSelectedExistingHardwareSigner(data.signers.first())

                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.AIRGAP -> showAirgapOptions()
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
        childFragmentManager.setFragmentResultListener(
            AssistedWalletBottomSheet.TAG,
            viewLifecycleOwner
        ) { _, bundle ->
            val walletId = bundle.getString(GlobalResultKey.WALLET_ID).orEmpty()
            viewModel.reuseKeyFromWallet(walletId)
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

            SignerType.COLDCARD_NFC.ordinal -> handleShowKeysOrCreate(
                viewModel.getColdcard(),
                SignerType.COLDCARD_NFC,
                ::showAddColdcardOptions
            )

            SignerType.AIRGAP.ordinal -> handleShowKeysOrCreate(
                viewModel.getAirgap(),
                SignerType.AIRGAP,
                ::showAirgapOptions
            )

            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(requireActivity(), true)
            SheetOptionType.TYPE_ADD_COLDCARD_FILE -> navigator.openSetupMk4(
                requireActivity(),
                true,
                ColdcardAction.RECOVER_KEY
            )

            SheetOptionType.TYPE_ADD_AIRGAP_JADE,
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
            SheetOptionType.TYPE_ADD_AIRGAP_OTHER,
            -> handleSelectAddAirgapType(option.type)

            SheetOptionType.TYPE_ADD_LEDGER -> openRequestAddDesktopKey(SignerTag.LEDGER)
            SheetOptionType.TYPE_ADD_TREZOR -> openRequestAddDesktopKey(SignerTag.TREZOR)
            SheetOptionType.TYPE_ADD_COLDCARD_USB -> openRequestAddDesktopKey(SignerTag.COLDCARD)
            SheetOptionType.TYPE_ADD_BITBOX -> openRequestAddDesktopKey(SignerTag.BITBOX)
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

    private fun handleSelectAddAirgapType(type: Int) {
        val tag = when (type) {
            SheetOptionType.TYPE_ADD_AIRGAP_JADE -> SignerTag.JADE
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER -> SignerTag.SEEDSIGNER
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT -> SignerTag.PASSPORT
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE -> SignerTag.KEYSTONE
            else -> null
        }
        viewModel.getUpdateSigner()?.let {
            if (tag != null) {
                viewModel.onUpdateSignerTag(it, tag)
            }
        } ?: run {
            navigator.openAddAirSignerScreen(
                activityContext = requireActivity(),
                isMembershipFlow = true,
                tag = tag
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
                    type = SheetOptionType.TYPE_ADD_AIRGAP_OTHER,
                    label = getString(R.string.nc_other),
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun observer() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is AddKeyListEvent.OnAddKey -> handleOnAddKey(event.data)
                is AddKeyListEvent.OnVerifySigner -> openVerifyTapSigner(event)
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

            MembershipStep.HONEY_ADD_TAP_SIGNER -> {
                findNavController().navigate(AddKeyListFragmentDirections.actionAddKeyListFragmentToTapSignerInheritanceIntroFragment())
            }

            MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            MembershipStep.IRON_ADD_HARDWARE_KEY_2,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
            -> openSelectHardwareOption()

            else -> Unit
        }
    }

    private val groupId: String
        get() = (activity as MembershipActivity).groupId

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
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_LEDGER,
                    label = getString(R.string.nc_ledger)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_TREZOR,
                    label = getString(R.string.nc_trezor)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_BITBOX,
                    label = getString(R.string.nc_bitbox)
                ),
            ),
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
        )
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
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
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    AddKeyListContent(
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = viewModel::onAddKeyClicked,
        onVerifyClicked = viewModel::onVerifyClicked,
        keys = keys,
        remainingTime = remainingTime,
        onMoreClicked = onMoreClicked
    )
}

@Composable
fun AddKeyListContent(
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    keys: List<AddKeyData> = emptyList(),
    remainingTime: Int,
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                NcTopAppBar(
                    elevation = 0.dp,
                    title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    })
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
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
                                    append(stringResource(id = R.string.nc_honey_add_key_list_desc_three))
                                } else {
                                    append(stringResource(id = R.string.nc_add_key_list_desc_three))
                                }
                                if (keys.size > 3) {
                                    append("\n\n")
                                    append(stringResource(R.string.nc_among_three_key_select_inheritance))
                                }
                            },
                            style = NunchukTheme.typography.body
                        )
                    }

                    items(keys) { key ->
                        AddKeyCard(
                            item = key,
                            onAddClicked = onAddClicked,
                            onVerifyClicked = onVerifyClicked,
                        )
                    }
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                    enabled = keys.all { it.isVerifyOrAddKey }
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
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
                color = if (item.verifyType != VerifyType.NONE)
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
                    resId = item.signer.toReadableDrawableResId(),
                    color = colorResource(id = R.color.nc_white_color)
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
                    if (item.signer.type == SignerType.NFC) {
                        val label = when (item.verifyType) {
                            VerifyType.NONE -> stringResource(id = R.string.nc_un_verify_backup)
                            VerifyType.APP_VERIFIED -> stringResource(id = R.string.nc_verified_backup)
                            VerifyType.SELF_VERIFIED -> stringResource(id = R.string.nc_self_verified_backup)
                        }
                        NcTag(
                            modifier = Modifier.padding(top = 4.dp),
                            label = label,
                            backgroundColor = colorResource(
                                id = R.color.nc_whisper_color
                            ),
                        )
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
        if (item.verifyType != VerifyType.NONE) {
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
    onAddClicked: ((data: AddKeyData) -> Unit)? = null,
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
                text = item.type.getLabel(LocalContext.current),
                style = NunchukTheme.typography.body
            )
            if (item.type == MembershipStep.HONEY_ADD_TAP_SIGNER) {
                NcTag(
                    modifier = Modifier.padding(top = 4.dp),
                    label = stringResource(R.string.nc_inheritance),
                    backgroundColor = colorResource(
                        id = R.color.nc_whisper_color
                    ),
                )
            }
        }
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
fun AddKeyListScreenIronHandPreview() {
    AddKeyListContent(
        keys = listOf(
            AddKeyData(
                type = MembershipStep.IRON_ADD_HARDWARE_KEY_1,
                SignerModel(
                    id = "123",
                    type = SignerType.NFC,
                    name = "My Key",
                    derivationPath = "",
                    fingerPrint = "123456"
                ),
                verifyType = VerifyType.APP_VERIFIED
            ),
            AddKeyData(
                type = MembershipStep.IRON_ADD_HARDWARE_KEY_2,
                signer = SignerModel(
                    id = "123",
                    type = SignerType.NFC,
                    name = "My Key",
                    derivationPath = "",
                    fingerPrint = "123456"
                ),
                verifyType = VerifyType.NONE
            ),
            AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
        ),
        remainingTime = 0,
    )
}

@Preview
@Composable
fun AddKeyListScreenHoneyBadgerPreview() {
    AddKeyListContent(
        keys = listOf(
            AddKeyData(
                type = MembershipStep.HONEY_ADD_TAP_SIGNER,
                verifyType = VerifyType.NONE
            ),
            AddKeyData(
                type = MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                signer = SignerModel(
                    id = "123",
                    type = SignerType.COLDCARD_NFC,
                    name = "TAPSIGNER",
                    derivationPath = "",
                    fingerPrint = "123456"
                ),
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