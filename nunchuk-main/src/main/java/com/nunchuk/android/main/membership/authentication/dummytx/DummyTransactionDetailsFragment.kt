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

package com.nunchuk.android.main.membership.authentication.dummytx

import android.app.Activity
import android.content.Intent
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.base.BaseShareSaveFileFragment
import com.nunchuk.android.core.domain.data.SignTransaction
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.BasePortalActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.core.util.showWarning
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationActivityArgs
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationEvent
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationViewModel
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.isInheritanceFlow
import com.nunchuk.android.nav.args.MembershipArgs
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.transaction.components.details.TransactionDetailView
import com.nunchuk.android.transaction.components.details.TransactionDetailsState
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DummyTransactionDetailsFragment : BaseShareSaveFileFragment<ViewBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var pushEventManager: PushEventManager

    private val viewModel: DummyTransactionDetailsViewModel by viewModels()
    private val walletAuthenticationViewModel: WalletAuthenticationViewModel by activityViewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.importTransactionViaFile(walletAuthenticationViewModel.getWalletId(), it)
            }
        }

    private val importTxLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null && it.resultCode == Activity.RESULT_OK) {
                val transaction = data.parcelable<Transaction>(GlobalResultKey.TRANSACTION_EXTRA)
                    ?: return@registerForActivityResult
                walletAuthenticationViewModel.handleImportAirgapTransaction(transaction)
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): ViewBinding = ViewBinding {
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val walletAuthenticationUiState by walletAuthenticationViewModel.state.collectAsStateWithLifecycle()
                val miniscriptUiState by viewModel.miniscriptState.collectAsStateWithLifecycle()

                val transaction = walletAuthenticationUiState.transaction
                if (transaction != null) {
                    TransactionDetailView(
                        isDummyTx = true,
                        walletId = viewModel.args.walletId,
                        txId = "",
                        state = TransactionDetailsState(
                            transaction = transaction.copy(
                                signers = walletAuthenticationUiState.signatures.mapValues { true },
                                // fake number of required signatures
                                m = walletAuthenticationUiState.pendingSignature + walletAuthenticationUiState.signatures.size
                            ),
                            signers = walletAuthenticationUiState.walletSigner,
                            enabledSigners = walletAuthenticationUiState.enabledSigners
                        ),
                        miniscriptUiState = miniscriptUiState.copy(
                            signedSigners = walletAuthenticationUiState.signatures.mapValues { true }
                        ),
                        onSignClick = {
                            walletAuthenticationViewModel.onSignerSelect(it)
                        },
                        onShowMore = ::handleMenuMore
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvent()
    }

    override fun onOptionClicked(option: SheetOption) {
        super.onOptionClicked(option)
        when (option.type) {
            TransactionOption.EXPORT_TRANSACTION.ordinal -> showExportTransactionOptions()
            TransactionOption.IMPORT_TRANSACTION.ordinal -> showImportTransactionOptions()
            SheetOptionType.TYPE_EXPORT_QR -> openExportTransactionScreen(false)
            SheetOptionType.TYPE_EXPORT_BBQR -> openExportTransactionScreen(true)
            SheetOptionType.TYPE_EXPORT_FILE -> showSaveShareOption()

            SheetOptionType.TYPE_IMPORT_QR -> openImportTransactionScreen()
            SheetOptionType.TYPE_IMPORT_FILE -> importFileLauncher.launch("*/*")
            SheetOptionType.TYPE_FORCE_SYNC_DUMMY_TX -> walletAuthenticationViewModel.uploadSignaturesFromLocalIfNeeded(
                true
            )
        }
    }

    override fun shareFile() {
        super.shareFile()
        viewModel.exportTransactionToFile(
            walletAuthenticationViewModel.getDataToSign()
        )
    }

    override fun saveFileToLocal() {
        super.saveFileToLocal()
        viewModel.saveLocalFile(
            walletAuthenticationViewModel.getDataToSign()
        )
    }

    private fun observeEvent() {
        flowObserver(walletAuthenticationViewModel.state.filter { it.transaction != null }) {
            viewModel.loadWallet(it.transaction!!, it.enabledSigners)
        }
        flowObserver(viewModel.event) {
            when (it) {
                is DummyTransactionDetailEvent.ExportToFileSuccess -> shareTransactionFile(it.filePath)
                is DummyTransactionDetailEvent.ImportTransactionSuccess -> walletAuthenticationViewModel.handleImportAirgapTransaction(
                    it.transaction ?: return@flowObserver
                )

                is DummyTransactionDetailEvent.LoadingEvent -> showOrHideLoading(it.isLoading)
                is DummyTransactionDetailEvent.TransactionError -> showError(it.error)
                is DummyTransactionDetailEvent.SaveLocalFile -> showSaveFileState(it.isSuccess)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            walletAuthenticationViewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is WalletAuthenticationEvent.SignDummyTxSuccess -> {
                            if (isOpenGroupDashboard()) {
                                openGroupDashboard()
                            } else {
                                requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                    putExtra(
                                        GlobalResultKey.SIGNATURE_EXTRA,
                                        HashMap(event.signatures)
                                    )
                                    putExtras(walletAuthenticationViewModel.getDummyTransactionExtra())
                                })
                                requireActivity().finish()
                            }
                        }

                        is WalletAuthenticationEvent.ScanTapSigner -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                        )

                        WalletAuthenticationEvent.ScanColdCard -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION
                        )

                        is WalletAuthenticationEvent.ProcessFailure -> showError(event.message)
                        WalletAuthenticationEvent.GenerateColdcardHealthMessagesSuccess -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE
                        )

                        is WalletAuthenticationEvent.NfcLoading -> showOrHideNfcLoading(
                            event.isLoading,
                            event.isColdCard
                        )

                        WalletAuthenticationEvent.ShowAirgapOption -> handleMenuMore()
                        WalletAuthenticationEvent.ExportTransactionToColdcardSuccess -> handleExportToColdcardSuccess()
                        WalletAuthenticationEvent.CanNotSignDummyTx -> showError(getString(R.string.nc_can_not_sign_please_try_again))
                        WalletAuthenticationEvent.CanNotSignHardwareKey -> showError(getString(R.string.nc_use_desktop_app_to_sign))
                        is WalletAuthenticationEvent.UploadSignatureSuccess -> openGroupDashboard()

                        is WalletAuthenticationEvent.ForceSyncSuccess -> if (event.isSuccess) showSuccess(
                            getString(R.string.nc_transaction_updated)
                        )
                        else showError(getString(R.string.nc_transaction_not_updated))

                        WalletAuthenticationEvent.PromptPassphrase -> NCInputDialog(requireActivity()).showDialog(
                            title = getString(com.nunchuk.android.transaction.R.string.nc_transaction_enter_passphrase),
                            onConfirmed = {
                                walletAuthenticationViewModel.handlePassphrase(it)
                            }
                        )

                        is WalletAuthenticationEvent.RequestSignPortal -> {
                            (requireActivity() as BasePortalActivity<*>).handlePortalAction(
                                SignTransaction(fingerPrint = event.fingerprint, psbt = event.psbt)
                            )
                        }

                        is WalletAuthenticationEvent.NoInternetConnectionToSign -> showWarning(
                            getString(R.string.nc_no_internet_connection_sign_dummy_tx)
                        )

                        is WalletAuthenticationEvent.NoInternetConnectionForceSync -> showError(
                            getString(R.string.nc_no_internet_connection_force_sync)
                        )

                        is WalletAuthenticationEvent.SignFailed -> {
                            if (!nfcViewModel.handleNfcError(event.e)) {
                                handleSignedFailed(event.singleSigner)
                            }
                        }

                        is WalletAuthenticationEvent.Loading,
                        is WalletAuthenticationEvent.FinalizeDummyTxSuccess,
                        is WalletAuthenticationEvent.ShowError,
                            -> Unit

                        WalletAuthenticationEvent.NoSignatureDetected -> showWarning(getString(R.string.nc_no_new_signatures_detected))
                    }
                }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }) { info ->
            walletAuthenticationViewModel.getInteractSingleSigner()?.let {
                walletAuthenticationViewModel.handleTapSignerSignCheckMessage(
                    it,
                    info,
                    nfcViewModel.inputCvc.orEmpty()
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION }) { scanInfo ->
            walletAuthenticationViewModel.handleExportTransactionToMk4(
                Ndef.get(scanInfo.tag) ?: return@flowObserver
            )
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE }) {
            walletAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                walletAuthenticationViewModel.generateSignatureFromColdCardPsbt(signer, it.records)
            }
            nfcViewModel.clearScanInfo()
        }
    }

    private fun isOpenGroupDashboard(): Boolean {
        val args by requireActivity().navArgs<WalletAuthenticationActivityArgs>()
        return !args.dummyTransactionId.isNullOrEmpty() && args.action != TargetAction.CLAIM_KEY.name
                && walletAuthenticationViewModel.getDummyTransactionType().isInheritanceFlow().not()
                && walletAuthenticationViewModel.getDummyTransactionType() != DummyTransactionType.KEY_RECOVERY_REQUEST
                && walletAuthenticationViewModel.getDummyTransactionType() != DummyTransactionType.CHANGE_EMAIL
    }

    private fun openGroupDashboard(message: String = walletAuthenticationViewModel.signedSuccessMessage) {
        val args by requireActivity().navArgs<WalletAuthenticationActivityArgs>()
        navigator.openGroupDashboardScreen(
            activityContext = requireActivity(),
            groupId = args.groupId.orEmpty(),
            walletId = args.walletId,
            message = message
        )
        ActivityManager.popUntilRoot()
    }

    private fun handleSignedFailed(singleSigner: SingleSigner) {
        val activityArgs: WalletAuthenticationActivityArgs by requireActivity().navArgs()
        if (activityArgs.action == TargetAction.CLAIM_KEY.name
            && (singleSigner.type == SignerType.COLDCARD_NFC
                    || (singleSigner.type == SignerType.HARDWARE && singleSigner.tags.contains(
                SignerTag.COLDCARD
            )) || singleSigner.type == SignerType.AIRGAP)
        ) {
            NCWarningDialog(requireActivity())
                .showDialog(
                    title = getString(R.string.nc_text_info),
                    message = getString(R.string.nc_claim_key_failed),
                    btnPositive = getString(R.string.nc_try_signing_again),
                    btnNegative = getString(R.string.nc_register_wallet),
                    btnNeutral = getString(R.string.nc_text_do_this_later),
                    onPositiveClick = {},
                    onNegativeClick = {
                        lifecycleScope.launch {
                            pushEventManager.push(PushEvent.DismissGroupWalletCreatedAlert)
                        }
                        startActivity(
                            MembershipActivity.openRegisterWalletIntent(
                                activity = requireActivity(),
                                args = MembershipArgs(
                                    groupStep = MembershipStage.REGISTER_WALLET,
                                    walletId = activityArgs.walletId,
                                    groupId = activityArgs.groupId.orEmpty()
                                )
                            )
                        )
                    },
                    onNeutralClick = {
                        openGroupDashboard("")
                    },
                )
        }
    }

    private fun shareTransactionFile(filePath: String) {
        controller.shareFile(filePath)
    }

    private fun handleExportToColdcardSuccess() {
        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE)
        showSuccess(getString(com.nunchuk.android.transaction.R.string.nc_transaction_exported))
    }

    private fun handleMenuMore() {
        val args by requireActivity().navArgs<WalletAuthenticationActivityArgs>()
        val options = mutableListOf(
            SheetOption(
                type = TransactionOption.IMPORT_TRANSACTION.ordinal,
                resId = R.drawable.ic_import,
                label = getString(R.string.nc_transaction_import_signature),
            ),
            SheetOption(
                type = TransactionOption.EXPORT_TRANSACTION.ordinal,
                resId = R.drawable.ic_export,
                label = getString(R.string.nc_transaction_export_transaction),
            ),
        )
        if (!args.dummyTransactionId.isNullOrEmpty()) {
            options.add(
                SheetOption(
                    type = SheetOptionType.TYPE_FORCE_SYNC_DUMMY_TX,
                    resId = R.drawable.ic_sync,
                    label = getString(R.string.nc_transaction_force_sync),
                )
            )
        }
        BottomSheetOption.newInstance(options).show(childFragmentManager, "BottomSheetOption")
    }

    private fun openExportTransactionScreen(isBBQR: Boolean) {
        navigator.openExportTransactionScreen(
            launcher = importTxLauncher,
            activityContext = requireActivity(),
            txToSign = walletAuthenticationViewModel.getDataToSign(),
            isDummyTx = true,
            walletId = walletAuthenticationViewModel.getWalletId(),
            isBBQR = isBBQR
        )
    }

    private fun openImportTransactionScreen() {
        navigator.openImportTransactionScreen(
            launcher = importTxLauncher,
            activityContext = requireActivity(),
            walletId = walletAuthenticationViewModel.getWalletId(),
            isDummyTx = true
        )
    }

    private fun showExportTransactionOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_QR,
                    resId = com.nunchuk.android.transaction.R.drawable.ic_qr,
                    label = getString(com.nunchuk.android.transaction.R.string.nc_export_via_qr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_BBQR,
                    resId = com.nunchuk.android.transaction.R.drawable.ic_qr,
                    label = getString(com.nunchuk.android.transaction.R.string.nc_export_via_bbqr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_FILE,
                    resId = com.nunchuk.android.transaction.R.drawable.ic_export,
                    label = getString(com.nunchuk.android.transaction.R.string.nc_export_via_file),
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showImportTransactionOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_QR,
                    resId = com.nunchuk.android.transaction.R.drawable.ic_qr,
                    label = getString(com.nunchuk.android.transaction.R.string.nc_import_via_qr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_FILE,
                    resId = com.nunchuk.android.transaction.R.drawable.ic_import,
                    label = getString(com.nunchuk.android.transaction.R.string.nc_import_via_file),
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun showError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).showError(message)
    }
}