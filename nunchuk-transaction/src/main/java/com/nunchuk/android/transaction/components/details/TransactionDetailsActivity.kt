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

package com.nunchuk.android.transaction.components.details

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.compose.CoinTagGroupView
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.sheet.input.InputBottomSheet
import com.nunchuk.android.core.sheet.input.InputBottomSheetListener
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.bindTransactionStatus
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getFormatDate
import com.nunchuk.android.core.util.getPendingSignatures
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.util.isConfirmed
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.truncatedAddress
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.share.model.TransactionOption.CANCEL
import com.nunchuk.android.share.model.TransactionOption.COPY_TRANSACTION_ID
import com.nunchuk.android.share.model.TransactionOption.EXPORT_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.IMPORT_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.REMOVE_TRANSACTION
import com.nunchuk.android.share.model.TransactionOption.REPLACE_BY_FEE
import com.nunchuk.android.share.model.TransactionOption.SCHEDULE_BROADCAST
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
import com.nunchuk.android.transaction.components.schedule.ScheduleBroadcastTransactionActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmCoinList
import com.nunchuk.android.transaction.databinding.ActivityTransactionDetailsBinding
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.simpleWeekDayYearFormat
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Date


@AndroidEntryPoint
class TransactionDetailsActivity : BaseNfcActivity<ActivityTransactionDetailsBinding>(),
    InputBottomSheetListener, BottomSheetOptionListener {
    private var shouldReload: Boolean = true

    private val args: TransactionDetailsArgs by lazy { TransactionDetailsArgs.deserializeFrom(intent) }

    private val viewModel: TransactionDetailsViewModel by viewModels()

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
                    initEventId = "",
                    roomId = ""
                )
                NcToastManager.scheduleShowMessage(getString(R.string.nc_replace_by_fee_success))
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

    override fun initializeBinding() = ActivityTransactionDetailsBinding.inflate(layoutInflater)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (isNfcIntent(intent ?: return)) {
            shouldReload = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        if (args.walletId.isEmpty() && args.transaction == null) {
            CrashlyticsReporter.recordException(Exception("Wallet id is empty"))
            finish()
            return
        }

        if (args.txId.isEmpty() && args.transaction == null) {
            CrashlyticsReporter.recordException(Exception("Tx id is empty"))
            finish()
            return
        }
        viewModel.init(
            walletId = args.walletId,
            txId = args.txId,
            initEventId = args.initEventId,
            roomId = args.roomId,
            transaction = args.transaction
        )

        if (args.isInheritanceClaimingFlow) {
            showInheritanceClaimingDialog()
        }
        if (args.errorMessage.isBlank().not()) {
            showError(message = args.errorMessage)
        }
        if (args.isCancelBroadcast) {
            viewModel.cancelScheduleBroadcast()
        }
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
            SheetOptionType.TYPE_EXPORT_QR -> openExportTransactionScreen()
            SheetOptionType.TYPE_EXPORT_FILE -> viewModel.exportTransactionToFile()
            SheetOptionType.TYPE_IMPORT_QR -> openImportTransactionScreen()
            SheetOptionType.TYPE_IMPORT_FILE -> importFileLauncher.launch("*/*")
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
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

    private fun setupViews() {
        binding.tvEditNote.setUnderline()
        binding.tvEditChangeAddress.setUnderline()
        binding.tvEditChangeAddress.setOnDebounceClickListener {
            navigator.openCoinDetail(
                launcher = coinLauncher,
                context = this,
                walletId = args.walletId,
                txId = args.txId,
                vout = viewModel.getTransaction().changeIndex
            )
        }
        binding.viewMore.setOnClickListener {
            viewModel.handleViewMoreEvent()
        }
        binding.btnBroadcast.setOnClickListener {
            viewModel.handleBroadcastEvent()
        }
        binding.btnViewBlockChain.setOnClickListener {
            viewModel.handleViewBlockchainEvent()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                R.id.menu_more -> {
                    handleMenuMore()
                    true
                }
                else -> false
            }
        }
        binding.changeAddressLabel.setOnLongClickListener {
            handleCopyContent(binding.changeAddressLabel.text.toString())
            true
        }
        binding.noteContent.setOnLongClickListener {
            if (viewModel.getTransaction().memo.isNotEmpty()) {
                handleCopyContent(viewModel.getTransaction().memo)
            }
            true
        }
        binding.tvEditNote.setOnDebounceClickListener(coroutineScope = lifecycleScope) {
            InputBottomSheet.show(
                fragmentManager = supportFragmentManager,
                currentInput = viewModel.getTransaction().memo,
                title = getString(R.string.nc_transaction_note)
            )
        }
        binding.tvManageCoin.setOnDebounceClickListener {
            when (viewModel.coins().size) {
                1 -> navigator.openCoinDetail(
                    launcher = coinLauncher,
                    context = this,
                    walletId = args.walletId,
                    txId = args.txId,
                    vout = viewModel.coins().first().vout
                )
                else -> navigator.openCoinList(
                    launcher = coinLauncher,
                    context = this,
                    walletId = args.walletId,
                    txId = args.txId
                )
            }
        }
        binding.switchShowInputCoin.setOnClickListener {
            viewModel.toggleShowInputCoin()
        }
    }

    private fun handleMenuMore() {
        viewModel.handleMenuMoreEvent()
    }

    private fun handleState(state: TransactionDetailsState) {
        binding.viewMore.setCompoundDrawablesWithIntrinsicBounds(
            null, null, if (state.viewMore) ContextCompat.getDrawable(
                this, R.drawable.ic_collapse
            ) else ContextCompat.getDrawable(this, R.drawable.ic_expand), null
        )
        binding.viewMore.text = if (state.viewMore) {
            getString(R.string.nc_transaction_less_details)
        } else {
            getString(R.string.nc_transaction_more_details)
        }

        binding.transactionDetailsContainer.isVisible = state.viewMore

        bindTransaction(state.transaction, state.coins)
        if (state.transaction.isReceive.not()) {
            bindSigners(
                state.transaction.signers,
                state.signers.sortedByDescending(SignerModel::localKey),
                state.transaction.status,
                state.serverTransaction
            )
        }
        handleServerTransaction(state.transaction, state.serverTransaction)
        handleManageCoin(state.transaction.status, state.coins)
        hideLoading()
        handleShowInputCoin(state)
    }

    private fun handleShowInputCoin(state: TransactionDetailsState) {
        binding.switchShowInputCoin.isChecked = state.isShowInputCoin
        binding.inputCoin.isVisible = state.isShowInputCoin
        if (state.isShowInputCoin) {
            val coins = viewModel.convertInputs(state.transaction.inputs)
            binding.inputCoin.setContent {
                TransactionConfirmCoinList(inputs = coins, allTags = state.tags)
            }
        }
    }

    private fun handleManageCoin(status: TransactionStatus, coins: List<UnspentOutput>) {
        binding.tvManageCoin.isVisible = coins.isNotEmpty() && status.hadBroadcast()
    }

    private fun handleServerTransaction(
        transaction: Transaction, serverTransaction: ServerTransaction?
    ) {
        if (serverTransaction != null && transaction.status.canBroadCast() && serverTransaction.type == ServerTransactionType.SCHEDULED) {
            binding.status.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_schedule, 0, 0, 0
            )
            if (serverTransaction.broadcastTimeInMilis > 0L) {
                val broadcastTime = Date(serverTransaction.broadcastTimeInMilis)
                binding.status.text = getString(
                    R.string.nc_broadcast_on,
                    broadcastTime.simpleWeekDayYearFormat(),
                    broadcastTime.formatByHour()
                )
            }
        } else {
            binding.status.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    private fun bindSigners(
        signerMap: Map<String, Boolean>,
        signers: List<SignerModel>,
        status: TransactionStatus,
        serverTransaction: ServerTransaction?
    ) {
        TransactionSignersViewBinder(container = binding.signerListView,
            signerMap = signerMap,
            signers = signers,
            txStatus = status,
            serverTransaction = serverTransaction,
            listener = { signer ->
                viewModel.setCurrentSigner(signer)
                when (signer.type) {
                    SignerType.COLDCARD_NFC -> showSignByMk4Options()
                    SignerType.NFC -> {
                        if (viewModel.isInheritanceSigner(signer.fingerPrint)) {
                            NCWarningDialog(this).showDialog(title = getString(R.string.nc_text_confirmation),
                                message = getString(R.string.nc_inheritance_key_warning),
                                onYesClick = {
                                    startNfcFlow(REQUEST_NFC_SIGN_TRANSACTION)
                                })
                        } else {
                            startNfcFlow(REQUEST_NFC_SIGN_TRANSACTION)
                        }
                    }
                    SignerType.AIRGAP -> showSignByAirgapOptions()
                    else -> viewModel.handleSignSoftwareKey(signer)
                }
            }).bindItems()
    }

    private fun bindTransaction(transaction: Transaction, coins: List<UnspentOutput>) {
        binding.tvReplaceByFee.isVisible = transaction.replacedTxid.isNotEmpty()
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
        binding.noteContent.text = transaction.memo.ifEmpty { getString(R.string.nc_none) }
        binding.sendingTo.text = output?.first.orEmpty().truncatedAddress()
        binding.signatureStatus.isVisible = !transaction.status.hadBroadcast()
        val pendingSigners = transaction.getPendingSignatures()
        if (pendingSigners > 0) {
            binding.signatureStatus.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pending_signatures, 0, 0, 0
            )
            binding.signatureStatus.text = resources.getQuantityString(
                R.plurals.nc_transaction_pending_signature, pendingSigners, pendingSigners
            )
        } else {
            binding.signatureStatus.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_check_circle, 0, 0, 0
            )
            binding.signatureStatus.text = getString(R.string.nc_transaction_enough_signers)
        }
        binding.confirmTime.text = transaction.getFormatDate()
        binding.status.bindTransactionStatus(transaction)
        binding.sendingBTC.text = transaction.totalAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive
        binding.btnBroadcast.isVisible = transaction.status.canBroadCast()
        binding.btnViewBlockChain.isVisible =
            transaction.isReceive || transaction.status.hadBroadcast()

        bindAddress(transaction)
        bindChangeAddress(transaction, coins)
        bindTransactionFee(transaction)
        bindingTotalAmount(transaction)
        bindViewSendOrReceive(transaction)
    }

    private fun bindViewSendOrReceive(transaction: Transaction) {
        binding.divider.isVisible = !transaction.isReceive
        binding.estimatedFeeBTC.isVisible = !transaction.isReceive
        binding.estimatedFeeUSD.isVisible = !transaction.isReceive
        binding.estimatedFeeLabel.isVisible = !transaction.isReceive
        binding.totalAmountLabel.isVisible = !transaction.isReceive
        binding.totalAmountBTC.isVisible = !transaction.isReceive
        binding.totalAmountUSD.isVisible = !transaction.isReceive
        binding.changeAddress.isVisible = !transaction.isReceive
        binding.changeAddressBTC.isVisible = !transaction.isReceive
        binding.changeAddressUSD.isVisible = !transaction.isReceive
    }

    private fun bindAddress(transaction: Transaction) {
        val coins = transaction.outputs.filter { viewModel.isMyCoin(it) == transaction.isReceive }
        if (coins.isNotEmpty()) {
            TransactionAddressViewBinder(
                binding.containerAddress, coins,
            ) {
                handleCopyContent(it)
            }.bindItems()
        }

        if (transaction.isReceive) {
            binding.sendingToLabel.text = getString(R.string.nc_transaction_receive_at)
            binding.sendToAddress.text = getString(R.string.nc_transaction_receive_address)
        } else {
            if (transaction.status.isConfirmed()) {
                binding.sendingToLabel.text = getString(R.string.nc_transaction_send_to)
            } else {
                binding.sendingToLabel.text = getString(R.string.nc_transaction_sending_to)
            }
            binding.sendToAddress.text = getString(R.string.nc_transaction_send_to_address)
        }
    }

    private fun bindingTotalAmount(transaction: Transaction) {
        binding.totalAmountBTC.text = transaction.totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = transaction.totalAmount.getCurrencyAmount()
    }

    private fun bindTransactionFee(transaction: Transaction) {
        binding.estimatedFeeBTC.text = transaction.fee.getBTCAmount()
        binding.estimatedFeeUSD.text = transaction.fee.getCurrencyAmount()
    }

    private fun bindChangeAddress(transaction: Transaction, coins: List<UnspentOutput>) {
        val hasChange: Boolean = transaction.hasChangeIndex()
        if (hasChange) {
            binding.tvEditChangeAddress.isVisible = coins.any { it.vout == transaction.changeIndex }
            val txOutput = transaction.outputs[transaction.changeIndex]
            binding.changeAddressLabel.text = txOutput.first
            binding.changeAddressBTC.text = txOutput.second.getBTCAmount()
            binding.changeAddressUSD.text = txOutput.second.getCurrencyAmount()

            val changeOutput = coins.find { it.vout == transaction.changeIndex }
            binding.tags.isVisible = changeOutput != null && changeOutput.tags.isNotEmpty()
            if (changeOutput != null && changeOutput.tags.isNotEmpty()) {
                binding.tags.setContent {
                    CoinTagGroupView(tagIds = changeOutput.tags, tags = viewModel.allTags())
                }
            }
        }
        binding.changeAddressLabel.isVisible = hasChange
        binding.changeAddressBTC.isVisible = hasChange
        binding.changeAddressUSD.isVisible = hasChange
    }

    private fun handleEvent(event: TransactionDetailsEvent) {
        when (event) {
            is SignTransactionSuccess -> showSignTransactionSuccess(event)
            is BroadcastTransactionSuccess -> showBroadcastTransactionSuccess(event.roomId)
            is DeleteTransactionSuccess -> showTransactionDeleteSuccess(event.isCancel)
            is ViewBlockchainExplorer -> openExternalLink(event.url)
            is TransactionDetailsError -> handleSignError(event)
            is PromptInputPassphrase -> requireInputPassphrase(event.func)
            is PromptTransactionOptions -> promptTransactionOptions(event)
            LoadingEvent -> showLoading()
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
            ImportTransactionSuccess -> NCToastMessage(this).show(getString(R.string.nc_transaction_imported))
            NoInternetConnection -> showError("There is no Internet connection. The platform key co-signing policies will apply once you are connected.")
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
    }

    private fun handleUpdateTransactionFailed(event: UpdateTransactionMemoFailed) {
        hideLoading()
        NCToastMessage(this).showError(event.message)
    }

    private fun handleUpdateTransactionSuccess(event: UpdateTransactionMemoSuccess) {
        setResult(Activity.RESULT_OK)
        hideLoading()
        NCToastMessage(this).show(getString(R.string.nc_private_note_updated))
        binding.noteContent.text = event.newMemo
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

    private fun promptCancelTransactionConfirmation() {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_confirmation),
            message = getString(R.string.nc_transaction_confirmation),
            onYesClick = viewModel::handleDeleteTransactionEvent
        )
    }

    private fun promptTransactionOptions(event: PromptTransactionOptions) {
        TransactionOptionsBottomSheet.show(
            fragmentManager = supportFragmentManager,
            isPending = event.isPendingTransaction,
            isPendingConfirm = event.isPendingConfirm,
            isRejected = event.isRejected,
            isAssistedWallet = viewModel.isAssistedWallet(),
            isScheduleBroadcast = viewModel.isScheduleBroadcast()
        ).setListener {
            when (it) {
                CANCEL -> promptCancelTransactionConfirmation()
                EXPORT_TRANSACTION -> showExportTransactionOptions()
                IMPORT_TRANSACTION -> showImportTransactionOptions()
                REPLACE_BY_FEE -> handleOpenEditFee()
                COPY_TRANSACTION_ID -> handleCopyContent(args.txId)
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
            }
        }
    }

    private fun handleOpenEditFee() {
        navigator.openReplaceTransactionFee(
            replaceByFeeLauncher,
            this,
            walletId = args.walletId,
            transaction = viewModel.getTransaction()
        )
    }

    private fun openExportTransactionScreen() {
        startActivity(
            ExportTransactionActivity.buildIntent(
                activityContext = this,
                walletId = args.walletId,
                txId = args.txId,
                initEventId = viewModel.getInitEventId(),
                masterFingerPrint = viewModel.currentSigner()?.fingerPrint.orEmpty(),
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
            title = getString(R.string.nc_transaction_enter_passphrase), onConfirmed = func
        )
    }

    private fun showSignTransactionSuccess(event: SignTransactionSuccess) {
        hideLoading()
        if (viewModel.isAssistedWallet().not()) {
            NCToastMessage(this).show(getString(R.string.nc_transaction_signed_successful))
        } else if (event.serverSigned != null) {
            lifecycleScope.launch {
                NCToastMessage(this@TransactionDetailsActivity).show(getString(R.string.nc_transaction_signed_successful))
                if (event.status == TransactionStatus.READY_TO_BROADCAST && event.serverSigned) {
                    delay(3000L)
                    NCToastMessage(this@TransactionDetailsActivity).show(getString(R.string.nc_server_key_signed))
                } else if (event.status == TransactionStatus.PENDING_CONFIRMATION && event.serverSigned) {
                    delay(3000L)
                    NCToastMessage(this@TransactionDetailsActivity).show(getString(R.string.nc_server_key_signed))
                    delay(3000L)
                    NCToastMessage(this@TransactionDetailsActivity).show(getString(R.string.nc_transaction_has_succesfully_broadcast))
                }
            }
        }
        if (event.roomId.isNotEmpty()) {
            returnActiveRoom()
        }
    }

    private fun showBroadcastTransactionSuccess(roomId: String) {
        hideLoading()
        NCToastMessage(this).show(getString(R.string.nc_transaction_broadcast_successful))
        if (roomId.isEmpty()) {
            finish()
        } else {
            returnActiveRoom()
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

    companion object {
        fun buildIntent(
            activityContext: Context,
            walletId: String,
            txId: String,
            initEventId: String = "",
            roomId: String = "",
            transaction: Transaction? = null,
            isInheritanceClaimingFlow: Boolean = false,
            isCancelBroadcast: Boolean = false,
            errorMessage: String = ""
        ): Intent {
            return TransactionDetailsArgs(
                walletId = walletId,
                txId = txId,
                initEventId = initEventId,
                roomId = roomId,
                transaction = transaction,
                isInheritanceClaimingFlow = isInheritanceClaimingFlow,
                isCancelBroadcast = isCancelBroadcast,
                errorMessage = errorMessage
            ).buildIntent(activityContext)
        }
    }
}