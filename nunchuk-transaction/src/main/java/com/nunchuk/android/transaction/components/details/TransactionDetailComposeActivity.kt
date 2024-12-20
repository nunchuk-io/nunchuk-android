package com.nunchuk.android.transaction.components.details

import android.app.Activity
import android.content.Intent
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.domain.data.SignTransaction
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.BaseComposePortalActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_EXPORT_TRANSACTION
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_IMPORT_SIGNATURE
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_SIGN_TRANSACTION
import com.nunchuk.android.core.nfc.PortalDeviceEvent
import com.nunchuk.android.core.nfc.RbfType
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.BottomSheetTooltip
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.sheet.input.InputBottomSheet
import com.nunchuk.android.core.sheet.input.InputBottomSheetListener
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.wallet.InvoiceInfo
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.model.TransactionOption.CANCEL
import com.nunchuk.android.share.model.TransactionOption.COPY_RAW_TRANSACTION_HEX
import com.nunchuk.android.share.model.TransactionOption.COPY_TRANSACTION_ID
import com.nunchuk.android.share.model.TransactionOption.EXPORT_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.IMPORT_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.REMOVE_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.REPLACE_BY_FEE
import com.nunchuk.android.share.model.TransactionOption.SCHEDULE_BROADCAST
import com.nunchuk.android.share.model.TransactionOption.SHOW_INVOICE
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.BroadcastTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.CancelScheduleBroadcastTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.DeleteTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ExportToFileSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ExportTransactionToMk4Success
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ImportTransactionFromMk4Success
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ImportTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.LoadingEvent
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.NfcLoadingEvent
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.NoInternetConnection
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.PromptInputPassphrase
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.PromptTransactionOptions
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.SignTransactionSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.TransactionDetailsError
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.TransactionError
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.UpdateTransactionMemoFailed
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.UpdateTransactionMemoSuccess
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.ViewBlockchainExplorer
import com.nunchuk.android.transaction.components.details.fee.ReplaceFeeArgs
import com.nunchuk.android.transaction.components.export.ExportTransactionActivity
import com.nunchuk.android.transaction.components.invoice.InvoiceActivity
import com.nunchuk.android.transaction.components.schedule.ScheduleBroadcastTransactionActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus.PENDING_CONFIRMATION
import com.nunchuk.android.type.TransactionStatus.READY_TO_BROADCAST
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.toInvoiceInfo
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionDetailComposeActivity : BaseComposePortalActivity(), InputBottomSheetListener,
    BottomSheetOptionListener {
    private val viewModel: TransactionDetailsViewModel by viewModels()
    private var shouldReload: Boolean = true

    private val args: TransactionDetailsArgs by lazy { TransactionDetailsArgs.deserializeFrom(intent) }
    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    private val scheduleBroadcastLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null && it.resultCode == Activity.RESULT_OK) {
                viewModel.updateServerTransaction(
                    data.parcelable(
                        ScheduleBroadcastTransactionActivity.EXTRA_SCHEDULE_BROADCAST_TIME
                    )
                )
                NCToastMessage(this).showMessage(getString(R.string.nc_broadcast_has_been_scheduled))
            }
        }

    private val replaceByFeeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val result = ReplaceFeeArgs.deserializeFrom(data)
                navigator.openTransactionDetailsScreen(
                    activityContext = this,
                    walletId = result.walletId,
                    txId = result.transaction.txId,
                )
                NcToastManager.scheduleShowMessage(getString(R.string.nc_the_transaction_has_been_replaced))
                finish()
            }
        }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.importTransactionViaFile(args.walletId, it)
            }
        }

    private val coinLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.getAllCoins()
                viewModel.getAllTags()
            }
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isNfcIntent(intent)) {
            shouldReload = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(
            walletId = args.walletId,
            txId = args.txId,
            initEventId = args.initEventId,
            roomId = args.roomId,
            transaction = args.transaction
        )

        enableEdgeToEdge()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            TransactionDetailView(
                args = args,
                state = state,
                onShowMore = { handleMenuMore() },
                onSignClick = { signer ->
                    viewModel.setCurrentSigner(signer)
                    when {
                        signer.type == SignerType.COLDCARD_NFC
                                || signer.type == SignerType.HARDWARE && signer.tags.contains(
                            SignerTag.COLDCARD
                        ) -> showSignByMk4Options()

                        signer.type == SignerType.NFC -> {
                            startNfcFlow(REQUEST_NFC_SIGN_TRANSACTION)
                        }

                        signer.type == SignerType.AIRGAP || signer.type == SignerType.UNKNOWN -> showSignByAirgapOptions()
                        signer.type == SignerType.HARDWARE -> showError(getString(R.string.nc_use_desktop_app_to_sign))
                        signer.type == SignerType.PORTAL_NFC -> handlePortalAction(
                            SignTransaction(
                                signer.fingerPrint,
                                viewModel.getTransaction().psbt
                            )
                        )

                        else -> viewModel.handleSignSoftwareKey(signer)
                    }
                },
                onBroadcastClick = viewModel::handleBroadcastEvent,
                onViewOnBlockExplorer = viewModel::handleViewBlockchainEvent,
                onManageCoinClick = {
                    when (viewModel.coins().size) {
                        1 -> navigator.openCoinDetail(
                            launcher = coinLauncher,
                            context = this,
                            walletId = args.walletId,
                            viewModel.coins().first()
                        )

                        else -> navigator.openCoinList(
                            launcher = coinLauncher,
                            context = this,
                            walletId = args.walletId,
                            txId = args.txId
                        )
                    }
                },
                onEditNote = {
                    InputBottomSheet.show(
                        fragmentManager = supportFragmentManager,
                        currentInput = viewModel.getTransaction().memo,
                        title = getString(R.string.nc_transaction_note)
                    )
                },
                onShowFeeTooltip = {
                    showEstimatedFeeTooltip()
                },
                onCopyText = { handleCopyContent(it) },
            )
        }

        if (args.isInheritanceClaimingFlow) {
            showInheritanceClaimingDialog()
        }
        if (args.errorMessage.isBlank().not()) {
            showError(message = args.errorMessage)
        }
        if (args.isCancelBroadcast) {
            viewModel.cancelScheduleBroadcast()
        }
        observeEvent()
    }


    override fun onHandledPortalAction(event: PortalDeviceEvent) {
        if (event is PortalDeviceEvent.SignTransactionSuccess) {
            viewModel.handleSignPortalKey(event.psbt)
        }
    }

    private fun showEstimatedFeeTooltip() {
        BottomSheetTooltip.newInstance(
            title = getString(R.string.nc_text_info),
            message = getString(R.string.nc_estimated_fee_tooltip),
        ).show(supportFragmentManager, "BottomSheetTooltip")
    }

    private fun showInheritanceClaimingDialog() {
        NCInfoDialog(this).showDialog(
            title = getString(R.string.nc_congratulation),
            message = getString(R.string.nc_your_inheritance_has_been_claimed),
        ).show()
    }

    override fun onInputDone(newInput: String) {
        viewModel.updateTransactionMemo(newInput)
    }

    override fun onResume() {
        super.onResume()
        if (shouldReload) {
            viewModel.getTransactionInfo()
        }
        shouldReload = true
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.EXPORT_TX_TO_Mk4 -> startNfcFlow(REQUEST_MK4_EXPORT_TRANSACTION)
            SheetOptionType.IMPORT_TX_FROM_Mk4 -> startNfcFlow(REQUEST_MK4_IMPORT_SIGNATURE)
            IMPORT_TRANSACTION.ordinal -> showImportTransactionOptions()
            EXPORT_TRANSACTION.ordinal -> showExportTransactionOptions()
            SheetOptionType.TYPE_EXPORT_QR -> openExportTransactionScreen(false)
            SheetOptionType.TYPE_EXPORT_BBQR -> openExportTransactionScreen(true)
            SheetOptionType.TYPE_EXPORT_FILE -> viewModel.exportTransactionToFile()
            SheetOptionType.TYPE_IMPORT_QR -> openImportTransactionScreen()
            SheetOptionType.TYPE_IMPORT_FILE -> importFileLauncher.launch("*/*")
        }
    }

    private fun observeEvent() {
        flowObserver(viewModel.event, collector = ::handleEvent)
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_SIGN_TRANSACTION }) {
            viewModel.handleSignByTapSigner(IsoDep.get(it.tag), nfcViewModel.inputCvc.orEmpty())
            nfcViewModel.clearScanInfo()
        }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_MK4_EXPORT_TRANSACTION }) {
            viewModel.handleExportTransactionToMk4(Ndef.get(it.tag) ?: return@flowObserver)
            nfcViewModel.clearScanInfo()
        }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_MK4_IMPORT_SIGNATURE }) {
            viewModel.handleImportTransactionFromMk4(it.records)
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleMenuMore() {
        viewModel.handleMenuMoreEvent()
    }

    private fun isServerBroadcastTime(
        transaction: Transaction,
        serverTransaction: ServerTransaction?
    ): Boolean {
        return serverTransaction != null && transaction.status.canBroadCast() && serverTransaction.type == ServerTransactionType.SCHEDULED && serverTransaction.broadcastTimeInMilis > 0L
    }

    private fun handleEvent(event: TransactionDetailsEvent) {
        when (event) {
            is SignTransactionSuccess -> showSignTransactionSuccess(event)
            is BroadcastTransactionSuccess -> showBroadcastTransactionSuccess(event)
            is DeleteTransactionSuccess -> showTransactionDeleteSuccess(event.isCancel)
            is ViewBlockchainExplorer -> openExternalLink(event.url)
            is TransactionDetailsError -> handleSignError(event)
            is PromptInputPassphrase -> requireInputPassphrase(event.func)
            is PromptTransactionOptions -> promptTransactionOptions(event)
            LoadingEvent -> if (args.isInheritanceClaimingFlow.not()) showLoading()
            is NfcLoadingEvent -> showOrHideNfcLoading(true, event.isColdcard)
            is ExportToFileSuccess -> showExportToFileSuccess(event)
            is TransactionError -> showExportToFileError(event)
            is UpdateTransactionMemoFailed -> handleUpdateTransactionFailed(event)
            is UpdateTransactionMemoSuccess -> handleUpdateTransactionSuccess(event)
            ImportTransactionFromMk4Success -> handleImportTransactionFromMk4Success()
            ExportTransactionToMk4Success -> handleExportTxToMk4Success()
            CancelScheduleBroadcastTransactionSuccess -> NCToastMessage(this).show(
                getString(R.string.nc_schedule_broadcast_has_been_canceled)
            )

            ImportTransactionSuccess -> {
                NCToastMessage(this).show(getString(R.string.nc_transaction_imported))
                handleSignRequestSignature()
            }

            NoInternetConnection -> showError("There is no Internet connection. The platform key co-signing policies will apply once you are connected.")
            is TransactionDetailsEvent.GetRawTransactionSuccess -> handleCopyContent(event.rawTransaction)
            TransactionDetailsEvent.RequestSignatureTransactionSuccess -> {
                hideLoading()
                NCToastMessage(this).show(getString(R.string.nc_request_signature_sent))
            }
        }
    }

    private fun handleExportTxToMk4Success() {
        hideLoading()
        startNfcFlow(REQUEST_MK4_IMPORT_SIGNATURE)
        NCToastMessage(this).show(getString(R.string.nc_transaction_exported))
    }

    private fun handleImportTransactionFromMk4Success() {
        hideLoading()
        NCToastMessage(this).show(getString(R.string.nc_signed_transaction))
        handleSignRequestSignature()
    }

    private fun handleUpdateTransactionFailed(event: UpdateTransactionMemoFailed) {
        hideLoading()
        NCToastMessage(this).showError(event.message)
    }

    private fun handleUpdateTransactionSuccess(event: UpdateTransactionMemoSuccess) {
        setResult(Activity.RESULT_OK)
        hideLoading()
        NCToastMessage(this).show(getString(R.string.nc_private_note_updated))
    }

    private fun handleSignError(event: TransactionDetailsError) {
        hideLoading()
        if (nfcViewModel.handleNfcError(event.e).not()) showError(event.message)
    }

    private fun showExportToFileError(event: TransactionError) {
        hideLoading()
        NCToastMessage(this).showError(event.message)
    }

    private fun showExportToFileSuccess(event: ExportToFileSuccess) {
        hideLoading()
        controller.shareFile(event.filePath)
    }

    private fun handleCancelTransaction() {
        if (viewModel.getTransaction().status.isPending()) {
            NCWarningDialog(this).showDialog(
                title = getString(R.string.nc_text_confirmation),
                message = getString(R.string.nc_transaction_confirmation),
                onYesClick = viewModel::handleDeleteTransactionEvent
            )
        } else {
            navigator.openReplaceTransactionFee(
                replaceByFeeLauncher,
                this,
                walletId = args.walletId,
                transaction = viewModel.getTransaction(),
                type = RbfType.CancelTransaction
            )
        }
    }

    private fun promptTransactionOptions(event: PromptTransactionOptions) {
        TransactionOptionsBottomSheet.show(
            fragmentManager = supportFragmentManager,
            isPending = event.isPendingTransaction,
            isPendingConfirm = event.isPendingConfirm,
            isRejected = event.isRejected,
            isSupportScheduleBroadcast = viewModel.isSupportScheduleBroadcast(),
            isScheduleBroadcast = viewModel.isScheduleBroadcast(),
            canBroadcast = event.canBroadcast,
            isShowRequestSignature = viewModel.getMembers().isNotEmpty(),
            userRole = viewModel.getUserRole().name,
            isReceive = viewModel.getTransaction().isReceive,
            plan = viewModel.getWalletPlan(),
            txStatus = event.txStatus
        ).setListener {
            when (it) {
                CANCEL -> handleCancelTransaction()
                EXPORT_TRANSACTION -> showExportTransactionOptions()
                IMPORT_TRANSACTION -> showImportTransactionOptions()
                REPLACE_BY_FEE -> handleOpenEditFee()
                COPY_TRANSACTION_ID -> handleCopyContent(args.txId)
                SHOW_INVOICE -> InvoiceActivity.navigate(this, getInvoiceInfo())
                COPY_RAW_TRANSACTION_HEX -> viewModel.getRawTransaction()
                REMOVE_TRANSACTION -> viewModel.handleDeleteTransactionEvent(false)
                SCHEDULE_BROADCAST -> if (viewModel.isScheduleBroadcast()) {
                    viewModel.cancelScheduleBroadcast()
                } else {
                    scheduleBroadcastLauncher.launch(
                        ScheduleBroadcastTransactionActivity.buildIntent(
                            this,
                            args.walletId,
                            args.txId,
                        )
                    )
                }

                TransactionOption.REQUEST_SIGNATURE -> {
                    RequestSignatureMemberFragment.show(
                        supportFragmentManager,
                        viewModel.getMembers(),
                    )
                }
            }
        }
    }

    private fun handleOpenEditFee() {
        navigator.openReplaceTransactionFee(
            replaceByFeeLauncher,
            this,
            walletId = args.walletId,
            transaction = viewModel.getTransaction(),
            type = RbfType.ReplaceFee
        )
    }

    private fun openExportTransactionScreen(isBBQR: Boolean) {
        startActivity(
            ExportTransactionActivity.buildIntent(
                activityContext = this,
                walletId = args.walletId,
                txId = args.txId,
                initEventId = viewModel.getInitEventId(),
                masterFingerPrint = viewModel.currentSigner()?.fingerPrint.orEmpty(),
                isBBQR = isBBQR
            )
        )
    }

    private fun openImportTransactionScreen() {
        navigator.openImportTransactionScreen(
            activityContext = this,
            walletId = args.walletId,
            masterFingerPrint = viewModel.currentSigner()?.fingerPrint.orEmpty(),
            initEventId = viewModel.getInitEventId()
        )
    }

    private fun requireInputPassphrase(func: (String) -> Unit) {
        NCInputDialog(this).showDialog(
            title = getString(R.string.nc_transaction_enter_passphrase),
            onConfirmed = func
        )
    }

    private fun showSignTransactionSuccess(event: SignTransactionSuccess) {
        hideLoading()
        if (viewModel.isAssistedWallet().not()) {
            NCToastMessage(this).show(getString(R.string.nc_transaction_signed_successful))
        } else if (event.serverSigned != null) {
            lifecycleScope.launch {
                NCToastMessage(this@TransactionDetailComposeActivity).show(getString(R.string.nc_transaction_signed_successful))
                if (event.status == READY_TO_BROADCAST && event.serverSigned) {
                    delay(3000L)
                    NCToastMessage(this@TransactionDetailComposeActivity).show(getString(R.string.nc_server_key_signed))
                } else if (event.status == PENDING_CONFIRMATION && event.serverSigned) {
                    delay(3000L)
                    NCToastMessage(this@TransactionDetailComposeActivity).show(getString(R.string.nc_server_key_signed))
                    delay(3000L)
                    NCToastMessage(this@TransactionDetailComposeActivity).show(getString(R.string.nc_transaction_has_succesfully_broadcast))
                }
            }
        }
        if (event.roomId.isNotEmpty()) {
            returnActiveRoom()
        }
        handleSignRequestSignature()
    }

    private fun showBroadcastTransactionSuccess(event: BroadcastTransactionSuccess) {
        hideLoading()
        NCToastMessage(this).show(getString(R.string.nc_transaction_broadcast_successful))
        val callback: () -> Unit = {
            if (event.roomId.isEmpty()) {
                finish()
            } else {
                returnActiveRoom()
            }
        }
        if (event.reviewInfo != null) {
            viewModel.showReview(this, event.reviewInfo, callback)
        } else {
            callback()
        }
    }

    private fun showTransactionDeleteSuccess(isCancel: Boolean) {
        setResult(Activity.RESULT_OK)
        finish()
        if (isCancel) {
            NcToastManager.scheduleShowMessage(getString(R.string.nc_transaction_cancelled))
        } else {
            NcToastManager.scheduleShowMessage(getString(R.string.nc_transaction_removed))
        }
    }

    private fun showError(message: String) {
        hideLoading()
        NCToastMessage(this).showError(message)
    }

    private fun returnActiveRoom() {
        finish()
    }

    private fun handleCopyContent(content: String) {
        copyToClipboard(label = "Nunchuk", text = content)
        NCToastMessage(this).showMessage(getString(R.string.nc_copied_to_clipboard))
    }

    private fun handleSignRequestSignature(isBack: Boolean = true) {
        if (args.isRequestSignatureFlow.not()) return
        lifecycleScope.launch {
            pushEventManager.push(PushEvent.SignedTxSuccess(args.txId))
        }
        if (isBack) finish()
    }

    private fun showSignByMk4Options() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.EXPORT_TX_TO_Mk4,
                    resId = R.drawable.ic_export,
                    label = getString(R.string.nc_transaction_export_transaction)
                ),
                SheetOption(
                    type = SheetOptionType.IMPORT_TX_FROM_Mk4,
                    resId = R.drawable.ic_import,
                    label = getString(R.string.nc_import_signature)
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showSignByAirgapOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = IMPORT_TRANSACTION.ordinal,
                    resId = R.drawable.ic_import,
                    label = getString(R.string.nc_transaction_import_signature),
                ),
                SheetOption(
                    type = EXPORT_TRANSACTION.ordinal,
                    resId = R.drawable.ic_export,
                    label = getString(R.string.nc_transaction_export_transaction),
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showExportTransactionOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_QR,
                    resId = R.drawable.ic_qr,
                    label = getString(R.string.nc_export_via_qr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_BBQR,
                    resId = R.drawable.ic_qr,
                    label = getString(R.string.nc_export_via_bbqr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXPORT_FILE,
                    resId = R.drawable.ic_export,
                    label = getString(R.string.nc_export_via_file),
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showImportTransactionOptions() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_QR,
                    resId = R.drawable.ic_qr,
                    label = getString(R.string.nc_import_via_qr),
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_IMPORT_FILE,
                    resId = R.drawable.ic_import,
                    label = getString(R.string.nc_import_via_file),
                ),
            )
        ).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun getInvoiceInfo(): InvoiceInfo {
        val transaction = viewModel.getTransaction()
        return transaction.toInvoiceInfo(
            this,
            isInheritanceClaimingFlow = args.isInheritanceClaimingFlow
        )
    }
}