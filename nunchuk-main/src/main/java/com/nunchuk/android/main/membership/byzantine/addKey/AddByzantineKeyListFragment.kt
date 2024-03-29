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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isAirgapTag
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.AssistedWalletBottomSheet
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragmentFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddByzantineKeyListFragment : MembershipFragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<AddByzantineKeyListViewModel>()

    private val args: AddByzantineKeyListFragmentArgs by navArgs()

    private var selectedSignerTag: SignerTag? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AddKeyListScreen(
                    viewModel = viewModel,
                    isAddOnly = args.isAddOnly,
                    membershipStepManager = membershipStepManager,
                    onMoreClicked = ::handleShowMore
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                val signer = data.signers.first()
                when (signer.type) {
                    SignerType.NFC -> {
                        findNavController().navigate(
                            AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                                signer
                            )
                        )
                    }

                    SignerType.AIRGAP -> {
                        val hasTag = signer.tags.any { it.isAirgapTag || it == SignerTag.COLDCARD }
                        if (hasTag) {
                            findNavController().navigate(
                                AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                                    signer
                                )
                            )
                        } else {
                            viewModel.requestAddAirgapTag(signer)
                        }
                    }

                    SignerType.COLDCARD_NFC -> {
                        findNavController().navigate(
                            AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                                signer
                            )
                        )
                    }

                    SignerType.HARDWARE -> {
                        findNavController().navigate(
                            AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                                signer
                            )
                        )
                    }

                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.AIRGAP -> showAirgapOptions()
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    SignerType.HARDWARE -> selectedSignerTag?.let { tag ->
                        openRequestAddDesktopKey(tag)
                    }

                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
        setFragmentResultListener(CustomKeyAccountFragmentFragment.REQUEST_KEY) { _, bundle ->
            val signer = bundle.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)
            if (signer != null) {
                viewModel.handleSignerNewIndex(signer)
            }
            clearFragmentResult(CustomKeyAccountFragmentFragment.REQUEST_KEY)
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
                viewModel.getColdcard() + viewModel.getHardwareSigners(SignerTag.COLDCARD),
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
                fromMembershipFlow = true,
                groupId = args.groupId
            )

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
                AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToAddDesktopKeyFragment(
                    tag,
                    step,
                    args.groupId
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

                is AddKeyListEvent.UpdateSignerTag -> findNavController().navigate(
                    AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                        event.signer
                    )
                )
            }
        }
        flowObserver(viewModel.state) {
            if (it.shouldShowKeyAdded) {
                findNavController().navigate(
                    AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToKeyAddedToGroupWalletFragment()
                )
                viewModel.markHandledShowKeyAdded()
            }
        }
    }

    private fun handleOnAddKey(data: AddKeyData) {
        when (data.type) {
            MembershipStep.ADD_SEVER_KEY -> {
                navigator.openConfigGroupServerKeyActivity(
                    activityContext = requireActivity(),
                    groupStep = MembershipStage.NONE,
                    groupId = args.groupId
                )
            }

            MembershipStep.BYZANTINE_ADD_TAP_SIGNER, MembershipStep.BYZANTINE_ADD_TAP_SIGNER_1 -> {
                findNavController().navigate(AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToTapSignerInheritanceIntroFragment())
            }

            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4 -> openSelectHardwareOption()

            else -> Unit
        }
    }

    private fun openSelectHardwareOption() {
        val options = if (args.isKeyHolderLimited) {
            listOf(
                SheetOption(
                    type = SignerType.NFC.ordinal,
                    label = getString(R.string.nc_tapsigner)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_LEDGER,
                    label = getString(R.string.nc_ledger)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_ADD_TREZOR,
                    label = getString(R.string.nc_trezor)
                ),
            )
        } else {
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
            )
        }
        BottomSheetOption.newInstance(
            options = options,
            desc = getString(R.string.nc_key_limit_desc).takeIf { args.isKeyHolderLimited },
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
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
}

@Composable
fun AddKeyListScreen(
    viewModel: AddByzantineKeyListViewModel = viewModel(),
    isAddOnly: Boolean = false,
    membershipStepManager: MembershipStepManager,
    onMoreClicked: () -> Unit = {}
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    AddByzantineKeyListContent(
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = viewModel::onAddKeyClicked,
        onVerifyClicked = viewModel::onVerifyClicked,
        keys = keys,
        remainingTime = remainingTime,
        onMoreClicked = onMoreClicked,
        refresh = viewModel::refresh,
        isRefreshing = state.isRefreshing,
        isAddOnly = isAddOnly,
        groupWalletType = state.groupWalletType
    )
}