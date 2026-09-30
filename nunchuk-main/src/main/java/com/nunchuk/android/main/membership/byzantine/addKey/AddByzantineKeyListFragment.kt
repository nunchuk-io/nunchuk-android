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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.bold
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceKeyAdded
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSeedPhraseBackup
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSeedPhraseVerified
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSharingMethod
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceVerifyBackups
import com.nunchuk.android.main.membership.honey.distribution.verifyClaimOptionRequest
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isAirgapTag
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.backupVendorTag
import com.nunchuk.android.main.membership.model.opensVerifyBackups
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.bitbox.BitBoxActivity
import com.nunchuk.android.signer.ledger.LedgerActivity
import com.nunchuk.android.signer.trezor.TrezorActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddByzantineKeyListFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel by viewModels<AddByzantineKeyListViewModel>()

    private val args: AddByzantineKeyListFragmentArgs by navArgs()

    private var selectedSignerTag: SignerTag? = null


    private val membershipWalletId: String get() = (activity as MembershipActivity).walletId

    /**
     * The confirm-and-choose-sharing-method flow. Refreshes on the way back so the key row reflects the choice; a
     * cancelled run leaves the key without one and the row keeps offering it.
     */
    // The type is spelled out because the callback launches this same launcher again, which
    // otherwise makes inference recurse.
    private val keyDistributionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.refresh()
            val (option, signer) = result.data?.verifyClaimOptionRequest()
                ?: return@registerForActivityResult
            when (option) {
                ClaimOption.SEED_PHRASE -> openInheritanceSeedPhraseBackup(
                    navigator = navigator,
                    signer = signer,
                    groupId = args.groupId,
                    walletId = membershipWalletId,
                    launcher = verifySeedPhraseBackupLauncher,
                )

                ClaimOption.ENCRYPTED_BACKUP -> viewModel.key.value
                    .firstOrNull { it.signer?.fingerPrint == signer.fingerPrint }
                    ?.let { viewModel.onVerifyClicked(it, claimOption = option) }
            }
        }

    /**
     * Tail of the seed-phrase branch. Ledger and BitBox re-read the restored device and hand back
     * the fingerprint they saw, which is what proves the backup; Coldcard and air-gap finish
     * inside their own screens and come back empty, so the refresh is all this does for them.
     */
    private val verifySeedPhraseBackupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val verifiedXfp = result.data?.getStringExtra(GlobalResultKey.EXTRA_VERIFIED_XFP)
            if (result.resultCode == Activity.RESULT_OK && !verifiedXfp.isNullOrEmpty()) {
                viewModel.onSeedPhraseBackupVerified(
                    masterSignerId = verifiedXfp,
                    verifiedSigner = result.data?.parcelable(GlobalResultKey.EXTRA_SIGNER),
                )
            } else {
                viewModel.refresh()
            }
        }

    /**
     * The off-chain inheritance key type picker, preceded by the inheritance intro and the
     * passphrase notice. It runs every key type's own flow itself and hands the key back; only
     * Ledger, Trezor and BitBox come back as a tag, because their in-app pairing belongs here.
     */
    private val inheritanceKeyPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode != Activity.RESULT_OK || data == null) return@registerForActivityResult
            // Every other key type is added inside the picker and comes back as a signer.
            data.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)?.let { signer ->
                // Raised here rather than left to the save below: the picker's own flows
                // (Coldcard, air-gap) have already registered the key, so re-syncing it can
                // legitimately be a no-op while the sharing-method choice is still owed.
                viewModel.onInheritanceKeyAdded(signer.fingerPrint)
                // A TAPSIGNER is a master signer: the key for the wallet still has to be derived
                // from it, which the rest cannot do.
                if (signer.type == SignerType.NFC) {
                    viewModel.addExistingTapSignerKey(signer)
                } else {
                    viewModel.handleSignerNewIndex(signer.toSingleSigner())
                }
                return@registerForActivityResult
            }
            // Ledger, Trezor and BitBox are handed back as a tag: the picker only records which
            // device was chosen, the in-app pairing (or desktop hand-off) belongs to this screen.
            (data.getSerializableExtra(GlobalResultKey.EXTRA_SIGNER_TAG) as? SignerTag)?.let { tag ->
                selectedSignerTag = tag
                openInAppHardwareOrDesktopFlow(tag)
                return@registerForActivityResult
            }
        }

    private val isKeyHolderLimited: Boolean by lazy { args.role.toRole == AssistedWalletRole.KEYHOLDER_LIMITED }

    private val addPortalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.handleSignerNewIndex(it)
                }
            }
        }

    private val addTrezorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.handleSignerNewIndex(it)
                    return@registerForActivityResult
                }
                if (data.getStringExtra(TrezorActivity.EXTRA_RESULT_ACTION) == TrezorActivity.RESULT_ACTION_OPEN_USB_FLOW) {
                    openRequestAddDesktopKey(SignerTag.TREZOR)
                }
            }
        }

    private val addLedgerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.handleSignerNewIndex(it)
                    return@registerForActivityResult
                }
                if (data.getStringExtra(LedgerActivity.EXTRA_RESULT_ACTION) == LedgerActivity.RESULT_ACTION_OPEN_DESKTOP_FLOW) {
                    openRequestAddDesktopKey(SignerTag.LEDGER)
                }
            }
        }

    private val addBitBoxLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.handleSignerNewIndex(it)
                    return@registerForActivityResult
                }
                if (data.getStringExtra(BitBoxActivity.EXTRA_RESULT_ACTION) == BitBoxActivity.RESULT_ACTION_OPEN_DESKTOP_FLOW) {
                    openRequestAddDesktopKey(SignerTag.BITBOX)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AddByzantineKeyListScreen(
                    viewModel = viewModel,
                    isAddOnly = args.isAddOnly,
                    membershipStepManager = membershipStepManager,
                    role = args.role.toRole,
                    onMoreClicked = ::handleShowMore,
                    onSetUpClaimOptionsClicked = ::openSharingMethod,
                    onInheritanceBackupClicked = ::openInheritanceBackup,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        // Deriving the wallet's key from a TAPSIGNER can need the card read again; the host
        // activity owns the NFC session.
        val membershipActivity = activity as? MembershipActivity
        membershipActivity?.setTapSignerCachingCallback { isoDep, cvc ->
            viewModel.onTapSignerCardTapped(isoDep, cvc)
        }
        flowObserver(
            viewModel.state
                .map { it.requestTapSignerCardTap }
                .distinctUntilChanged()
        ) { isRequested ->
            if (isRequested) {
                membershipActivity?.requestTapSignerCaching()
                viewModel.onTapSignerCardTapHandled()
            }
        }
        // A StateFlow, not an event: the key is saved while the add-key screen is still on top, so
        // a one-shot event would be dropped by this stopped fragment and the owner would have to
        // resume the app by hand to see the screen.
        flowObserver(
            viewModel.state
                .map { it.pendingClaimOptionsSigner }
                .distinctUntilChanged()
        ) { signer ->
            if (signer != null) {
                viewModel.onClaimOptionsPromptHandled()
                openInheritanceKeyAdded(
                    signer = signer,
                    groupId = args.groupId,
                    launcher = keyDistributionLauncher,
                )
            }
        }
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                val signer = data.signers.first()
                when (signer.type) {
                    SignerType.AIRGAP -> {
                        val hasTag = signer.tags.any { it.isAirgapTag || it == SignerTag.COLDCARD }
                        val selectedSignerTag = selectedSignerTag
                        if (hasTag || selectedSignerTag == null) {
                            findNavController().navigate(
                                AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                                    signer,
                                    groupId = args.groupId
                                )
                            )
                        } else {
                            viewModel.onUpdateSignerTag(signer, selectedSignerTag)
                        }
                    }

                    else -> {
                        if (signer.type == SignerType.SOFTWARE && viewModel.isUnBackedUpSigner(signer)) {
                            showUnBackedUpSignerWarning()
                        } else {
                            findNavController().navigate(
                                AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                                    signer,
                                    groupId = args.groupId
                                )
                            )
                        }
                    }
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.AIRGAP -> handleSelectAddAirgapType(selectedSignerTag)
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    SignerType.HARDWARE -> selectedSignerTag?.let { tag ->
                        openInAppHardwareOrDesktopFlow(tag)
                    }

                    SignerType.PORTAL_NFC -> openSetupPortal()
                    SignerType.SOFTWARE -> openAddSoftwareKey()

                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
        setFragmentResultListener(CustomKeyAccountFragment.REQUEST_KEY) { _, bundle ->
            val signer = bundle.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)
            if (signer != null) {
                viewModel.handleSignerNewIndex(signer)
            }
            clearFragmentResult(CustomKeyAccountFragment.REQUEST_KEY)
        }

        if (args.role.toRole.isFacilitatorAdmin) {
            showFacilitatorInfoDialog()
        }
    }

    override fun onDestroyView() {
        // The callback outlives this fragment otherwise, and would tap into a dead view model.
        (activity as? MembershipActivity)?.clearTapSignerCachingCallback()
        super.onDestroyView()
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
                    viewModel.getPortalSigners(),
                    SignerType.PORTAL_NFC,
                    ::openSetupPortal
                )
            }

            SignerType.COLDCARD_NFC.ordinal -> {
                selectedSignerTag = SignerTag.COLDCARD
                handleShowKeysOrCreate(
                    viewModel.getColdcard() + viewModel.getAirgap(SignerTag.COLDCARD),
                    SignerType.COLDCARD_NFC,
                    ::showAddColdcardOptions
                )
            }

            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(
                activity = requireActivity(),
                args = SetupMk4Args(
                    fromMembershipFlow = true,
                    groupId = args.groupId
                )
            )

            SheetOptionType.TYPE_ADD_COLDCARD_QR,
            SheetOptionType.TYPE_ADD_COLDCARD_FILE,
            -> navigator.openSetupMk4(
                activity = requireActivity(),
                args = SetupMk4Args(
                    fromMembershipFlow = true,
                    action = ColdcardAction.RECOVER_KEY,
                    groupId = args.groupId,
                    isScanQRCode = option.type == SheetOptionType.TYPE_ADD_COLDCARD_QR
                )
            )

            SheetOptionType.TYPE_ADD_AIRGAP_JADE,
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
            SheetOptionType.TYPE_ADD_AIRGAP_KRUX,
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
                ) { openLedgerFlow() }
            }

            SheetOptionType.TYPE_ADD_TREZOR -> {
                selectedSignerTag = SignerTag.TREZOR
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.TREZOR),
                    SignerType.HARDWARE
                ) { openTrezorFlow() }
            }

            SheetOptionType.TYPE_ADD_COLDCARD_USB -> openRequestAddDesktopKey(SignerTag.COLDCARD)
            SheetOptionType.TYPE_ADD_BITBOX -> {
                selectedSignerTag = SignerTag.BITBOX
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.BITBOX),
                    SignerType.HARDWARE
                ) { openBitBoxFlow() }
            }

            SheetOptionType.TYPE_ADD_SOFTWARE_KEY ->
                checkTwoSoftwareKeySameDevice {
                    handleShowKeysOrCreate(
                        viewModel.getSoftwareSigners(),
                        SignerType.SOFTWARE
                    ) { openAddSoftwareKey() }
                }
        }
    }

    private fun showFacilitatorInfoDialog() {
        NCInfoDialog(requireActivity())
            .showDialog(message = getString(R.string.nc_info_facilitator))
    }

    private fun checkTwoSoftwareKeySameDevice(onSuccess: () -> Unit) {
        val total = viewModel.getCountWalletSoftwareSignersInDevice()
        if (total >= 1) {
            NCInfoDialog(requireActivity())
                .showDialog(
                    title = getString(R.string.nc_text_warning),
                    btnYes = getString(R.string.nc_text_continue),
                    message = SpannableStringBuilder()
                        .bold {
                            append(getString(R.string.nc_info_software_key_same_device_part_1))
                        }
                        .append(" ")
                        .append(getString(R.string.nc_info_software_key_same_device_part_2)),
                    onYesClick = { onSuccess() },
                    btnInfo = getString(R.string.nc_i_ll_choose_another_type_of_key),
                )
        } else {
            onSuccess()
        }
    }

    private fun openAddSoftwareKey() {
        navigator.openAddSoftwareSignerScreen(
            activityContext = requireActivity(),
            groupId = args.groupId,
        )
    }

    private fun getSignerTag(type: Int): SignerTag? {
        return when (type) {
            SheetOptionType.TYPE_ADD_AIRGAP_JADE -> SignerTag.JADE
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER -> SignerTag.SEEDSIGNER
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT -> SignerTag.PASSPORT
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE -> SignerTag.KEYSTONE
            SheetOptionType.TYPE_ADD_AIRGAP_KRUX -> SignerTag.KRUX
            else -> null
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

    /**
     * Trezor, Ledger and BitBox have in-app add-key flows; every other hardware key
     * (COLDCARD over USB) is desktop-only.
     */
    private fun openInAppHardwareOrDesktopFlow(tag: SignerTag) {
        when (tag) {
            SignerTag.TREZOR -> openTrezorFlow()
            SignerTag.LEDGER -> openLedgerFlow()
            SignerTag.BITBOX -> openBitBoxFlow()
            else -> openRequestAddDesktopKey(tag)
        }
    }

    private fun openTrezorFlow() {
        addTrezorLauncher.launch(
            TrezorActivity.buildIntent(
                activityContext = requireActivity(),
                isMembershipFlow = true
            )
        )
    }

    private fun openLedgerFlow() {
        addLedgerLauncher.launch(
            LedgerActivity.buildIntent(
                activityContext = requireActivity(),
                isMembershipFlow = true
            )
        )
    }

    private fun openBitBoxFlow() {
        addBitBoxLauncher.launch(
            BitBoxActivity.buildIntent(
                activityContext = requireActivity(),
                isMembershipFlow = true
            )
        )
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            args = AddAirSignerArgs(
                isMembershipFlow = true,
                tag = tag,
                groupId = args.groupId,
                step = membershipStepManager.currentStep,
            )
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
                    type = SheetOptionType.TYPE_ADD_AIRGAP_KRUX,
                    label = getString(R.string.nc_krux),
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
                        // With BYOH the backup is made after the sharing method is chosen, so a
                        // TAPSIGNER can reach this with nothing to verify yet.
                        if (event.backUpFileName.isEmpty()) {
                            openBackUpTapSigner(event)
                        } else {
                            openVerifyTapSigner(event)
                        }
                    } else {
                        openVerifyColdCard(event)
                    }
                }
                AddKeyListEvent.OnAddAllKey -> onAddAllKey()
                is AddKeyListEvent.OnSeedPhraseBackupVerified -> openInheritanceSeedPhraseVerified(
                    navigator = navigator,
                    signer = event.signer,
                    groupId = args.groupId,
                    walletId = membershipWalletId,
                )
                is AddKeyListEvent.ShowError -> showError(event.message)
                AddKeyListEvent.SelectAirgapType -> showAirgapOptions()

                is AddKeyListEvent.UpdateSignerTag -> findNavController().navigate(
                    AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToCustomKeyAccountFragmentFragment(
                        event.signer,
                        groupId = args.groupId
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

    private fun onAddAllKey() {
        if ((activity as MembershipActivity).changeTimelockFlow != -1) {
            findNavController().popBackStack(R.id.addGroupKeyStepFragment, false)
        } else {
            findNavController().popBackStack()
        }
    }

    private fun handleOnAddKey(data: AddKeyData) {
        when (data.type) {
            MembershipStep.ADD_SEVER_KEY -> {
                if (!isKeyHolderLimited) {
                    navigator.openConfigGroupServerKeyActivity(
                        activityContext = requireActivity(),
                        groupStep = MembershipStage.NONE,
                        groupId = args.groupId
                    )
                }
            }

            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY,
            MembershipStep.BYZANTINE_ADD_INHERITANCE_KEY_1,
                -> openInheritanceKeyPicker()

            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_0,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_1,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_2,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_3,
            MembershipStep.BYZANTINE_ADD_HARDWARE_KEY_4,
            -> openSelectHardwareOption()

            else -> Unit
        }
    }

    private fun openSelectHardwareOption() {
        val options = getKeyOptions(
            context = requireContext(),
            isKeyHolderLimited = isKeyHolderLimited,
            isStandard = viewModel.getGroupWalletType()?.isStandard == true,
        )
        BottomSheetOption.newInstance(
            options = options,
            desc = getString(R.string.nc_key_limit_desc).takeIf { isKeyHolderLimited },
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun handleShowKeysOrCreate(
        signer: List<SignerModel>,
        type: SignerType,
        onEmptySigner: () -> Unit,
    ) {
        if (signer.isNotEmpty()) {
            findNavController().navigate(
                AddByzantineKeyListFragmentDirections.actionAddByzantineKeyListFragmentToTapSignerListBottomSheetFragment(
                    signer.toTypedArray(),
                    type,
                )
            )
        } else {
            onEmptySigner()
        }
    }

    /** The TAPSIGNER has no encrypted backup yet: make and upload one, then verify it. */
    private fun openBackUpTapSigner(event: AddKeyListEvent.OnVerifySigner) {
        navigator.openCreateBackUpTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            masterSignerId = event.signer.id,
            groupId = args.groupId,
            walletId = membershipWalletId,
            claimOption = event.claimOption,
        )
    }

    private fun openVerifyTapSigner(event: AddKeyListEvent.OnVerifySigner) {
        navigator.openVerifyBackupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            backUpFilePath = event.filePath,
            masterSignerId = event.signer.id,
            groupId = (activity as MembershipActivity).groupId,
            walletId = membershipWalletId,
            claimOption = event.claimOption,
        )
    }

    private fun openVerifyColdCard(event: AddKeyListEvent.OnVerifySigner) {
        navigator.openSetupMk4(
            activity = requireActivity(),
            args = SetupMk4Args(
                fromMembershipFlow = true,
                backUpFilePath = event.filePath,
                xfp = event.signer.fingerPrint,
                groupId = (activity as MembershipActivity).groupId,
                action = if (event.backUpFileName.isNotEmpty()) ColdcardAction.VERIFY_KEY else ColdcardAction.UPLOAD_BACKUP,
                signerType = event.signer.type,
                keyName = event.signer.name,
                backUpFileName = event.backUpFileName,
                claimOption = event.claimOption,
                // The backup screens are shared; the vendor is what names the device on them.
                signerTag = event.signer.backupVendorTag,
            )
        )
    }

    private fun openSetupTapSigner() {
        navigator.openSetupTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            groupId = (activity as MembershipActivity).groupId,
        )
    }

    /** The sharing-method choice, entered from the key row rather than opening itself. */
    private fun openSharingMethod(data: AddKeyData) {
        val signer = data.signer ?: return
        openInheritanceSharingMethod(
            signer = signer,
            groupId = args.groupId,
            launcher = keyDistributionLauncher,
        )
    }

    /**
     * The Backup / Verify action on the inheritance key row. Once the owner has recorded a sharing
     * method the row opens the checklist of what they chose — one card per method, and the way to
     * change the choice — whether that is one method or two. A legacy plan records no method and
     * keeps the encrypted-backup flow the key list has always run.
     */
    private fun openInheritanceBackup(data: AddKeyData) {
        val signer = data.signer ?: return
        if (data.opensVerifyBackups()) {
            openInheritanceVerifyBackups(
                signer = signer,
                groupId = args.groupId,
                launcher = keyDistributionLauncher,
                claimOptions = data.claimState.claimOptions,
            )
        } else {
            viewModel.onVerifyClicked(data)
        }
    }

    private fun openInheritanceKeyPicker() {
        navigator.openSignerIntroScreen(
            launcher = inheritanceKeyPickerLauncher,
            activityContext = requireActivity(),
            request = SignerIntroRequest(
                groupId = (activity as MembershipActivity).groupId,
                walletId = (activity as MembershipActivity).walletId,
                walletType = WalletType.MULTI_SIG,
                flow = SignerIntroFlow.OffChainInheritanceKey(
                    // A key already on the wallet cannot fill this slot too, so keep it out of the
                    // "reuse an existing key" offer the picker makes.
                    existingSigners = viewModel.existingWalletSigners(),
                ),
            ),
        )
    }

    private fun openCreateBackUpTapSigner(masterSignerId: String) {
        navigator.openCreateBackUpTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            masterSignerId = masterSignerId,
            groupId = (activity as MembershipActivity).groupId,
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

    private fun showUnBackedUpSignerWarning() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(com.nunchuk.android.wallet.R.string.nc_unbacked_up_signer_warning_desc),
            onYesClick = {}
        )
    }
}

@Composable
fun AddByzantineKeyListScreen(
    viewModel: AddByzantineKeyListViewModel = viewModel(),
    isAddOnly: Boolean = false,
    membershipStepManager: MembershipStepManager,
    onMoreClicked: () -> Unit = {},
    role: AssistedWalletRole = AssistedWalletRole.NONE,
    onSetUpClaimOptionsClicked: (data: AddKeyData) -> Unit = {},
    onInheritanceBackupClicked: (data: AddKeyData) -> Unit = {},
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    AddByzantineKeyListContent(
        onSetUpClaimOptionsClicked = onSetUpClaimOptionsClicked,
        onInheritanceBackupClicked = onInheritanceBackupClicked,
        onContinueClicked = viewModel::onContinueClicked,
        onAddClicked = viewModel::onAddKeyClicked,
        onVerifyClicked = viewModel::onVerifyClicked,
        keys = keys,
        missingBackupKeys = state.missingBackupKeys,
        remainingTime = remainingTime,
        onMoreClicked = onMoreClicked,
        refresh = viewModel::refresh,
        isRefreshing = state.isRefreshing,
        isAddOnly = isAddOnly,
        groupWalletType = state.groupWalletType,
        role = role
    )
}
