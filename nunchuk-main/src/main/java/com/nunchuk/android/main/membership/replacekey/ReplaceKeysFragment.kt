package com.nunchuk.android.main.membership.replacekey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isAirgapTag
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.MembershipViewModel
import com.nunchuk.android.main.membership.byzantine.addKey.getKeyOptions
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragment
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceKeyAdded
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSeedPhraseBackup
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSeedPhraseVerified
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceSharingMethod
import com.nunchuk.android.main.membership.honey.distribution.openInheritanceVerifyBackups
import com.nunchuk.android.main.membership.honey.distribution.verifyClaimOptionRequest
import com.nunchuk.android.main.membership.model.backupVendorTag
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.bitbox.BitBoxActivity
import com.nunchuk.android.signer.ledger.LedgerActivity
import com.nunchuk.android.signer.trezor.TrezorActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReplaceKeysFragment : Fragment(), BottomSheetOptionListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ReplaceKeysViewModel by activityViewModels()
    private val activityViewModel by activityViewModels<MembershipViewModel>()

    private val args by navArgs<ReplaceKeysFragmentArgs>()
    private var selectedSignerTag: SignerTag? = null
    private val addPortalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.onReplaceKey(it)
                }
            }
        }

    private val addTrezorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.onReplaceKey(it)
                    return@registerForActivityResult
                }
                if (data.getStringExtra(TrezorActivity.EXTRA_RESULT_ACTION) == TrezorActivity.RESULT_ACTION_OPEN_USB_FLOW) {
                    showAddKeyByDesktopApp()
                }
            }
        }

    private val addLedgerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.onReplaceKey(it)
                    return@registerForActivityResult
                }
                if (data.getStringExtra(LedgerActivity.EXTRA_RESULT_ACTION) == LedgerActivity.RESULT_ACTION_OPEN_DESKTOP_FLOW) {
                    showAddKeyByDesktopApp()
                }
            }
        }

    private val addBitBoxLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)?.let {
                    viewModel.onReplaceKey(it)
                    return@registerForActivityResult
                }
                if (data.getStringExtra(BitBoxActivity.EXTRA_RESULT_ACTION) == BitBoxActivity.RESULT_ACTION_OPEN_DESKTOP_FLOW) {
                    showAddKeyByDesktopApp()
                }
            }
        }

    /**
     * The off-chain inheritance key type picker, preceded by the inheritance intro and the
     * passphrase notice. It is the same server-driven picker the assisted key lists use; the
     * replaced slot travels with it so every device's own flow performs a replace rather than an
     * add. Only Ledger, Trezor and BitBox come back as a tag — their in-app pairing belongs here.
     */
    private val inheritanceKeyPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode != Activity.RESULT_OK || data == null) return@registerForActivityResult
            data.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)?.let { signer ->
                // A TAPSIGNER is a master signer: the key for the wallet still has to be derived
                // from it, which the rest cannot do.
                if (signer.type == SignerType.NFC) {
                    viewModel.addExistingTapSignerKey(signer)
                } else {
                    viewModel.onReplaceKey(signer.toSingleSigner())
                }
                return@registerForActivityResult
            }
            (data.getSerializableExtra(GlobalResultKey.EXTRA_SIGNER_TAG) as? SignerTag)?.let { tag ->
                selectedSignerTag = tag
                openInAppHardwareOrDesktopFlow(tag)
            }
        }

    /**
     * The confirm-and-choose-sharing-method flow. Re-reads the replacement status on the way back
     * so the key row reflects the choice; a cancelled run leaves the key without one and the row
     * keeps offering it.
     */
    private val keyDistributionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.getReplaceWalletStatus()
            val (option, signer) = result.data?.verifyClaimOptionRequest()
                ?: return@registerForActivityResult
            when (option) {
                ClaimOption.SEED_PHRASE -> openSeedPhraseBackup(signer)
                ClaimOption.ENCRYPTED_BACKUP -> openEncryptedBackup(signer, option)
            }
        }

    /**
     * Tail of the seed-phrase branch. Ledger and BitBox re-read the restored device and hand back
     * the fingerprint they saw, which is what proves the backup; Coldcard and air-gap finish
     * inside their own screens and come back empty, so the refresh is all this does for them.
     */
    /**
     * The key [verifySeedPhraseBackupLauncher] reported verified, for the confirmation screen when
     * the replacement list has not been reloaded by the time it opens.
     */
    private var seedPhraseVerifiedSigner: SignerModel? = null

    private val verifySeedPhraseBackupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val verifiedXfp = result.data?.getStringExtra(GlobalResultKey.EXTRA_VERIFIED_XFP)
            if (result.resultCode == Activity.RESULT_OK && !verifiedXfp.isNullOrEmpty()) {
                seedPhraseVerifiedSigner = result.data?.parcelable(GlobalResultKey.EXTRA_SIGNER)
                viewModel.onSeedPhraseBackupVerified(verifiedXfp)
            } else {
                viewModel.getReplaceWalletStatus()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ReplaceKeysScreen(
                    viewModel = viewModel,
                    onReplaceKeyClicked = { signer ->
                        viewModel.setReplacingXfp(signer.fingerPrint)
                        openSelectHardwareOption()
                    },
                    onReplaceInheritanceClicked = { signer ->
                        viewModel.setReplacingXfp(signer.fingerPrint)
                        openInheritanceKeyPicker()
                    },
                    onSetUpClaimOptionsClicked = ::openSharingMethod,
                    onInheritanceBackupClicked = ::openInheritanceBackup,
                    onCreateNewWalletSuccess = { walletId ->
                        findNavController().navigate(
                            ReplaceKeysFragmentDirections.actionReplaceKeysFragmentToCreateWalletSuccessFragment(
                                walletId = walletId,
                                replacedWalletId = args.walletId
                            )
                        )
                    },
                    onVerifyClicked = { signer -> openEncryptedBackup(signer) },
                    onRemove = viewModel::onRemoveKey,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeInheritanceFlow()
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
                                ReplaceKeysFragmentDirections.actionReplaceKeysFragmentToCustomKeyAccountFragmentFragment(
                                    signer = signer,
                                    replacedXfp = viewModel.replacedXfp,
                                    isMultisigWallet = viewModel.isMultiSig(),
                                    isFreeWallet = !viewModel.isActiveAssistedWallet,
                                    walletId = args.walletId,
                                    groupId = args.groupId
                                )
                            )
                        } else {
                            viewModel.onUpdateSignerTag(signer, selectedSignerTag)
                        }
                    }

                    else -> {
                        findNavController().navigate(
                            ReplaceKeysFragmentDirections.actionReplaceKeysFragmentToCustomKeyAccountFragmentFragment(
                                signer = signer,
                                replacedXfp = viewModel.replacedXfp,
                                isFreeWallet = !viewModel.isActiveAssistedWallet,
                                isMultisigWallet = viewModel.isMultiSig(),
                                walletId = args.walletId,
                                groupId = args.groupId
                            )
                        )
                    }
                }
            } else {
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.AIRGAP -> handleSelectAddAirgapType(selectedSignerTag)
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    SignerType.PORTAL_NFC -> openSetupPortal()
                    SignerType.SOFTWARE -> openAddSoftwareKey()
                    SignerType.HARDWARE -> selectedSignerTag?.let { tag ->
                        openInAppHardwareOrDesktopFlow(tag)
                    } ?: showAddKeyByDesktopApp()
                    SignerType.UNKNOWN -> openSignerIntro()

                    else -> throw IllegalArgumentException("Signer type invalid ${data.signers.first().type}")
                }
            }
            clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
        }
        setFragmentResultListener(CustomKeyAccountFragment.REQUEST_KEY) { _, bundle ->
            val signer = bundle.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)
            if (signer != null) {
                viewModel.onReplaceKey(signer)
            }
            clearFragmentResult(CustomKeyAccountFragment.REQUEST_KEY)
        }
    }

    /**
     * The server-driven inheritance key picker, scoped to the slot being replaced. It is the same
     * screen the assisted key lists open: the intro, the passphrase notice, then whichever key
     * types the server advertises for an off-chain (`MULTI_SIG`) inheritance key.
     */
    private fun openInheritanceKeyPicker() {
        // Armed here rather than on the result: an air-gapped key is replaced inside its own
        // screen and never comes back with a signer, so the prompt waits for the replacement
        // status to show a key on the slot instead.
        viewModel.onInheritanceKeyAdded()
        navigator.openSignerIntroScreen(
            launcher = inheritanceKeyPickerLauncher,
            activityContext = requireActivity(),
            request = SignerIntroRequest(
                groupId = args.groupId,
                walletId = args.walletId,
                walletType = WalletType.MULTI_SIG,
                flow = SignerIntroFlow.OffChainInheritanceKey(
                    replaceInfo = OnChainAddSignerParam.ReplaceInfo(
                        replacedXfp = viewModel.replacedXfp,
                        step = null,
                    ),
                    // A key already in the wallet cannot fill the slot, so keep it out of the
                    // "reuse an existing key" offer the picker makes.
                    existingSigners = viewModel.existingWalletSigners(),
                ),
            ),
        )
    }

    /** The sharing-method choice, entered from the key row rather than opening itself. */
    private fun openSharingMethod(signer: SignerModel) {
        viewModel.setReplacingXfp(viewModel.getReplaceSignerXfp(signer.fingerPrint))
        openInheritanceSharingMethod(
            signer = signer,
            groupId = args.groupId,
            walletId = args.walletId,
            launcher = keyDistributionLauncher,
        )
    }

    /**
     * The Backup / Verify action on a replacement inheritance key. Once the owner has recorded a
     * sharing method the row opens the checklist of what they chose — one card per method, and the
     * way to change the choice — whether that is one method or two. A legacy plan records no
     * method and keeps the encrypted-backup flow this screen has always run.
     */
    private fun openInheritanceBackup(signer: SignerModel) {
        val slotXfp = viewModel.getReplaceSignerXfp(signer.fingerPrint)
        viewModel.setReplacingXfp(slotXfp)
        val claimState = viewModel.inheritanceClaimState(slotXfp)
        if (claimState.isUnset) {
            openEncryptedBackup(signer, claimState.encryptedBackupClaimOption())
        } else {
            openInheritanceVerifyBackups(
                signer = signer,
                groupId = args.groupId,
                walletId = args.walletId,
                launcher = keyDistributionLauncher,
                claimOptions = claimState.claimOptions,
            )
        }
    }

    /** Back up the seed phrase, then prove it by restoring onto a device and re-adding the key. */
    private fun openSeedPhraseBackup(signer: SignerModel) {
        openInheritanceSeedPhraseBackup(
            navigator = navigator,
            signer = signer,
            groupId = args.groupId,
            walletId = args.walletId,
            launcher = verifySeedPhraseBackupLauncher,
            replacedXfp = viewModel.getReplaceSignerXfp(signer.fingerPrint),
        )
    }

    /**
     * Create the encrypted backup on the device, upload it and verify it — the flow this screen
     * has always run for an inheritance key. [claimOption] names the artifact being resolved so
     * the verification is recorded against the right half of a "do both" key; null keeps the
     * legacy behaviour, where the server infers a key's single verification.
     */
    private fun openEncryptedBackup(signer: SignerModel, claimOption: ClaimOption? = null) {
        val slotXfp = viewModel.getReplaceSignerXfp(signer.id)
        val backUpFileName = viewModel.getBackUpFileName(signer.fingerPrint)
        // One rule for the row's label and for what its action opens. `getBackUpFileName` cannot
        // serve both: it reads a map built only for non-NFC keys, so a TAPSIGNER whose backup the
        // server already holds reads as having none and would be sent to create a second one while
        // its row says "Verify backup".
        val hasEncryptedBackup = if (claimOption != null) {
            viewModel.inheritanceClaimState(slotXfp).hasEncryptedBackupFile
        } else {
            backUpFileName.isNotEmpty()
        }
        if (signer.type == SignerType.NFC) {
            // With BYOH the backup is made after the sharing method is chosen, so a TAPSIGNER can
            // reach this with nothing to verify yet.
            if (!hasEncryptedBackup && claimOption != null) {
                navigator.openCreateBackUpTapSigner(
                    activity = requireActivity(),
                    fromMembershipFlow = true,
                    masterSignerId = signer.id,
                    groupId = args.groupId,
                    walletId = args.walletId,
                    replacedXfp = slotXfp,
                    claimOption = claimOption,
                )
            } else {
                navigator.openVerifyBackupTapSigner(
                    activity = requireActivity(),
                    fromMembershipFlow = true,
                    backUpFilePath = viewModel.getFilePath(signer.id),
                    masterSignerId = signer.id,
                    groupId = args.groupId,
                    keyId = viewModel.getKeyId(signer.id),
                    walletId = args.walletId,
                    replacedXfp = slotXfp,
                    claimOption = claimOption,
                )
            }
        } else {
            navigator.openSetupMk4(
                activity = requireActivity(),
                args = SetupMk4Args(
                    fromMembershipFlow = true,
                    backUpFilePath = viewModel.getFilePath(signer.id),
                    xfp = signer.fingerPrint,
                    action = if (hasEncryptedBackup) ColdcardAction.VERIFY_KEY else ColdcardAction.UPLOAD_BACKUP,
                    keyName = signer.name,
                    signerType = signer.type,
                    keyId = viewModel.getKeyId(signer.id),
                    backUpFileName = backUpFileName,
                    groupId = args.groupId,
                    walletId = args.walletId,
                    replacedXfp = slotXfp,
                    claimOption = claimOption,
                    // The backup screens are shared; the vendor is what names the device on them.
                    signerTag = signer.backupVendorTag,
                )
            )
        }
    }

    /**
     * The three signals the off-chain inheritance flow raises on this screen. All of them live in
     * the state rather than in one-shot events: the key is saved while a device screen is still on
     * top, so this fragment is stopped and an emission would simply be dropped.
     */
    private fun observeInheritanceFlow() {
        // Deriving the wallet's key from a TAPSIGNER can need the card read again; the host
        // activity owns the NFC session.
        val membershipActivity = activity as? MembershipActivity
        membershipActivity?.setTapSignerCachingCallback { isoDep, cvc ->
            viewModel.onTapSignerCardTapped(isoDep, cvc)
        }
        flowObserver(
            viewModel.uiState.map { it.requestTapSignerCardTap }.distinctUntilChanged()
        ) { isRequested ->
            if (isRequested) {
                membershipActivity?.requestTapSignerCaching()
                viewModel.onTapSignerCardTapHandled()
            }
        }
        flowObserver(
            viewModel.uiState.map { it.pendingClaimOptionsSigner }.distinctUntilChanged()
        ) { signer ->
            if (signer != null) {
                viewModel.onClaimOptionsPromptHandled()
                openInheritanceKeyAdded(
                    signer = signer,
                    groupId = args.groupId,
                    walletId = args.walletId,
                    launcher = keyDistributionLauncher,
                )
            }
        }
        flowObserver(
            viewModel.uiState.map { it.seedPhraseVerifiedSigner }.distinctUntilChanged()
        ) { event ->
            if (event is StateEvent.String) {
                viewModel.onSeedPhraseVerifiedHandled()
                openInheritanceSeedPhraseVerified(
                    navigator = navigator,
                    signer = viewModel.uiState.value.replaceSigners.values
                        .firstOrNull { it.fingerPrint.equals(event.data, ignoreCase = true) }
                        ?: seedPhraseVerifiedSigner?.takeIf { it.fingerPrint.equals(event.data, ignoreCase = true) },
                    groupId = args.groupId,
                    walletId = args.walletId,
                )
            }
        }
    }

    override fun onDestroyView() {
        // The callback outlives this fragment otherwise, and would tap into a dead view model.
        (activity as? MembershipActivity)?.clearTapSignerCachingCallback()
        super.onDestroyView()
    }

    private fun openSignerIntro() {
        navigator.openSignerIntroScreen(
            activityContext = requireActivity(),
            request = SignerIntroRequest(
                flow = SignerIntroFlow.ReplaceWalletKey,
                walletId = args.walletId,
                supportedSigners = activityViewModel.getSupportedSigners(),
            ),
        )
    }

    private fun showAddKeyByDesktopApp() {
        NCInfoDialog(requireActivity())
            .showDialog(
                message = getString(R.string.nc_info_hardware_key_not_supported),
            )
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
                    viewModel.getColdcard(),
                    SignerType.COLDCARD_NFC,
                    ::showAddColdcardOptions
                )
            }

            SheetOptionType.TYPE_ADD_COLDCARD_NFC -> navigator.openSetupMk4(
                activity = requireActivity(),
                args = SetupMk4Args(
                    fromMembershipFlow = true,
                    groupId = args.groupId,
                    replacedXfp = viewModel.replacedXfp,
                    walletId = args.walletId
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
                    isScanQRCode = option.type == SheetOptionType.TYPE_ADD_COLDCARD_QR,
                    replacedXfp = viewModel.replacedXfp,
                    walletId = args.walletId
                )
            )

            SheetOptionType.TYPE_ADD_AIRGAP_JADE,
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
            SheetOptionType.TYPE_ADD_AIRGAP_KRUX,
            SheetOptionType.TYPE_ADD_AIRGAP_OTHER -> {
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

            SheetOptionType.TYPE_ADD_COLDCARD_USB -> showAddKeyByDesktopApp()
            SheetOptionType.TYPE_ADD_BITBOX -> {
                selectedSignerTag = SignerTag.BITBOX
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.BITBOX),
                    SignerType.HARDWARE
                ) { openBitBoxFlow() }
            }

            SheetOptionType.TYPE_ADD_SOFTWARE_KEY -> checkTwoSoftwareKeySameDevice {
                handleShowKeysOrCreate(
                    viewModel.getSoftwareSigners(),
                    SignerType.SOFTWARE
                ) { openAddSoftwareKey() }
            }

            else -> Unit
        }
    }

    /**
     * Trezor, Ledger and BitBox pair in-app; every other hardware key has no replace path here
     * and falls back to the "not supported" dialog.
     */
    private fun openInAppHardwareOrDesktopFlow(tag: SignerTag) {
        when (tag) {
            SignerTag.TREZOR -> openTrezorFlow()
            SignerTag.LEDGER -> openLedgerFlow()
            SignerTag.BITBOX -> openBitBoxFlow()
            else -> showAddKeyByDesktopApp()
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

    private fun handleShowKeysOrCreate(
        signer: List<SignerModel>,
        type: SignerType,
        onEmptySigner: () -> Unit,
    ) {
        if (signer.isNotEmpty()) {
            findNavController().navigate(
                ReplaceKeysFragmentDirections.actionReplaceKeysFragmentToTapSignerListBottomSheetFragment(
                    signer.toTypedArray(),
                    type,
                )
            )
        } else {
            onEmptySigner()
        }
    }

    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            args = AddAirSignerArgs(
                isMembershipFlow = true,
                tag = tag,
                groupId = args.groupId,
                replacedXfp = viewModel.replacedXfp,
                walletId = args.walletId,
            )
        )
    }

    private fun openAddSoftwareKey() {
        navigator.openAddSoftwareSignerScreen(
            activityContext = requireActivity(),
            groupId = args.groupId,
            replacedXfp = viewModel.replacedXfp,
            walletId = args.walletId
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

    private fun openSetupPortal() {
        navigator.openPortalScreen(
            launcher = addPortalLauncher,
            activity = requireActivity(),
            args = PortalDeviceArgs(
                type = PortalDeviceFlow.SETUP,
                isMembershipFlow = true,
                walletId = args.walletId
            ),
        )
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

    private fun openSelectHardwareOption() {
        val isKeyHolderLimited =
            viewModel.uiState.value.myRole == AssistedWalletRole.KEYHOLDER_LIMITED
        val isStandard =
            viewModel.uiState.value.group?.walletConfig?.toGroupWalletType()?.isStandard == true

        if (!viewModel.isActiveAssistedWallet) {
            handleShowKeysOrCreate(
                signer = viewModel.getAllSigners(),
                type = SignerType.UNKNOWN,
            ) {
                openSignerIntro()
            }
        } else {
            val options = getKeyOptions(
                context = requireContext(),
                isKeyHolderLimited = isKeyHolderLimited,
                isStandard = isStandard,
            )
            BottomSheetOption.newInstance(
                options = options,
                desc = getString(R.string.nc_key_limit_desc).takeIf { isKeyHolderLimited },
                title = getString(R.string.nc_what_type_of_hardware_want_to_add),
            ).show(childFragmentManager, "BottomSheetOption")
        }
    }
}
