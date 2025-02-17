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

package com.nunchuk.android.wallet.personal.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.CommonInputBottomSheet
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletActionBottomSheet
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletOption
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalletIntermediaryNewUIFragment : BaseCameraFragment<ViewBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    private val viewModel: WalletIntermediaryViewModel by viewModels()
    private val isQuickWallet: Boolean by lazy {
        requireActivity().intent.getBooleanExtra(
            "is_quick_wallet",
            false
        )
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                requireActivity().apply {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }

    private val hasSigner
        get() = requireArguments().getBoolean(WalletIntermediaryActivity.EXTRA_HAS_SIGNER, false)

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): ViewBinding {
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                WalletIntermediaryScreen(
                    isMembership = state.isMembership,
                    remainingAssistedWallets = state.walletsCount.values.sum(),
                    onRecoverWalletClicked = {
                        openRecoverWalletScreen()
                    },
                    onWalletTypeSelected = {
                        onWalletTypeSelected(it)
                    },
                    onScanQRClicked = {
                        checkRunOutGroupWallet {
                            navigator.openScanQrCodeScreen(requireActivity(), true)
                        }
                    },
                    onJoinGroupWalletClicked = {
                        checkRunOutGroupWallet {
                            showInputGroupWalletLinkDialog()
                        }
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(isQuickWallet)
        observer()

        childFragmentManager.setFragmentResultListener(
            UnassistedWalletTypeBottomSheet.TAG,
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.parcelable<UnassistedWalletTypeBottomSheet.Result>(
                UnassistedWalletTypeBottomSheet.RESULT
            )
                ?: return@setFragmentResultListener
            val walletType = result.walletType
            onWalletTypeSelected(walletType)
            clearFragmentResult(UnassistedWalletTypeBottomSheet.TAG)
        }
    }

    private fun onWalletTypeSelected(walletType: WalletType) {
        when (walletType) {
            WalletType.ASSISTED -> {
                createAssistedWallet()
            }

            WalletType.UNASSISTED -> {
                UnassistedWalletTypeBottomSheet.show(childFragmentManager)
            }

            WalletType.CUSTOM -> {
                if (isQuickWallet) {
                    navigator.openCreateNewSeedScreen(this, true)
                } else if (hasSigner) {
                    openCreateNewWalletScreen()
                } else {
                    openWalletEmptySignerScreen()
                }
            }

            WalletType.HOT -> {
                navigator.openHotWalletScreen(launcher, requireActivity(), isQuickWallet)
            }

            WalletType.GROUP -> {
                checkRunOutGroupWallet {
                    navigator.openFreeGroupWalletScreen(requireActivity())
                }
            }

            WalletType.DECOY -> {
                navigator.openWalletSecuritySettingScreen(
                    activityContext = requireContext(),
                    args = WalletSecurityArgs(type = WalletSecurityType.CREATE_DECOY_WALLET)
                )
            }
        }
    }

    private fun checkRunOutGroupWallet(action: () -> Unit) {
        viewModel.checkRemainingGroupWalletLimit {
            if (it) {
                showRunOutFreeGroupWallet()
            } else {
                action()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAssistedWalletConfig()
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        openScanQRCodeScreen()
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.IMPORT_MULTI_SIG_COLD_CARD -> navigator.openSetupMk4(
                requireActivity(), false, ColdcardAction.RECOVER_MULTI_SIG_WALLET
            )

            SheetOptionType.IMPORT_SINGLE_SIG_COLD_CARD -> navigator.openSetupMk4(
                requireActivity(), false, ColdcardAction.RECOVER_SINGLE_SIG_WALLET
            )

            SheetOptionType.TYPE_GROUP_WALLET -> {
                if (viewModel.isGroupWalletAvailable()) {
                    openCreateGroupWallet()
                } else {
                    showRunOutWallet(false)
                }
            }

            SheetOptionType.TYPE_PERSONAL_WALLET -> {
                if (viewModel.isPersonalWalletAvailable()) {
                    openCreateAssistedWallet()
                } else {
                    showRunOutWallet(true)
                }
            }
        }
    }

    private fun showRunOutWallet(isPersonalWallet: Boolean) {
        val message = if (isPersonalWallet) {
            getString(R.string.nc_run_out_of_personal_wallet)
        } else {
            getString(R.string.nc_run_out_of_group_wallet)
        }
        NCInfoDialog(requireActivity()).init(
            message = message,
            btnYes = getString(R.string.nc_take_me_there),
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                requireActivity().openExternalLink("https://nunchuk.io/my-plan")
            }
        ).show()
    }

    private fun showRunOutFreeGroupWallet() {
        val isGuestMode = signInModeHolder.getCurrentMode().isGuestMode()
        val message = if (isGuestMode) {
            getString(R.string.nc_free_group_wallet_run_out_guest_mode)
        } else {
            getString(R.string.nc_free_group_wallet_run_out_signed_mode)
        }
        if (isGuestMode) {
            NCWarningDialog(requireActivity()).showDialog(
                message = message,
                btnYes = getString(R.string.nc_sign_up),
                btnNo = getString(R.string.nc_text_got_it),
                onYesClick = {
                    navigator.openSignUpScreen(requireActivity())
                }
            )
        } else {
            NCInfoDialog(requireActivity()).init(
                message = message,
                btnYes = getString(R.string.nc_text_got_it),
            ).show()
        }
    }

    private fun observer() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is WalletIntermediaryEvent.OnLoadFileSuccess -> handleLoadFilePath(event)
                is WalletIntermediaryEvent.ShowError -> showError(event.msg)
                is WalletIntermediaryEvent.Loading -> showOrHideLoading(event.isLoading)
                WalletIntermediaryEvent.NoSigner -> showNoSignerDialog()
                WalletIntermediaryEvent.JoinGroupWalletFailed -> {
                    NCToastMessage(requireActivity()).showInfo(getString(R.string.nc_unable_access_link))
                }

                is WalletIntermediaryEvent.JoinGroupWalletSuccess -> {
                    navigator.openFreeGroupWalletScreen(requireActivity(), event.groupId)
                }

                is WalletIntermediaryEvent.ImportWalletSuccessEvent -> {
                    navigator.openFreeGroupWalletRecoverScreen(requireActivity(), event.wallet.id, event.path)
                    requireActivity().finish()
                }
            }
        }
    }

    private fun showNoSignerDialog() {
        NCInfoDialog(requireActivity()).init(
            message = getString(R.string.nc_no_signer_dialog_message),
            btnYes = getString(R.string.nc_add_key),
            btnInfo = getString(R.string.nc_cancel),
            onYesClick = {
                navigator.openSignerIntroScreen(requireActivity())
            }
        ).show()
    }

    private fun showInputGroupWalletLinkDialog() {
        val dialog = CommonInputBottomSheet.show(
            CommonInputBottomSheet.Args(
                title = getString(R.string.nc_enter_wallet_link),
                desc = "",
                action = getString(R.string.nc_text_continue),
                defaultValue = ""
            ),
            childFragmentManager
        )
        dialog.listener = {
            if (it.isNotEmpty()) {
                viewModel.handleInputWalletLink(it)
            }
        }
    }

    private fun handleLoadFilePath(it: WalletIntermediaryEvent.OnLoadFileSuccess) {
        if (it.path.isNotEmpty()) {
            if (it.isGroupWallet) {
                viewModel.parseWalletDescriptor(it.uri, it.path)
            } else {
                navigator.openAddRecoverWalletScreen(
                    requireActivity(), RecoverWalletData(
                        type = RecoverWalletType.FILE,
                        filePath = it.path
                    )
                )
                requireActivity().finish()
            }
        }
    }

    private fun openCreateNewWalletScreen() {
        navigator.openAddWalletScreen(requireContext())
    }

    private fun openRecoverWalletScreen() {
        val recoverWalletBottomSheet = RecoverWalletActionBottomSheet.show(childFragmentManager)
        recoverWalletBottomSheet.listener = {
            when (it) {
                RecoverWalletOption.QrCode -> requestCameraPermissionOrExecuteAction()
                RecoverWalletOption.BSMSFile -> openSelectFileChooser(WalletIntermediaryActivity.REQUEST_CODE)
                RecoverWalletOption.ColdCard -> showOptionImportFromColdCard()
                RecoverWalletOption.HotWallet -> navigator.openRecoverSeedScreen(
                    activityContext = requireActivity(),
                    isRecoverHotWallet = true
                )

                RecoverWalletOption.PortalWallet -> navigator.openPortalScreen(
                    activity = requireActivity(),
                    args = PortalDeviceArgs(
                        type = PortalDeviceFlow.RECOVER
                    )
                )

                RecoverWalletOption.GroupWallet -> openSelectFileChooser(WalletIntermediaryActivity.REQUEST_CODE_GROUP_WALLET)
            }
        }
    }

    private fun openScanQRCodeScreen() {
        navigator.openRecoverWalletQRCodeScreen(requireContext(), false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if ((requestCode == WalletIntermediaryActivity.REQUEST_CODE || requestCode == WalletIntermediaryActivity.REQUEST_CODE_GROUP_WALLET) && resultCode == AppCompatActivity.RESULT_OK) {
            intent?.data?.let {
                viewModel.extractFilePath(
                    it,
                    isGroupWallet = requestCode == WalletIntermediaryActivity.REQUEST_CODE_GROUP_WALLET
                )
            }
        }
    }

    private fun createAssistedWallet() {
        val state = viewModel.state.value
        val walletCount = state.walletsCount.values.sum()
        if (state.personalSteps.isNotEmpty()) {
            openCreateAssistedWallet()
        } else if (viewModel.isPersonalWalletAvailable() && walletCount == 1) {
            openCreateAssistedWallet()
        } else if (viewModel.isGroupWalletAvailable() && walletCount == 1) {
            openCreateGroupWallet()
        } else {
            showOptionGroupWalletType()
        }
    }

    private fun openCreateGroupWallet() {
        navigator.openMembershipActivity(
            activityContext = requireActivity(),
            groupStep = MembershipStage.NONE,
            isPersonalWallet = false
        )
    }

    private fun openCreateAssistedWallet() {
        val walletType = when {
            viewModel.state.value.personalSteps.any { it.plan == MembershipPlan.IRON_HAND } -> GroupWalletType.TWO_OF_THREE_PLATFORM_KEY
            viewModel.state.value.personalSteps.any { it.plan == MembershipPlan.HONEY_BADGER } -> GroupWalletType.TWO_OF_FOUR_MULTISIG
            else -> null
        }
        when (walletType) {
            GroupWalletType.TWO_OF_THREE_PLATFORM_KEY -> viewModel.setLocalMembershipPlan(
                MembershipPlan.IRON_HAND
            )

            GroupWalletType.TWO_OF_FOUR_MULTISIG -> viewModel.setLocalMembershipPlan(
                MembershipPlan.HONEY_BADGER
            )

            else -> Unit
        }
        navigator.openMembershipActivity(
            activityContext = requireActivity(),
            groupStep = viewModel.getGroupStage(),
            isPersonalWallet = true,
            walletType = walletType
        )
    }

    private fun openWalletEmptySignerScreen() {
        navigator.openWalletEmptySignerScreen(requireActivity())
    }

    private fun showOptionImportFromColdCard() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.IMPORT_SINGLE_SIG_COLD_CARD,
                    stringId = R.string.nc_single_sig_wallet,
                    resId = R.drawable.ic_group_wallet_menu,
                ),
                SheetOption(
                    SheetOptionType.IMPORT_MULTI_SIG_COLD_CARD,
                    stringId = R.string.nc_multisig_wallet,
                    resId = R.drawable.ic_personal_wallet_menu,
                ),
            ),
            title = getString(R.string.nc_which_type_wallet_you_want_import)
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showOptionGroupWalletType() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_GROUP_WALLET,
                    stringId = R.string.nc_group_wallet,
                    resId = R.drawable.ic_group_wallet_menu,
                ),
                SheetOption(
                    SheetOptionType.TYPE_PERSONAL_WALLET,
                    stringId = R.string.nc_personal_wallet,
                    resId = R.drawable.ic_personal_wallet_menu,
                ),
            ),
            title = getString(R.string.nc_select_assisted_wallet_type)
        ).show(childFragmentManager, "BottomSheetOption")
    }
}