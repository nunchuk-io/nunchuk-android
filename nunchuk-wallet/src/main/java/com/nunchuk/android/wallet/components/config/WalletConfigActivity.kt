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

package com.nunchuk.android.wallet.components.config

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatddMMMyyyyDate
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.openSelectFileChooser
import com.nunchuk.android.core.util.upperCase
import com.nunchuk.android.core.wallet.WalletBottomSheetResult
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.isKeyHolderWithoutKeyHolderLimited
import com.nunchuk.android.model.byzantine.isMasterOrAdmin
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.utils.toInvoiceInfo
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.alias.AliasActivity
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent
import com.nunchuk.android.wallet.databinding.ActivityWalletConfigBinding
import com.nunchuk.android.widget.NCDeleteConfirmationDialog
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCProgressDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WalletConfigActivity : BaseWalletConfigActivity<ActivityWalletConfigBinding>() {

    private val viewModel: WalletConfigViewModel by viewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val signatureMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                        ?: return@registerForActivityResult
                val securityQuestionToken =
                    data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                viewModel.deleteAssistedWallet(signatureMap, securityQuestionToken)
            }
        }

    private val aliasLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val alias = data.getString(AliasActivity.EXTRA_ALIAS).orEmpty()
                if (alias.isNotEmpty()) {
                    NCToastMessage(this).showMessage(getString(R.string.nc_alias_has_been_set))
                } else {
                    NCToastMessage(this).showMessage(getString(R.string.nc_alias_removed))
                }
            }
        }

    private val exportWalletToPortalLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                NCToastMessage(this).showMessage(getString(R.string.nc_export_to_portal_success))
            }
        }

    private val ncProgressDialog: NCProgressDialog by lazy {
        NCProgressDialog(activity = this@WalletConfigActivity)
    }

    private val args: WalletConfigArgs by lazy { WalletConfigArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityWalletConfigBinding.inflate(layoutInflater).also {
        enableEdgeToEdge()
    }

    private var isCancelExportInvoice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.container.setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            WalletConfigView(
                state = state,
                onShowMore = ::showMoreOptions,
                onChangeAlias = { aliasLauncher.launch(AliasActivity.createIntent(this, args.walletId)) },
                openWalletConfig = {
                    showReEnterPassword(TargetAction.UPDATE_SERVER_KEY, it)
                },
                onEditWalletName = {
                    WalletUpdateBottomSheet.show(
                        fragmentManager = supportFragmentManager,
                        walletName = viewModel.walletName()
                    ).setListener(viewModel::handleEditCompleteEvent)
                },
            )
        }

        observeEvent()
        sharedViewModel.init(args.walletId)

        supportFragmentManager.setFragmentResultListener(
            WalletComposeBottomSheet.TAG,
            this
        ) { _, bundle ->
            val result = bundle.parcelable<WalletBottomSheetResult>(WalletComposeBottomSheet.RESULT)
                ?: return@setFragmentResultListener
            if (result.walletId != null) {
                navigator.openRollOverWalletScreen(
                    this, oldWalletId = args.walletId, newWalletId = result.walletId!!,
                    startScreen = RollOverWalletFlow.REFUND,
                    source = RollOverWalletSource.WALLET_CONFIG
                )
            }
            supportFragmentManager.clearFragmentResult(WalletComposeBottomSheet.TAG)
        }

        lifecycleScope.launch {
            viewModel.progressFlow.collect { (current, total) ->
                if (current == 0 || total == 0) {
                    return@collect
                }
                when (current) {
                    total -> ncProgressDialog.dialog?.dismiss()
                    else -> ncProgressDialog.updateProgress(current, total)
                }
            }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_EXPORT_AS_QR -> showExportQRTypeOption()
            SheetOptionType.TYPE_DELETE_WALLET -> handleDeleteWallet()
            SheetOptionType.TYPE_EXPORT_TO_COLD_CARD -> showExportColdcardOptions()
            SheetOptionType.TYPE_FORCE_REFRESH_WALLET -> showForceRefreshWalletDialog()
            SheetOptionType.TYPE_SAVE_WALLET_CONFIG -> showSaveWalletConfigurationOption()
            SheetOptionType.TYPE_EXPORT_BSMS -> showSaveShareOption()
            SheetOptionType.TYPE_EXPORT_PORTAL -> navigator.openPortalScreen(
                launcher = exportWalletToPortalLauncher,
                activity = this,
                args = PortalDeviceArgs(walletId = args.walletId, type = PortalDeviceFlow.EXPORT)
            )

            SheetOptionType.TYPE_IMPORT_TX_COIN_CONTROL -> showImportFormatOption()
            SheetOptionType.TYPE_EXPORT_TX_COIN_CONTROL -> showExportFormatOption()
            SheetOptionType.TYPE_EXPORT_NUNCHUK -> viewModel.exportCoinControlNunchuk()
            SheetOptionType.TYPE_ROLL_OVER_ANOTHER_WALLET -> {
                WalletComposeBottomSheet.show(
                    fragmentManager = supportFragmentManager,
                    exclusiveAssistedWalletIds = arrayListOf(args.walletId),
                    configArgs = WalletComposeBottomSheet.ConfigArgs()
                )
            }

            SheetOptionType.TYPE_EXPORT_BIP329 -> {
                NCWarningDialog(this).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_select_export_format_desc),
                    onYesClick = { viewModel.exportCoinControlBIP329() }
                )
            }

            SheetOptionType.TYPE_IMPORT_NUNCHUK -> {
                NCWarningDialog(this).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_select_import_format_desc),
                    onYesClick = { openSelectFileChooser(requestCode = IMPORT_NUNCHUK_REQ) }
                )
            }

            SheetOptionType.TYPE_IMPORT_BIP329 -> {
                NCWarningDialog(this).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_select_import_format_desc),
                    onYesClick = { openSelectFileChooser(requestCode = IMPORT_BIP329_REQ) }
                )
            }

            SheetOptionType.TYPE_CONFIGURE_GAP_LIMIT -> {
                showConfigureGapLimitDialog()
            }

            SheetOptionType.TYPE_EDIT_PRIMARY_OWNER -> {
                navigator.openPrimaryOwnerScreen(
                    activityContext = this,
                    walletId = args.walletId,
                    groupId = viewModel.getGroupId().orEmpty(),
                    flowInfo = PrimaryOwnerFlow.EDIT
                )
            }

            SheetOptionType.TYPE_REPLACE_KEY -> {
                if (viewModel.isAssistedWallet()) {
                    showReEnterPassword(TargetAction.REPLACE_KEYS)
                } else {
                    navigator.openMembershipActivity(
                        activityContext = this,
                        groupStep = MembershipStage.REPLACE_KEY,
                        walletId = args.walletId,
                        groupId = viewModel.getGroupId().orEmpty(),
                    )
                }
            }

            SheetOptionType.TYPE_EXPORT_TX_INVOICES -> {
                isCancelExportInvoice = false
                runCatching {
                    ncProgressDialog.dialog?.dismiss()
                }
                ncProgressDialog.showDialog(
                    currentStep = 1,
                    totalSteps = viewModel.getTransactions().size,
                    onCancelClick = {
                        isCancelExportInvoice = true
                    }
                )
                viewModel.exportInvoice(
                    viewModel.getTransactions().map { it.toInvoiceInfo(this, false) },
                    "${viewModel.getWalletName()}_transaction_history_${
                        System.currentTimeMillis().formatddMMMyyyyDate.upperCase()
                    }"
                )
            }
        }
    }

    private fun showConfigureGapLimitDialog() {
        val bottomSheet = ConfigureGapLimitBottomSheetFragment.show(
            gapLimit = viewModel.walletGapLimit(),
            fragmentManager = supportFragmentManager
        )

        bottomSheet.listener = {
            viewModel.updateGapLimit(it)
        }
    }

    private fun showForceRefreshWalletDialog() {
        if (viewModel.isAssistedWallet()) {
            NCWarningDialog(this).showDialog(title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_force_refresh_desc),
                onYesClick = {
                    viewModel.forceRefreshWallet()
                })
        } else {
            NCWarningDialog(this).showDialog(title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_force_refresh_free_user_desc),
                onYesClick = {
                    viewModel.forceRefreshWallet()
                })
        }
    }

    private fun handleDeleteWallet() {
        if (viewModel.isAssistedWallet() || viewModel.isServerWallet()) {
            showReEnterPassword(TargetAction.DELETE_WALLET)
        } else if (viewModel.isSharedWallet()) {
            NCWarningDialog(this).showDialog(
                message = getString(R.string.nc_delete_collaborative_wallet),
                onYesClick = { viewModel.handleDeleteWallet() }
            )
        } else if (viewModel.isHotWalletNeedBackup() && !viewModel.isSignerDeleted()) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_delete_hot_wallet_need_backup),
                onYesClick = { }
            )
        } else {
            NCDeleteConfirmationDialog(this).showDialog(
                message = getString(R.string.nc_are_you_sure_to_delete_wallet),
                onConfirmed = {
                    if (it.trim() == CONFIRMATION_TEXT) {
                        viewModel.handleDeleteWallet()
                    } else {
                        NCToastMessage(this).showWarning(getString(R.string.nc_incorrect))
                    }
                }
            )
        }
    }

    private fun observeEvent() {
        flowObserver(flow = viewModel.event, collector = ::handleEvent)
    }

    override fun handleSharedEvent(event: UploadConfigurationEvent) {
        super.handleSharedEvent(event)
        if (event is UploadConfigurationEvent.ExportColdcardSuccess
            && event.filePath.isNullOrEmpty().not()
        ) {
            shareConfigurationFile(event.filePath.orEmpty())
        }
    }

    private fun handleEvent(event: WalletConfigEvent) {
        when (event) {
            UpdateNameSuccessEvent -> showEditWalletSuccess()
            is UpdateNameErrorEvent -> NCToastMessage(this).showWarning(event.message)
            WalletConfigEvent.DeleteWalletSuccess -> walletDeleted()
            is WalletConfigEvent.WalletDetailsError -> onGetWalletError(event)
            is WalletConfigEvent.VerifyPasswordSuccess -> openServerKeyDetail(event)
            is WalletConfigEvent.Loading -> showOrHideLoading(event.isLoading)
            is WalletConfigEvent.Error -> NCToastMessage(this).showError(message = event.message)
            WalletConfigEvent.ForceRefreshWalletSuccess -> {
                NcToastManager.scheduleShowMessage(message = getString(R.string.nc_force_refresh_success))
                setResult(RESULT_OK, Intent().apply {
                    putExtra(EXTRA_WALLET_ACTION, WalletConfigAction.FORCE_REFRESH)
                })
                finish()
            }

            is WalletConfigEvent.CalculateRequiredSignaturesSuccess -> {
                navigator.openWalletAuthentication(
                    walletId = event.walletId,
                    userData = "",
                    requiredSignatures = event.requiredSignatures,
                    type = event.type,
                    launcher = launcher,
                    activityContext = this
                )
            }

            WalletConfigEvent.DeleteAssistedWalletSuccess -> walletDeleted()
            is WalletConfigEvent.UploadWalletConfigEvent -> shareConfigurationFile(event.filePath)
            is WalletConfigEvent.ExportTxCoinControlSuccess -> shareConfigurationFile(event.filePath)
            WalletConfigEvent.ImportTxCoinControlSuccess -> NCToastMessage(this).showMessage(
                message = getString(
                    R.string.nc_import_completed
                )
            )

            WalletConfigEvent.UpdateGapLimitSuccessEvent -> NCToastMessage(this).showMessage(
                message = getString(
                    R.string.nc_gap_limit_updated
                )
            )

            WalletConfigEvent.OpenReplaceKey -> navigator.openMembershipActivity(
                activityContext = this,
                groupStep = MembershipStage.REPLACE_KEY,
                walletId = args.walletId,
                groupId = viewModel.getGroupId().orEmpty(),
            )

            is WalletConfigEvent.ExportInvoiceSuccess -> {
                if (isCancelExportInvoice) {
                    return
                }
                if (event.filePath.isEmpty().not()) {
                    shareConfigurationFile(event.filePath)
                }
            }

            is WalletConfigEvent.SaveLocalFile -> {
                if (event.isSuccess) {
                    NCToastMessage(this).showMessage(getString(R.string.nc_save_file_success))
                } else {
                    NCToastMessage(this).showError(getString(R.string.nc_save_file_failed))
                }
            }
        }
    }

    private fun openServerKeyDetail(event: WalletConfigEvent.VerifyPasswordSuccess) {
        if (!event.groupId.isNullOrEmpty()) {
            CosigningPolicyActivity.start(
                activity = this,
                signer = event.signer,
                token = event.token,
                walletId = args.walletId,
                groupId = event.groupId,
            )
        } else {
            CosigningPolicyActivity.start(
                activity = this,
                keyPolicy = args.keyPolicy,
                signer = event.signer,
                token = event.token,
                walletId = args.walletId,
            )
        }
    }

    private fun onGetWalletError(event: WalletConfigEvent.WalletDetailsError) {
        NCToastMessage(this).showError(event.message)
    }

    private fun shareConfigurationFile(filePath: String) {
        controller.shareFile(filePath)
    }

    private fun walletDeleted() {
        if (viewModel.isAssistedWallet()) {
            NcToastManager.scheduleShowMessage(
                message = getString(
                    R.string.nc_delete_assisted_wallet_success,
                    viewModel.walletName()
                )
            )
        } else {
            NCToastMessage(this).showMessage(getString(R.string.nc_wallet_delete_wallet_success))
        }
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_WALLET_ACTION, WalletConfigAction.DELETE)
        })
        ActivityManager.popUntilRoot()
    }

    private fun handleExportBSMS() {
        viewModel.handleExportBSMS()
    }

    override fun shareFile() {
        handleExportBSMS()
    }

    override fun saveFileToLocal() {
        viewModel.saveBSMSToLocal()
    }

    private fun showMoreOptions() {
        var options = mutableListOf<SheetOption>()
        if (viewModel.getRole().isFacilitatorAdmin) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_FORCE_REFRESH_WALLET,
                    R.drawable.ic_cached,
                    R.string.nc_force_refresh
                )
            )
        } else {
            options = mutableListOf(
                SheetOption(
                    SheetOptionType.TYPE_SAVE_WALLET_CONFIG,
                    R.drawable.ic_backup,
                    R.string.nc_wallet_save_wallet_configuration
                ),
                SheetOption(
                    SheetOptionType.TYPE_EXPORT_TX_COIN_CONTROL,
                    R.drawable.ic_export,
                    R.string.nc_export_labels
                ),
                SheetOption(
                    SheetOptionType.TYPE_IMPORT_TX_COIN_CONTROL,
                    R.drawable.ic_import,
                    R.string.nc_import_labels
                ),
                SheetOption(
                    SheetOptionType.TYPE_FORCE_REFRESH_WALLET,
                    R.drawable.ic_cached,
                    R.string.nc_force_refresh
                ),
                SheetOption(
                    SheetOptionType.TYPE_CONFIGURE_GAP_LIMIT,
                    R.drawable.ic_gap_limit,
                    R.string.nc_configure_gap_limit
                )
            )
            if (viewModel.getRole().isKeyHolderLimited.not() &&
                viewModel.getRole().isFacilitatorAdmin.not() &&
                viewModel.getTransactions().isEmpty().not()
            ) {
                options.add(
                    3, SheetOption(
                        SheetOptionType.TYPE_EXPORT_TX_INVOICES,
                        R.drawable.ic_export_invoices,
                        R.string.nc_export_transaction_invoices
                    )
                )
            }
            if (viewModel.getRole().isMasterOrAdmin) {
                options.add(
                    SheetOption(
                        SheetOptionType.TYPE_EDIT_PRIMARY_OWNER,
                        R.drawable.ic_account_member,
                        R.string.nc_edit_primary_owner,
                    ),
                )
            }

            if (!viewModel.isReplacedOrLocked()) {
                options.add(
                    SheetOption(
                        SheetOptionType.TYPE_REPLACE_KEY,
                        R.drawable.ic_hardware_key,
                        R.string.nc_replace_keys
                    )
                )
                if (viewModel.getRole().isKeyHolderWithoutKeyHolderLimited || viewModel.getGroupId()
                        .isNullOrEmpty()
                ) {
                    options.add(
                        SheetOption(
                            SheetOptionType.TYPE_ROLL_OVER_ANOTHER_WALLET,
                            R.drawable.ic_wallet_info,
                            R.string.nc_roll_funds_over_another_wallet
                        )
                    )
                }
            }

            if (viewModel.isShowDeleteWallet()) {
                options.add(
                    SheetOption(
                        SheetOptionType.TYPE_DELETE_WALLET,
                        R.drawable.ic_delete_red,
                        R.string.nc_wallet_delete_wallet,
                        isDeleted = true
                    ),
                )
            }
        }

        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showSaveWalletConfigurationOption() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_select_export_format),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_BSMS,
                    stringId = R.string.nc_bsms
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_TO_COLD_CARD,
                    stringId = R.string.nc_coldcard
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_PORTAL,
                    stringId = R.string.nc_portal
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_AS_QR,
                    stringId = R.string.nc_text_wallet_qr_code
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showImportFormatOption() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_select_import_format),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_NUNCHUK,
                    stringId = R.string.nc_nunchuk
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_BIP329,
                    stringId = R.string.nc_bip329
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showExportFormatOption() {
        BottomSheetOption.newInstance(
            title = getString(R.string.nc_select_export_format),
            options = listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_NUNCHUK,
                    stringId = R.string.nc_nunchuk
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_BIP329,
                    stringId = R.string.nc_bip329
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showEditWalletSuccess() {
        binding.root.post {
            NCToastMessage(this).show(R.string.nc_text_change_wallet_success)
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_WALLET_ACTION, WalletConfigAction.UPDATE_NAME)
            })
        }
    }

    private fun showReEnterPassword(
        targetAction: TargetAction,
        signer: SignerModel? = null,
    ) {
        NCDeleteConfirmationDialog(this).showDialog(
            title = getString(R.string.nc_re_enter_password),
            isMaskInput = true,
            message = getString(R.string.nc_enter_your_password_desc),
            onConfirmed = { password ->
                when (targetAction) {
                    TargetAction.DELETE_WALLET -> viewModel.verifyPasswordToDeleteAssistedWallet(
                        password
                    )

                    TargetAction.UPDATE_SERVER_KEY -> viewModel.verifyPassword(password, signer!!)
                    TargetAction.REPLACE_KEYS -> viewModel.verifyPasswordToReplaceKey(password)
                    else -> Unit
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMPORT_NUNCHUK_REQ) {
                intent?.data?.let {
                    getFileFromUri(contentResolver, it, cacheDir)
                }?.absolutePath?.let(viewModel::importCoinControlNunchuk)
            } else if (requestCode == IMPORT_BIP329_REQ) {
                intent?.data?.let {
                    getFileFromUri(contentResolver, it, cacheDir)
                }?.absolutePath?.let(viewModel::importCoinControlBIP329)
            }
        }
    }

    companion object {
        private const val IMPORT_NUNCHUK_REQ = 2
        private const val IMPORT_BIP329_REQ = 3
        private const val CONFIRMATION_TEXT = "DELETE"
        const val EXTRA_WALLET_ACTION = "action"

        fun buildIntent(activityContext: Context, walletId: String, keyPolicy: KeyPolicy?) =
            WalletConfigArgs(walletId = walletId, keyPolicy).buildIntent(
                activityContext
            )
    }
}

enum class WalletConfigAction {
    DELETE, UPDATE_NAME, FORCE_REFRESH
}