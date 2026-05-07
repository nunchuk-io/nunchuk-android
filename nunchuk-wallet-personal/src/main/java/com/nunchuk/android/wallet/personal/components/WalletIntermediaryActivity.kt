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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeCameraActivity
import com.nunchuk.android.core.data.model.QuickWalletParam
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
import com.nunchuk.android.core.wallet.WalletSecurityArgs
import com.nunchuk.android.core.wallet.WalletSecurityType
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.RecoverWalletData
import com.nunchuk.android.model.RecoverWalletType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.nav.args.AddWalletArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletActionBottomSheet
import com.nunchuk.android.wallet.personal.components.recover.RecoverWalletOption
import com.nunchuk.android.wallet.personal.components.stablecoin.StablecoinWalletActivity
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WalletIntermediaryActivity : BaseComposeCameraActivity(), BottomSheetOptionListener {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    @Inject
    internal lateinit var vmFactory: WalletIntermediaryViewModel.Factory

    private val viewModel: WalletIntermediaryViewModel by viewModels {
        viewModelProviderFactoryOf { vmFactory.create(isQuickWallet = quickWalletParam != null) }
    }

    private val quickWalletParam: QuickWalletParam? by lazy(LazyThreadSafetyMode.NONE) {
        intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM)
    }

    private val isHideAddKeyDialog: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        intent.getBooleanExtra(EXTRA_IS_HIDE_ADD_KEY_DIALOG, false)
    }

    private var isRecoverGroupWalletViaScanQrCode = false

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                finish()
            }
        }

    private val recoverWalletFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.extractFilePath(uri, isGroupWallet = false)
                }
            }
        }

    private val groupWalletFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.extractFilePath(uri, isGroupWallet = true)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observer()

        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            var showUnassistedSheet by rememberSaveable { mutableStateOf(false) }

            NunchukTheme {
                WalletIntermediaryScreen(
                    isMembership = state.isMembership,
                    remainingAssistedWallets = state.walletsCount.values.sum(),
                    isQuickWalletFlow = quickWalletParam != null,
                    onWalletTypeSelected = { type ->
                        if (type == WalletType.UNASSISTED) {
                            showUnassistedSheet = true
                        } else {
                            onWalletTypeSelected(type)
                        }
                    },
                    onRecoverWalletClicked = { openRecoverWalletScreen() },
                    onScanQRClicked = {
                        checkRunOutGroupWallet {
                            navigator.openScanQrCodeScreen(
                                this,
                                true,
                                quickWalletParam = quickWalletParam,
                            )
                        }
                    },
                    onJoinGroupWalletClicked = {
                        checkRunOutGroupWallet { showInputGroupWalletLinkDialog() }
                    },
                )

                if (showUnassistedSheet) {
                    UnassistedWalletTypeSheet(
                        onDismiss = { showUnassistedSheet = false },
                        onWalletTypeSelected = { type ->
                            showUnassistedSheet = false
                            onWalletTypeSelected(type)
                        },
                    )
                }
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
                this,
                SetupMk4Args(
                    fromMembershipFlow = false,
                    action = ColdcardAction.RECOVER_MULTI_SIG_WALLET,
                    quickWalletParam = quickWalletParam,
                ),
            )

            SheetOptionType.IMPORT_SINGLE_SIG_COLD_CARD -> navigator.openSetupMk4(
                this,
                SetupMk4Args(
                    fromMembershipFlow = false,
                    action = ColdcardAction.RECOVER_SINGLE_SIG_WALLET,
                    quickWalletParam = quickWalletParam,
                ),
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

            SheetOptionType.TYPE_RECOVER_GROUP_WALLET_QR_CODE -> {
                isRecoverGroupWalletViaScanQrCode = true
                requestCameraPermissionOrExecuteAction()
            }

            SheetOptionType.TYPE_RECOVER_GROUP_WALLET_BSMS -> launchFileChooser(isGroupWallet = true)

            else -> Unit
        }
    }

    private fun onWalletTypeSelected(walletType: WalletType) {
        when (walletType) {
            WalletType.ASSISTED -> createAssistedWallet()

            WalletType.UNASSISTED -> Unit // handled inline as bottom sheet

            WalletType.CUSTOM -> {
                if (viewModel.hasSigner) {
                    openCreateNewWalletScreen()
                } else {
                    openWalletEmptySignerScreen()
                }
            }

            WalletType.HOT -> navigator.openHotWalletScreen(
                launcher,
                this,
                quickWalletParam = quickWalletParam,
            )

            WalletType.GROUP -> checkRunOutGroupWallet {
                navigator.openFreeGroupWalletScreen(
                    this,
                    quickWalletParam = quickWalletParam,
                )
            }

            WalletType.DECOY -> navigator.openWalletSecuritySettingScreen(
                activityContext = this,
                args = WalletSecurityArgs(
                    type = WalletSecurityType.CREATE_DECOY_WALLET,
                    quickWalletParam = quickWalletParam,
                ),
            )

            WalletType.MINISCRIPT -> openCreateNewWalletScreen(true)

            WalletType.STABLECOIN -> StablecoinWalletActivity.start(this)
        }
    }

    private fun checkRunOutGroupWallet(action: () -> Unit) {
        viewModel.checkRemainingGroupWalletLimit { runOut ->
            if (runOut) showRunOutFreeGroupWallet() else action()
        }
    }

    private fun observer() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is WalletIntermediaryEvent.OnLoadFileSuccess -> handleLoadFilePath(event)
                is WalletIntermediaryEvent.ShowError -> NCToastMessage(this).showError(event.msg)
                is WalletIntermediaryEvent.Loading -> showOrHideLoading(event.isLoading)
                WalletIntermediaryEvent.NoSigner -> showNoSignerDialog()
                WalletIntermediaryEvent.JoinGroupWalletFailed ->
                    NCToastMessage(this).showInfo(getString(R.string.nc_unable_access_link))

                is WalletIntermediaryEvent.JoinGroupWalletSuccess -> navigator.openFreeGroupWalletScreen(
                    this,
                    groupId = event.groupId,
                    quickWalletParam = quickWalletParam,
                )

                is WalletIntermediaryEvent.ImportWalletSuccessEvent -> {
                    navigator.openFreeGroupWalletRecoverScreen(
                        this,
                        event.wallet.id,
                        event.path,
                        quickWalletParam = quickWalletParam,
                    )
                    finish()
                }
            }
        }
    }

    private fun handleLoadFilePath(event: WalletIntermediaryEvent.OnLoadFileSuccess) {
        if (event.path.isEmpty()) return
        if (event.isGroupWallet) {
            viewModel.parseWalletDescriptor(event.uri, event.path)
        } else {
            navigator.openAddRecoverWalletScreen(
                this,
                RecoverWalletData(type = RecoverWalletType.FILE, filePath = event.path),
                quickWalletParam = quickWalletParam,
            )
            finish()
        }
    }

    private fun showNoSignerDialog() {
        if (isHideAddKeyDialog) return
        NCInfoDialog(this).init(
            message = getString(R.string.nc_no_signer_dialog_message),
            btnYes = getString(R.string.nc_add_key),
            btnInfo = getString(R.string.nc_cancel),
            onYesClick = { navigator.openSignerIntroScreen(activityContext = this) },
        ).show()
    }

    private fun showInputGroupWalletLinkDialog() {
        val dialog = CommonInputBottomSheet.show(
            CommonInputBottomSheet.Args(
                title = getString(R.string.nc_enter_wallet_link),
                desc = "",
                action = getString(R.string.nc_text_continue),
                defaultValue = "",
            ),
            supportFragmentManager,
        )
        dialog.listener = { input ->
            if (input.isNotEmpty()) viewModel.handleInputWalletLink(input)
        }
    }

    private fun openCreateNewWalletScreen(isMiniscript: Boolean = false) {
        navigator.openAddWalletScreen(
            this,
            args = AddWalletArgs(
                quickWalletParam = quickWalletParam,
                isCreateMiniscriptWallet = isMiniscript,
            ),
        )
    }

    private fun openRecoverWalletScreen() {
        val sheet = RecoverWalletActionBottomSheet.show(supportFragmentManager)
        sheet.listener = { option ->
            when (option) {
                RecoverWalletOption.QrCode -> {
                    isRecoverGroupWalletViaScanQrCode = false
                    requestCameraPermissionOrExecuteAction()
                }

                RecoverWalletOption.BSMSFile -> launchFileChooser(isGroupWallet = false)
                RecoverWalletOption.ColdCard -> showOptionImportFromColdCard()
                RecoverWalletOption.HotWallet -> navigator.openRecoverSeedScreen(
                    activityContext = this,
                    isRecoverHotWallet = true,
                    quickWalletParam = quickWalletParam,
                )

                RecoverWalletOption.PortalWallet -> navigator.openPortalScreen(
                    activity = this,
                    args = PortalDeviceArgs(
                        type = PortalDeviceFlow.RECOVER,
                        quickWalletParam = quickWalletParam,
                    ),
                )

                RecoverWalletOption.GroupWallet -> checkRunOutGroupWallet {
                    showRecoveryFormatGroupWallet()
                }
            }
        }
    }

    private fun openScanQRCodeScreen() {
        navigator.openRecoverWalletQRCodeScreen(
            this,
            isGroupWallet = isRecoverGroupWalletViaScanQrCode,
            quickWalletParam = quickWalletParam,
        )
        isRecoverGroupWalletViaScanQrCode = false
    }

    private fun showRunOutWallet(isPersonalWallet: Boolean) {
        val message = if (isPersonalWallet) {
            getString(R.string.nc_run_out_of_personal_wallet)
        } else {
            getString(R.string.nc_run_out_of_group_wallet)
        }
        NCInfoDialog(this).init(
            message = message,
            btnYes = getString(R.string.nc_take_me_there),
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = { openExternalLink("https://nunchuk.io/my-plan") },
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
            NCWarningDialog(this).showDialog(
                message = message,
                btnYes = getString(R.string.nc_sign_up),
                btnNo = getString(R.string.nc_text_got_it),
                onYesClick = { navigator.openSignUpScreen(this) },
            )
        } else {
            NCInfoDialog(this).init(
                message = message,
                btnYes = getString(R.string.nc_text_got_it),
            ).show()
        }
    }

    private fun createAssistedWallet() {
        val state = viewModel.state.value
        val walletCount = state.walletsCount.values.sum()
        val personalWalletCount = viewModel.getPersonalWalletCount()
        val groupWalletCount = viewModel.getGroupWalletCount()

        when {
            state.personalSteps.isNotEmpty() -> openCreateAssistedWallet()
            personalWalletCount == 0 && walletCount > 0 -> openCreateGroupWallet()
            groupWalletCount == 0 && walletCount > 0 -> openCreateAssistedWallet()
            else -> showOptionGroupWalletType()
        }
    }

    private fun openCreateGroupWallet() {
        navigator.openMembershipActivity(
            activityContext = this,
            groupStep = MembershipStage.NONE,
            isPersonalWallet = false,
            quickWalletParam = quickWalletParam,
        )
    }

    private fun openCreateAssistedWallet() {
        val walletType = when {
            viewModel.state.value.personalSteps.any { it.plan == MembershipPlan.IRON_HAND } -> GroupWalletType.TWO_OF_THREE_PLATFORM_KEY
            viewModel.state.value.personalSteps.any { it.plan == MembershipPlan.HONEY_BADGER } -> GroupWalletType.TWO_OF_FOUR_MULTISIG
            else -> null
        }
        when (walletType) {
            GroupWalletType.TWO_OF_THREE_PLATFORM_KEY ->
                viewModel.setLocalMembershipPlan(MembershipPlan.IRON_HAND)

            GroupWalletType.TWO_OF_FOUR_MULTISIG ->
                viewModel.setLocalMembershipPlan(MembershipPlan.HONEY_BADGER)

            else -> Unit
        }
        navigator.openMembershipActivity(
            activityContext = this,
            groupStep = viewModel.getGroupStage(),
            isPersonalWallet = true,
            groupWalletType = walletType,
            quickWalletParam = quickWalletParam,
        )
    }

    private fun openWalletEmptySignerScreen() {
        navigator.openWalletEmptySignerScreen(this)
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
            title = getString(R.string.nc_which_type_wallet_you_want_import),
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showRecoveryFormatGroupWallet() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_RECOVER_GROUP_WALLET_QR_CODE,
                    stringId = R.string.nc_text_wallet_qr_code,
                ),
                SheetOption(
                    SheetOptionType.TYPE_RECOVER_GROUP_WALLET_BSMS,
                    stringId = R.string.nc_bsms_descriptors,
                ),
            ),
            title = getString(R.string.nc_select_recovery_format),
        ).show(supportFragmentManager, "BottomSheetOption")
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
            title = getString(R.string.nc_select_assisted_wallet_type),
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun launchFileChooser(isGroupWallet: Boolean) {
        val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
        val chooser = Intent.createChooser(intent, getString(R.string.nc_text_select_file))
        if (isGroupWallet) {
            groupWalletFileLauncher.launch(chooser)
        } else {
            recoverWalletFileLauncher.launch(chooser)
        }
    }

    companion object {
        const val EXTRA_IS_HIDE_ADD_KEY_DIALOG = "EXTRA_IS_HIDE_ADD_KEY_DIALOG"
        const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"

        fun start(
            activityContext: Context,
            isHideAddKeyDialog: Boolean,
            quickWalletParam: QuickWalletParam?,
        ) {
            val intent = Intent(activityContext, WalletIntermediaryActivity::class.java).apply {
                putExtra(EXTRA_IS_HIDE_ADD_KEY_DIALOG, isHideAddKeyDialog)
                putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
            }
            activityContext.startActivity(intent)
        }
    }
}
