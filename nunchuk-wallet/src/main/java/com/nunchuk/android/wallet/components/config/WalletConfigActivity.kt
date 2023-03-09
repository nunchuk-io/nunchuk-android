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

package com.nunchuk.android.wallet.components.config

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.share.wallet.bindWalletConfiguration
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent
import com.nunchuk.android.wallet.databinding.ActivityWalletConfigBinding
import com.nunchuk.android.wallet.util.toReadableString
import com.nunchuk.android.widget.NCDeleteConfirmationDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletConfigActivity : BaseWalletConfigActivity<ActivityWalletConfigBinding>() {

    private val viewModel: WalletConfigViewModel by viewModels()

    private val controller: IntentSharingController by lazy(LazyThreadSafetyMode.NONE) {
        IntentSharingController.from(
            this
        )
    }

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

    private val args: WalletConfigArgs by lazy { WalletConfigArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityWalletConfigBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.walletId)
        sharedViewModel.init(args.walletId)
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            SheetOptionType.TYPE_EXPORT_AS_QR -> openDynamicQRScreen(sharedViewModel.walletId)
            SheetOptionType.TYPE_DELETE_WALLET -> handleDeleteWallet()
            SheetOptionType.TYPE_EXPORT_TO_COLD_CARD -> showExportColdcardOptions()
            SheetOptionType.TYPE_FORCE_REFRESH_WALLET -> showForceRefreshWalletDialog()
        }
    }

    private fun showForceRefreshWalletDialog() {
        NCWarningDialog(this).showDialog(title = getString(R.string.nc_confirmation),
            message = getString(R.string.nc_force_refresh_desc),
            onYesClick = {
                viewModel.forceRefreshWallet()
            })
    }

    private fun handleDeleteWallet() {
        if (viewModel.isAssistedWallet()) {
            showReEnterPassword(true, "")
        } else if (viewModel.isSharedWallet()) {
            NCWarningDialog(this).showDialog(
                message = getString(R.string.nc_delete_collaborative_wallet),
                onYesClick = { viewModel.handleDeleteWallet() }
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
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
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
        }
    }

    private fun openServerKeyDetail(event: WalletConfigEvent.VerifyPasswordSuccess) {
        CosigningPolicyActivity.start(
            activity = this,
            keyPolicy = args.keyPolicy,
            xfp = event.xfp,
            token = event.token,
            walletId = args.walletId,
        )
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

    private fun handleState(state: WalletConfigState) {
        val wallet = state.walletExtended.wallet
        binding.walletName.text = wallet.name

        binding.configuration.bindWalletConfiguration(wallet)

        binding.walletType.text =
            (if (wallet.escrow) WalletType.ESCROW else WalletType.MULTI_SIG).toReadableString(this)
        binding.addressType.text = wallet.addressType.toReadableString(this)
        binding.shareIcon.isVisible = state.walletExtended.isShared || state.isAssistedWallet
        if (state.isAssistedWallet) {
            binding.shareIcon.text = getString(R.string.nc_assisted)
        } else {
            binding.shareIcon.text = getString(R.string.nc_text_shared)
        }
        SignersViewBinder(
            container = binding.signersContainer,
            signers = state.signers,
            isInactiveAssistedWallet = viewModel.isInactiveAssistedWallet()
        ) {
            showReEnterPassword(false, it.fingerPrint)
        }.bindItems()
    }

    private fun setupViews() {
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_more) {
                showMoreOptions()
            }
            false
        }
        binding.walletName.setOnClickListener { onEditClicked() }
        binding.btnDone.setOnClickListener {
            finish()
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun showMoreOptions() {
        val options = mutableListOf(
            SheetOption(
                SheetOptionType.TYPE_EXPORT_AS_QR,
                R.drawable.ic_qr,
                R.string.nc_show_as_qr_code
            ),
            SheetOption(
                SheetOptionType.TYPE_EXPORT_TO_COLD_CARD,
                R.drawable.ic_export,
                R.string.nc_wallet_export_coldcard
            ),
        )
        if (viewModel.isAssistedWallet()) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_FORCE_REFRESH_WALLET,
                    R.drawable.ic_cached,
                    R.string.nc_force_refresh
                )
            )
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

        val bottomSheet = BottomSheetOption.newInstance(options)
        bottomSheet.show(supportFragmentManager, "BottomSheetOption")
    }

    private fun onEditClicked() {
        val bottomSheet = WalletUpdateBottomSheet.show(
            fragmentManager = supportFragmentManager,
            walletName = binding.walletName.text.toString()
        )

        bottomSheet.setListener(viewModel::handleEditCompleteEvent)
    }

    private fun showEditWalletSuccess() {
        binding.root.post {
            NCToastMessage(this).show(R.string.nc_text_change_wallet_success)
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(EXTRA_WALLET_ACTION, WalletConfigAction.UPDATE_NAME)
            })
        }
    }

    private fun showReEnterPassword(isDeletedWalletFlow: Boolean, fingerPrint: String) {
        NCDeleteConfirmationDialog(this).showDialog(
            title = getString(R.string.nc_re_enter_password),
            isMaskInput = true,
            message = getString(R.string.nc_enter_your_password_desc),
            onConfirmed = { password ->
                if (isDeletedWalletFlow) {
                    viewModel.verifyPasswordToDeleteAssistedWallet(password)
                } else {
                    viewModel.verifyPassword(password, fingerPrint)
                }
            }
        )
    }

    companion object {
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