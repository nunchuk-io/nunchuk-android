package com.nunchuk.android.main.membership.replacekey

import android.app.Activity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.nunchuk.android.core.util.isAirgapTag
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.MembershipViewModel
import com.nunchuk.android.main.membership.byzantine.addKey.getKeyOptions
import com.nunchuk.android.main.membership.custom.CustomKeyAccountFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragment
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
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
                        handleShowKeysOrCreate(
                            viewModel.getTapSigners() + viewModel.getColdcard(),
                            SignerType.NFC,
                        ) {
                            openSelectInheritanceHardwareOption()
                        }
                    },
                    onCreateNewWalletSuccess = { walletId ->
                        findNavController().navigate(
                            ReplaceKeysFragmentDirections.actionReplaceKeysFragmentToCreateWalletSuccessFragment(
                                walletId = walletId,
                                replacedWalletId = args.walletId
                            )
                        )
                    },
                    onVerifyClicked = { signer ->
                        if (signer.type == SignerType.NFC) {
                            navigator.openVerifyBackupTapSigner(
                                activity = requireActivity(),
                                fromMembershipFlow = true,
                                backUpFilePath = viewModel.getFilePath(signer.id),
                                masterSignerId = signer.id,
                                groupId = (activity as MembershipActivity).groupId,
                                keyId = viewModel.getKeyId(signer.id),
                                walletId = args.walletId
                            )
                        } else {
                            val backUpFileName = viewModel.getBackUpFileName(signer.fingerPrint)
                            navigator.openSetupMk4(
                                activity = requireActivity(),
                                fromMembershipFlow = true,
                                backUpFilePath = viewModel.getFilePath(signer.id),
                                xfp = signer.fingerPrint,
                                action = if (backUpFileName.isNotEmpty()) ColdcardAction.VERIFY_KEY else ColdcardAction.UPLOAD_BACKUP,
                                keyName = signer.name,
                                signerType = signer.type,
                                keyId = viewModel.getKeyId(signer.id),
                                backUpFileName = backUpFileName,
                                groupId = args.groupId,
                                walletId = args.walletId,
                                replacedXfp = viewModel.getReplaceSignerXfp(signer.id)
                            )
                        }
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(TapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            val data = TapSignerListBottomSheetFragmentArgs.fromBundle(bundle)
            if (data.signers.isNotEmpty()) {
                val signer = data.signers.first()
                if (viewModel.isInheritanceXfp(viewModel.replacedXfp)) {
                    if (signer.type == SignerType.COLDCARD_NFC || signer.type == SignerType.AIRGAP) {
                        navigator.openSetupMk4(
                            activity = requireActivity(),
                            fromMembershipFlow = true,
                            action = ColdcardAction.INHERITANCE_PASSPHRASE_QUESTION,
                            groupId = (activity as MembershipActivity).groupId,
                            walletId = (activity as MembershipActivity).walletId,
                            replacedXfp = viewModel.replacedXfp
                        )
                    }
                    clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
                    return@setFragmentResultListener
                }
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
                if (viewModel.isInheritanceXfp(viewModel.replacedXfp)) {
                    openSelectInheritanceHardwareOption()
                    clearFragmentResult(TapSignerListBottomSheetFragment.REQUEST_KEY)
                    return@setFragmentResultListener
                }
                when (data.type) {
                    SignerType.NFC -> openSetupTapSigner()
                    SignerType.AIRGAP -> handleSelectAddAirgapType(selectedSignerTag)
                    SignerType.COLDCARD_NFC -> showAddColdcardOptions()
                    SignerType.PORTAL_NFC -> openSetupPortal()
                    SignerType.SOFTWARE -> openAddSoftwareKey()
                    SignerType.HARDWARE -> showAddKeyByDesktopApp()
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

    private fun openSignerIntro() {
        navigator.openSignerIntroScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
            supportedSigners = activityViewModel.getSupportedSigners()
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
            SheetOptionType.TYPE_ADD_INHERITANCE_NFC -> {
                openSetupTapSigner()
            }

            SheetOptionType.TYPE_ADD_INHERITANCE_COLDCARD -> {
                navigator.openSetupMk4(
                    activity = requireActivity(),
                    fromMembershipFlow = true,
                    action = ColdcardAction.INHERITANCE_PASSPHRASE_QUESTION,
                    groupId = (activity as MembershipActivity).groupId,
                    walletId = (activity as MembershipActivity).walletId,
                    replacedXfp = viewModel.replacedXfp
                )
            }
            SignerType.NFC.ordinal -> handleShowKeysOrCreate(
                viewModel.getTapSigners(),
                SignerType.NFC,
                ::openSetupTapSigner
            )

            SignerType.PORTAL_NFC.ordinal -> {
                viewModel.markNewPortalShown()
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
                fromMembershipFlow = true,
                groupId = args.groupId,
                replacedXfp = viewModel.replacedXfp,
                walletId = args.walletId
            )

            SheetOptionType.TYPE_ADD_COLDCARD_QR,
            SheetOptionType.TYPE_ADD_COLDCARD_FILE,
            -> navigator.openSetupMk4(
                activity = requireActivity(),
                fromMembershipFlow = true,
                action = ColdcardAction.RECOVER_KEY,
                groupId = args.groupId,
                isScanQRCode = option.type == SheetOptionType.TYPE_ADD_COLDCARD_QR,
                replacedXfp = viewModel.replacedXfp,
                walletId = args.walletId
            )

            SheetOptionType.TYPE_ADD_AIRGAP_JADE,
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER,
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT,
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE,
            SheetOptionType.TYPE_ADD_AIRGAP_OTHER -> {
                selectedSignerTag = getSignerTag(option.type)
                handleShowKeysOrCreate(
                    viewModel.getAirgap(getSignerTag(option.type)),
                    SignerType.AIRGAP
                ) { handleSelectAddAirgapType(selectedSignerTag) }
            }

            SheetOptionType.TYPE_ADD_LEDGER -> {
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.LEDGER),
                    SignerType.HARDWARE
                ) { showAddKeyByDesktopApp() }
            }

            SheetOptionType.TYPE_ADD_TREZOR -> {
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.TREZOR),
                    SignerType.HARDWARE
                ) { showAddKeyByDesktopApp() }
            }

            SheetOptionType.TYPE_ADD_COLDCARD_USB -> showAddKeyByDesktopApp()
            SheetOptionType.TYPE_ADD_BITBOX -> {
                handleShowKeysOrCreate(
                    viewModel.getHardwareSigners(SignerTag.BITBOX),
                    SignerType.HARDWARE
                ) { showAddKeyByDesktopApp() }
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

    private fun getSignerTag(type: Int): SignerTag? {
        return when (type) {
            SheetOptionType.TYPE_ADD_AIRGAP_JADE -> SignerTag.JADE
            SheetOptionType.TYPE_ADD_AIRGAP_SEEDSIGNER -> SignerTag.SEEDSIGNER
            SheetOptionType.TYPE_ADD_AIRGAP_PASSPORT -> SignerTag.PASSPORT
            SheetOptionType.TYPE_ADD_AIRGAP_KEYSTONE -> SignerTag.KEYSTONE
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

    private fun openSelectInheritanceHardwareOption() {
        val options = listOf(
            SheetOption(
                type = SheetOptionType.TYPE_ADD_INHERITANCE_NFC,
                label = getString(R.string.nc_tapsigner)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_INHERITANCE_COLDCARD,
                label = getString(R.string.nc_coldcard)
            )
        )
        BottomSheetOption.newInstance(
            options = options,
            desc = "We support Inheritance Key on COLDCARD and TAPSIGNER.",
            title = getString(R.string.nc_what_type_of_hardware_want_to_add),
        ).show(childFragmentManager, "BottomSheetOption")
    }


    private fun handleSelectAddAirgapType(tag: SignerTag?) {
        navigator.openAddAirSignerScreen(
            activityContext = requireActivity(),
            isMembershipFlow = true,
            tag = tag,
            groupId = args.groupId,
            replacedXfp = viewModel.replacedXfp,
            walletId = args.walletId
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
                shouldShowNewPortal = viewModel.shouldShowNewPortal
            )
            BottomSheetOption.newInstance(
                options = options,
                desc = getString(R.string.nc_key_limit_desc).takeIf { isKeyHolderLimited },
                title = getString(R.string.nc_what_type_of_hardware_want_to_add),
            ).show(childFragmentManager, "BottomSheetOption")
        }
    }
}