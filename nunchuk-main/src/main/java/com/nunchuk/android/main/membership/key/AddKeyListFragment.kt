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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.constraintlayout.compose.ConstraintLayout
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
import com.nunchuk.android.compose.textSecondary
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
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceKeyAdded
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSeedPhraseBackup
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSeedPhraseVerified
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSharingMethod
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceVerifyBackups
import com.nunchuk.android.main.membership.honey.distribution.InheritanceClaimStatusRow
import com.nunchuk.android.main.membership.honey.distribution.verifyClaimOptionRequest
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.main.membership.model.ClaimOptionState
import com.nunchuk.android.main.membership.model.needsEncryptedBackupUpload
import com.nunchuk.android.main.membership.model.InheritanceBackupBranch
import com.nunchuk.android.main.membership.model.backupVendorTag
import com.nunchuk.android.main.membership.model.inheritanceBackupBranch
import com.nunchuk.android.main.membership.model.getButtonText
import com.nunchuk.android.main.membership.model.getLabel
import com.nunchuk.android.main.membership.model.resId
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.model.isAddInheritanceKey
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.SetupMk4Args
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
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections.emptyList

@AndroidEntryPoint
class AddKeyListFragment : MembershipFragment(), BottomSheetOptionListener {

    private val viewModel by activityViewModels<AddKeyListViewModel>()

    private var selectedSignerTag: SignerTag? = null

    private val membershipGroupId: String get() = (activity as MembershipActivity).groupId
    private val membershipWalletId: String get() = (activity as MembershipActivity).walletId


