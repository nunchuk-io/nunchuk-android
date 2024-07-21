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
import com.nunchuk.android.core.util.isPending
import com.nunchuk.android.core.util.isPendingConfirm
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.truncatedAddress
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.isObserver
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
import com.nunchuk.android.transaction.components.details.RequestSignatureMemberFragment.Companion.EXTRA_MEMBER_ID
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
import com.nunchuk.android.transaction.components.invoice.InvoiceInfo
import com.nunchuk.android.transaction.components.schedule.ScheduleBroadcastTransactionActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmCoinList
import com.nunchuk.android.transaction.databinding.ActivityTransactionDetailsBinding
import com.nunchuk.android.type.SignerTag
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
        binding.estimatedFeeLabel.setOnClickListener {
            showEstimatedFeeTooltip()
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
            viewModel.coins().find { it.vout == viewModel.getTransaction().changeIndex }?.let {
                navigator.openCoinDetail(
                    launcher = coinLauncher,
                    context = this,
                    walletId = args.walletId,
                    it
                )
            }
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
            if (viewModel.getUserRole().isKeyHolderLimited.not()) {
                handleCopyContent(binding.changeAddressLabel.text.toString())
            }
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
                    viewModel.coins().first()
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

        supportFragmentManager.setFragmentResultListener(
            RequestSignatureMemberFragment.REQUEST_KEY,
            this
        ) { requestKey, result ->
            if (requestKey == RequestSignatureMemberFragment.REQUEST_KEY) {
                val memberId = result.getString(EXTRA_MEMBER_ID)
                viewModel.requestSignatureTransaction(memberId.orEmpty())
            }
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
        if (viewModel.getUserRole().isObserver) {
            binding.toolbar.menu.clear()
        }

        bindTransaction(state.transaction, state.coins, state.serverTransaction)
        if (state.transaction.isReceive.not()) {
            bindSigners(
                state.transaction.signers,
                state.signers.sortedByDescending(SignerModel::localKey),
                state.transaction.status,
                state.serverTransaction
            )
        }
        handleServerTransaction(state.transaction, state.serverTransaction)
        handleManageCoin(state.transaction.status, state.coins, state.userRole)
        hideLoading()
        handleShowInputCoin(state)
    }

    private fun handleShowInputCoin(state: TransactionDetailsState) {
        if (state.userRole.isKeyHolderLimited) {
            binding.switchShowInputCoin.apply {
                isEnabled = false
                isChecked = false
                isClickable = false
            }
        }
        binding.switchShowInputCoin.isVisible = state.txInputCoins.isNotEmpty()
        binding.tvShowInputCoin.isVisible = state.txInputCoins.isNotEmpty()
        binding.switchShowInputCoin.isChecked = state.isShowInputCoin
        binding.inputCoin.isVisible = state.isShowInputCoin
        if (state.isShowInputCoin) {
            binding.inputCoin.setContent {
                TransactionConfirmCoinList(inputs = state.txInputCoins, allTags = state.tags)
            }
        }
    }

    private fun handleManageCoin(
        status: TransactionStatus,
        coins: List<UnspentOutput>,
        role: AssistedWalletRole
    ) {
        binding.tvManageCoin.isVisible =
            coins.isNotEmpty() && status.hadBroadcast() && role.isKeyHolderLimited.not()
    }

    private fun handleServerTransaction(
        transaction: Transaction, serverTransaction: ServerTransaction?,
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

    private fun isServerBroadcastTime(transaction: Transaction, serverTransaction: ServerTransaction?): Boolean {
        return serverTransaction != null && transaction.status.canBroadCast() && serverTransaction.type == ServerTransactionType.SCHEDULED && serverTransaction.broadcastTimeInMilis > 0L
    }

    private fun bindSigners(
        signerMap: Map<String, Boolean>,
        signers: List<SignerModel>,
        status: TransactionStatus,
        serverTransaction: ServerTransaction?,
    ) {
        TransactionSignersViewBinder(container = binding.signerListView,
            signerMap = signerMap,
            signers = signers,
            txStatus = status,
            serverTransaction = serverTransaction,
            userRole = viewModel.getUserRole(),
            listener = { signer ->
                viewModel.setCurrentSigner(signer)
                when {
                    signer.type == SignerType.COLDCARD_NFC
                            || signer.type == SignerType.HARDWARE && signer.tags.contains(SignerTag.COLDCARD) -> showSignByMk4Options()

                    signer.type == SignerType.NFC -> {
                        startNfcFlow(REQUEST_NFC_SIGN_TRANSACTION)
                    }

                    signer.type == SignerType.AIRGAP || signer.type == SignerType.UNKNOWN -> showSignByAirgapOptions()
                    signer.type == SignerType.HARDWARE -> showError(getString(R.string.nc_use_desktop_app_to_sign))
                    else -> viewModel.handleSignSoftwareKey(signer)
                }
            }).bindItems()
    }

    private fun bindTransaction(transaction: Transaction, coins: List<UnspentOutput>, serverTransaction: ServerTransaction?) {
        binding.tvReplaceByFee.isVisible = transaction.replacedTxid.isNotEmpty()
        binding.noteContent.text = transaction.memo.ifEmpty { getString(R.string.nc_none) }
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
        binding.confirmTime.isVisible = args.isInheritanceClaimingFlow.not()
        binding.confirmTime.text = transaction.getFormatDate()
        binding.status.bindTransactionStatus(transaction)
        binding.sendingBTC.text = transaction.totalAmount.getBTCAmount()
        binding.signersContainer.isVisible =
            !transaction.isReceive && args.isInheritanceClaimingFlow.not()
        binding.btnBroadcast.isVisible =
            transaction.status.canBroadCast() && args.isInheritanceClaimingFlow.not() && viewModel.getUserRole().isObserver.not() &&
                    isServerBroadcastTime(transaction, serverTransaction).not()
        binding.btnViewBlockChain.isVisible =
            transaction.isReceive || transaction.status.hadBroadcast()
        if (transaction.status.canBroadCast() || transaction.status.isPendingConfirm() || transaction.status.isConfirmed()) {
            handleSignRequestSignature(false)
        }

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
        val coins = if (transaction.isReceive)
            transaction.receiveOutputs else
            transaction.outputs.filterIndexed { index, _ -> index != transaction.changeIndex }
        binding.tvMoreAddress.isVisible = coins.size > 30
        binding.tvMoreAddress.text = getString(R.string.nc_more_address, coins.size - 30)
        if (coins.isNotEmpty()) {
            TransactionAddressViewBinder(
                binding.containerAddress, coins.take(30),
            ) {
                if (viewModel.getUserRole().isKeyHolderLimited.not()) {
                    handleCopyContent(it)
                }
            }.bindItems()
        }
        if (coins.size >= 2) {
            binding.sendingTo.text = getString(R.string.nc_multiple_addresses)
        } else {
            val output = coins.firstOrNull()
            binding.sendingTo.text = output?.first.orEmpty().truncatedAddress()
        }
        if (transaction.isReceive) {
            binding.sendingToLabel.text = getString(R.string.nc_transaction_receive_at)
            binding.sendToAddress.text = getString(R.string.nc_transaction_receive_at)
        } else {
            if (transaction.status.isConfirmed()) {
                binding.sendingToLabel.text = getString(R.string.nc_transaction_send_to)
            } else {
                binding.sendingToLabel.text = getString(R.string.nc_transaction_sending_to)
            }
            binding.sendToAddress.text = getString(R.string.nc_transaction_sending_to)
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
        binding.noteContent.text = event.newMemo.ifEmpty { getString(R.string.nc_none) }
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
        handleSignRequestSignature()
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
        val coins = if (transaction.isReceive)
            transaction.receiveOutputs else
            transaction.outputs.filterIndexed { index, _ -> index != transaction.changeIndex }
        val txOutput = if (transaction.hasChangeIndex()) transaction.outputs[transaction.changeIndex] else null
        return InvoiceInfo(
            amountSent = transaction.totalAmount.getBTCAmount(),
            confirmTime = if (args.isInheritanceClaimingFlow.not()) transaction.getFormatDate() else "",
            transactionId = args.txId,
            txOutputs = coins,
            estimatedFee = if (!transaction.isReceive) transaction.fee.getBTCAmount() else "",
            changeAddress = if (transaction.hasChangeIndex()) txOutput?.first.orEmpty() else "",
            changeAddressAmount = if (transaction.hasChangeIndex()) txOutput?.second?.getBTCAmount().orEmpty() else "",
            note = transaction.memo.ifEmpty { getString(R.string.nc_none) },
            isReceive = transaction.isReceive,
        )
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
            errorMessage: String = "",
            isRequestSignatureFlow: Boolean = false,
        ): Intent {
            return TransactionDetailsArgs(
                walletId = walletId,
                txId = txId,
                initEventId = initEventId,
                roomId = roomId,
                transaction = transaction,
                isInheritanceClaimingFlow = isInheritanceClaimingFlow,
                isCancelBroadcast = isCancelBroadcast,
                errorMessage = errorMessage,
                isRequestSignatureFlow = isRequestSignatureFlow,
            ).buildIntent(activityContext)
        }
    }
}