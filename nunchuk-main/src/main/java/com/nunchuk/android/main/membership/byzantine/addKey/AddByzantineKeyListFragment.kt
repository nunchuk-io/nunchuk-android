/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.main.membership.byzantine.addKey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.AssistedWalletBottomSheet
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.key.AddKeyCard
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections.emptyList
import javax.inject.Inject

@AndroidEntryPoint
class AddByzantineKeyListFragment : MembershipFragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<AddByzantineKeyListViewModel>()

    private val args: AddByzantineKeyListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
                    SignerType.COLDCARD_NFC -> viewModel.onSelectedExistingHardwareSigner(data.signers.first())

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

            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(
                activity = requireActivity(),
                fromMembershipFlow = true, groupId = args.groupId)
            SheetOptionType.TYPE_ADD_COLDCARD_FILE -> navigator.openSetupMk4(
                activity = requireActivity(),
                fromMembershipFlow = true,
                action = ColdcardAction.RECOVER_KEY,
                groupId = args.groupId
            )

            SheetOptionType.TYPE_ADD_AIRGAP_JADE,
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
            SheetOptionType.TYPE_ADD_AIRGAP_OTHER -> handleSelectAddAirgapType(option.type)
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
                tag = tag,
                groupId = args.groupId
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

    private fun showAirgapOptions() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_what_type_of_airgap_you_have),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
                    label = getString(R.string.nc_keystone),
                ),
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
                navigator.openConfigServerKeyActivity(activityContext = requireActivity(), groupStep = MembershipStage.NONE)
            }

            MembershipStep.BYZANTINE_ADD_TAP_SIGNER -> {
                findNavController().navigate(AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToTapSignerInheritanceIntroFragment())
            }

            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2 -> openSelectHardwareOption()

            else -> Unit
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
            ),
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
            showClosedIcon = true,
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun handleShowKeysOrCreate(
        signer: List<SignerModel>,
        type: SignerType,
        onEmptySigner: () -> Unit
    ) {
        if (signer.isNotEmpty()) {
            findNavController().navigate(
                AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToTapSignerListBottomSheetFragment(
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
            groupId = (activity as MembershipActivity).groupId
        )
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            groupId = (activity as MembershipActivity).groupId,
        )
    }

    private fun openCreateBackUpTapSigner(masterSignerId: String) {
        navigator.openCreateBackUpTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            masterSignerId = masterSignerId,
            groupId = (activity as MembershipActivity).groupId
        )
    }
}

@Composable
fun AddKeyListScreen(
    viewModel: AddByzantineKeyListViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    onMoreClicked: () -> Unit = {}
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    AddKeyListContent(
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = viewModel::onAddKeyClicked,
        onVerifyClicked = viewModel::onVerifyClicked,
        keys = keys,
        remainingTime = remainingTime,
        onMoreClicked = onMoreClicked,
        refresh = viewModel::refresh,
        isRefreshing = state.isRefreshing
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddKeyListContent(
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    refresh: () -> Unit = { },
    onMoreClicked: () -> Unit = {},
    keys: List<AddKeyData> = emptyList(),
    remainingTime: Int,
    isRefreshing: Boolean = false,
) {
    val state = rememberPullRefreshState(isRefreshing, refresh)
    NunchukTheme {
        Scaffold(modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(), topBar = {
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
        }, bottomBar = {
            if (keys.all { it.isVerifyOrAddKey }) {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }) { innerPadding ->
            Box(Modifier.pullRefresh(state)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.nc_let_add_your_keys),
                            style = NunchukTheme.typography.heading
                        )
                        NcSpannedText(
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            text = stringResource(R.string.nc_byzantine_add_key_desc),
                            baseStyle = NunchukTheme.typography.body,
                            styles = mapOf(SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold))
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

                PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
            }
        }
    }
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