    private val addPortalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.onSelectedExistingHardwareSigner(it)
                }
            }
        }

    private val addTrezorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.onSelectedExistingHardwareSigner(it)
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
                    viewModel.onSelectedExistingHardwareSigner(it)
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
                    viewModel.onSelectedExistingHardwareSigner(it)
                    return@registerForActivityResult
                }
                if (data.getStringExtra(BitBoxActivity.EXTRA_RESULT_ACTION) == BitBoxActivity.RESULT_ACTION_OPEN_DESKTOP_FLOW) {
                    openRequestAddDesktopKey(SignerTag.BITBOX)
                }
            }
        }

    // Add-key flow reused from the free group wallet for customized (custom Miniscript / n-m)
    // assisted drafts. The returned key is saved against the step set by onAddKeyClicked.
    private val signerIntroLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)?.let { signer ->
                    viewModel.onSelectedExistingHardwareSigner(signer.toSingleSigner())
                }
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
                    viewModel.onSelectedExistingHardwareSigner(signer.toSingleSigner())
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

    /**
     * The confirm-and-choose-sharing-method flow. Re-runs [AddKeyListViewModel.refresh] on the way back so the key row
     * reflects the choice; a cancelled run leaves the key without one and the row keeps offering it.
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
                    groupId = membershipGroupId,
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
                viewModel.onSeedPhraseBackupVerified(verifiedXfp)
            } else {
                viewModel.refresh()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AddKeyListScreen(
                    viewModel = viewModel,
                    membershipStepManager = membershipStepManager,
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
                    groupId = membershipGroupId,
                    launcher = keyDistributionLauncher,
                )
            }
        }
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
                        when {
                            signer.type == SignerType.AIRGAP && signer.tags.isEmpty() && selectedSignerTag != null ->
                                viewModel.onUpdateSignerTag(signer, selectedSignerTag)

                            else -> viewModel.onSelectedExistingHardwareSigner(signer.toSingleSigner())
                        }
                    }
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.PORTAL_NFC -> openSetupPortal()
                    SignerType.AIRGAP -> handleSelectAddAirgapType(selectedSignerTag)
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    SignerType.HARDWARE -> selectedSignerTag?.let { openInAppHardwareOrDesktopFlow(it) }
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

            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(
                requireActivity(),
                SetupMk4Args(fromMembershipFlow = true)
            )

            SheetOptionType.TYPE_ADD_COLDCARD_QR,
            SheetOptionType.TYPE_ADD_COLDCARD_FILE,
                -> navigator.openSetupMk4(
                requireActivity(),
                SetupMk4Args(
                    fromMembershipFlow = true,
                    action = ColdcardAction.RECOVER_KEY,
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
                    groupId = membershipGroupId,
                    walletId = membershipWalletId,
                )
                is AddKeyListEvent.ShowError -> showError(event.message)
                AddKeyListEvent.SelectAirgapType -> showAirgapOptions()
                // Draft switched to an on-chain (Miniscript) wallet: this screen can't host it,
                // so close and let the user reopen the pending wallet on the correct screen.
                AddKeyListEvent.RequireReopenWallet -> requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        // The callback outlives this fragment otherwise, and would tap into a dead view model.
        (activity as? MembershipActivity)?.clearTapSignerCachingCallback()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun onAddAllKey() {
        if ((activity as MembershipActivity).changeTimelockFlow != -1) {
            findNavController().popBackStack(R.id.addKeyStepFragment, false)
        } else {
            findNavController().popBackStack()
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

            MembershipStep.HONEY_ADD_INHERITANCE_KEY -> openInheritanceKeyPicker()

            MembershipStep.IRON_ADD_HARDWARE_KEY_1,
            MembershipStep.IRON_ADD_HARDWARE_KEY_2,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
                -> if (viewModel.state.value.isCustomized) {
                openSignerIntro()
            } else {
                openSelectHardwareOption()
            }

            else -> Unit
        }
    }

    private fun openSignerIntro() {
        navigator.openSignerIntroScreen(
            launcher = signerIntroLauncher,
            activityContext = requireActivity(),
            groupId = "",
            walletType = WalletType.MULTI_SIG,
        )
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

    /** The TAPSIGNER has no encrypted backup yet: make and upload one, then verify it. */
    private fun openBackUpTapSigner(event: AddKeyListEvent.OnVerifySigner) {
        navigator.openCreateBackUpTapSigner(
            activity = requireActivity(),
            fromMembershipFlow = true,
            masterSignerId = event.signer.id,
            groupId = membershipGroupId,
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
                action = if (event.backUpFileName.isNotEmpty()) ColdcardAction.VERIFY_KEY else ColdcardAction.UPLOAD_BACKUP,
                keyName = event.signer.name,
                signerType = event.signer.type,
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

    /** The sharing-method choice, entered from the key row rather than opening itself. */
    private fun openSharingMethod(data: AddKeyData) {
        val signer = data.signer ?: return
        openInheritanceSharingMethod(
            signer = signer,
            groupId = membershipGroupId,
            launcher = keyDistributionLauncher,
        )
    }

    /**
     * The Backup / Verify action on the inheritance key row. The branch is the owner's sharing
     * method, not the device: a Coldcard whose owner chose the seed phrase goes down the
     * seed-phrase branch rather than its own encrypted-backup flow, and "do both" opens the
     * checklist that tracks the two artifacts apart.
     */
    private fun openInheritanceBackup(data: AddKeyData) {
        val signer = data.signer ?: return
        when (data.inheritanceBackupBranch()) {
            InheritanceBackupBranch.SEED_PHRASE -> openInheritanceSeedPhraseBackup(
                navigator = navigator,
                signer = signer,
                groupId = membershipGroupId,
                walletId = membershipWalletId,
                launcher = verifySeedPhraseBackupLauncher,
            )

            InheritanceBackupBranch.BOTH -> openInheritanceVerifyBackups(
                signer = signer,
                groupId = membershipGroupId,
                launcher = keyDistributionLauncher,
            )

            // The encrypted backup runs the flow the key list already owns: make the file on
            // the device, import it, upload it, verify it. A legacy plan carries no sharing
            // method at all and lands here too.
            InheritanceBackupBranch.ENCRYPTED_BACKUP, null -> viewModel.onVerifyClicked(data)
        }
    }

    private fun openInheritanceKeyPicker() {
        navigator.openSignerIntroScreen(
            launcher = inheritanceKeyPickerLauncher,
            activityContext = requireActivity(),
            groupId = (activity as MembershipActivity).groupId,
            walletId = (activity as MembershipActivity).walletId,
            walletType = WalletType.MULTI_SIG,
            onChainAddSignerParam = OnChainAddSignerParam(
                flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER or
                        OnChainAddSignerParam.FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER,
                // A key already on the wallet cannot fill this slot too, so keep it out of the
                // "reuse an existing key" offer the picker makes.
                existingSigners = viewModel.existingWalletSigners(),
            ),
        )
    }

}

@Composable
fun AddKeyListScreen(
    viewModel: AddKeyListViewModel = viewModel(),
    membershipStepManager: MembershipStepManager,
    onMoreClicked: () -> Unit = {},
    onSetUpClaimOptionsClicked: (data: AddKeyData) -> Unit = {},
    onInheritanceBackupClicked: (data: AddKeyData) -> Unit = {},
) {
    val keys by viewModel.key.collectAsStateWithLifecycle()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val remainingTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
    AddKeyListContent(
        onSetUpClaimOptionsClicked = onSetUpClaimOptionsClicked,
        onInheritanceBackupClicked = onInheritanceBackupClicked,
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
    onSetUpClaimOptionsClicked: (data: AddKeyData) -> Unit = {},
    onInheritanceBackupClicked: (data: AddKeyData) -> Unit = {},
    refresh: () -> Unit = { },
) {
    val state = rememberPullRefreshState(isRefreshing, refresh)

    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (remainingTime <= 0) "" else stringResource(R.string.nc_estimate_remain_time, remainingTime),
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
                    enabled = keys.all { it.isVerifyOrAddKey && !it.isInheritanceIncomplete }
                            && missingBackupKeys.isEmpty()
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
                            text = stringResource(R.string.nc_let_configure_your_wallet),
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
                            onSetUpClaimOptionsClicked = onSetUpClaimOptionsClicked,
                            onInheritanceBackupClicked = onInheritanceBackupClicked,
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
    onSetUpClaimOptionsClicked: (data: AddKeyData) -> Unit = {},
    onInheritanceBackupClicked: (data: AddKeyData) -> Unit = {},
    isDisabled: Boolean = false,
    isStandard: Boolean = false
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val (banner, content) = createRefs()
        Box(
            modifier = Modifier
                .constrainAs(content) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }) {
            if (item.signer != null) {
                Box(
                    modifier = modifier.background(
                        color = if (item.isRowComplete) {
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
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            label = stringResource(
                                                R.string.nc_acct_x,
                                                item.signer.index
                                            ),
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
                            if (item.needsClaimOptions) {
                                NcOutlineButton(
                                    modifier = Modifier.height(36.dp),
                                    onClick = { onSetUpClaimOptionsClicked(item) },
                                ) {
                                    Text(text = stringResource(R.string.nc_set_up))
                                }
                            } else if (item.needsClaimVerification) {
                                // Ahead of the verifyType tick on purpose: a "do both" key is half
                                // done after one artifact and still owes the other.
                                NcOutlineButton(
                                    modifier = Modifier.height(36.dp),
                                    onClick = { onInheritanceBackupClicked(item) },
                                ) {
                                    Text(
                                        text = if (item.needsEncryptedBackupUpload) {
                                            stringResource(R.string.nc_upload_backup)
                                        } else {
                                            stringResource(R.string.nc_verify_backup)
                                        }
                                    )
                                }
                            } else if (item.isRowComplete) {
                                // The same rule the card's colour uses. Reading the local verifyType
                                // here instead left a green row still offering "Verify backup": for an
                                // inheritance key the server's per-method records decide, and the local
                                // step carries a single flag that can lag behind them.
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
                        if (item.showsClaimStatus) {
                            // Full card width, lined up with the text column (48dp icon + 8dp gap).
                            InheritanceClaimStatusRow(
                                modifier = Modifier.padding(start = 56.dp),
                                claimState = item.claimState,
                            )
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
                        ConfigItem(
                            item = item,
                            onAddClicked = onAddClicked,
                            isDisabled = isDisabled,
                            isStandard = isStandard
                        )
                    }
                }
            }
        }

        if (item.type.isAddInheritanceKey) {
            Image(
                modifier = modifier
                    .constrainAs(banner) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                contentDescription = "Inheritance icon",
                painter = painterResource(id = R.drawable.ic_badge_inheritance)
            )
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
