package com.nunchuk.android.transaction.components.details

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.model.TransactionOption.*
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.*
import com.nunchuk.android.transaction.components.export.ExportTransactionActivity
import com.nunchuk.android.transaction.components.imports.ImportTransactionActivity
import com.nunchuk.android.transaction.databinding.ActivityTransactionDetailsBinding
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class TransactionDetailsActivity : BaseNfcActivity<ActivityTransactionDetailsBinding>() {
    private var shouldReload: Boolean = true

    private val args: TransactionDetailsArgs by lazy { TransactionDetailsArgs.deserializeFrom(intent) }

    private val viewModel: TransactionDetailsViewModel by viewModels()

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    override fun initializeBinding() = ActivityTransactionDetailsBinding.inflate(layoutInflater)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent?.action) {
            shouldReload = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        if (args.walletId.isEmpty()) {
            CrashlyticsReporter.recordException(Exception("Wallet id is empty"))
            finish()
            return
        }

        if (args.txId.isEmpty()) {
            CrashlyticsReporter.recordException(Exception("Tx id is empty"))
            finish()
            return
        }
        viewModel.init(walletId = args.walletId, txId = args.txId, initEventId = args.initEventId, roomId = args.roomId)
    }

    override fun onResume() {
        super.onResume()
        if (shouldReload) {
            viewModel.getTransactionInfo()
        }
        shouldReload = true
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_SIGN_TRANSACTION }
                    .collect {
                        viewModel.handleSignByTapSigner(IsoDep.get(it.tag), nfcViewModel.inputCvc.orEmpty())
                        nfcViewModel.clearScanInfo()
                    }
            }
        }
    }

    private fun setupViews() {
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
    }

    private fun handleMenuMore() {
        viewModel.handleMenuMoreEvent()
    }

    private fun handleState(state: TransactionDetailsState) {
        binding.viewMore.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            if (state.viewMore) ContextCompat.getDrawable(this, R.drawable.ic_collapse) else ContextCompat.getDrawable(this, R.drawable.ic_expand),
            null
        )
        binding.viewMore.text = if (state.viewMore) {
            getString(R.string.nc_transaction_less_details)
        } else {
            getString(R.string.nc_transaction_more_details)
        }

        binding.transactionDetailsContainer.isVisible = state.viewMore

        bindTransaction(state.transaction)
        bindSigners(state.transaction.signers, state.signers.sortedByDescending(SignerModel::localKey))
        hideLoading()
    }

    private fun bindSigners(signerMap: Map<String, Boolean>, signers: List<SignerModel>) {
        TransactionSignersViewBinder(
            container = binding.signerListView,
            signerMap = signerMap,
            signers = signers,
            listener = { signer ->
                if (signer.type == SignerType.NFC) {
                    startNfcFlow(REQUEST_NFC_SIGN_TRANSACTION)
                } else {
                    viewModel.handleSignEvent(signer)
                }
            }
        ).bindItems()
    }

    private fun bindTransaction(transaction: Transaction) {
        binding.toolbar.menu.findItem(R.id.menu_more).isVisible =
            transaction.status != TransactionStatus.CONFIRMED && transaction.status != TransactionStatus.PENDING_CONFIRMATION
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
        binding.sendingTo.text = output?.first.orEmpty().truncatedAddress()
        binding.signatureStatus.isVisible = !transaction.status.hadBroadcast()
        val pendingSigners = transaction.getPendingSignatures()
        if (pendingSigners > 0) {
            binding.signatureStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pending_signatures, 0, 0, 0)
            binding.signatureStatus.text = resources.getQuantityString(R.plurals.nc_transaction_pending_signature, pendingSigners, pendingSigners)
        } else {
            binding.signatureStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0)
            binding.signatureStatus.text = getString(R.string.nc_transaction_enough_signers)
        }
        binding.confirmTime.text = transaction.getFormatDate()
        binding.status.bindTransactionStatus(transaction)
        binding.sendingBTC.text = transaction.totalAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive
        binding.btnBroadcast.isVisible = transaction.status.canBroadCast()
        binding.btnViewBlockChain.isVisible = transaction.isReceive || transaction.status.hadBroadcast()

        bindAddress(transaction)
        bindChangeAddress(transaction)
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
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
        binding.sendAddressLabel.text = output?.first.orEmpty()
        binding.sendAddressBTC.text = output?.second?.getBTCAmount().orEmpty()
        binding.sendAddressUSD.text = output?.second?.getUSDAmount().orEmpty()

        if (transaction.isReceive) {
            binding.sendingToLabel.text = getString(R.string.nc_transaction_receive_at)
            binding.sendToAddress.text = getString(R.string.nc_transaction_receive_address)
        } else {
            if (transaction.status.isConfirmed()) {
                binding.sendingToLabel.text = getString(R.string.nc_transaction_sent_to)
            } else {
                binding.sendingToLabel.text = getString(R.string.nc_transaction_sending_to)
            }
            binding.sendToAddress.text = getString(R.string.nc_transaction_send_to_address)
        }
    }

    private fun bindingTotalAmount(transaction: Transaction) {
        binding.totalAmountBTC.text = transaction.totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = transaction.totalAmount.getUSDAmount()
    }

    private fun bindTransactionFee(transaction: Transaction) {
        binding.estimatedFeeBTC.text = transaction.fee.getBTCAmount()
        binding.estimatedFeeUSD.text = transaction.fee.getUSDAmount()
    }

    private fun bindChangeAddress(transaction: Transaction) {
        val hasChange: Boolean = transaction.hasChangeIndex()
        if (hasChange) {
            val txOutput = transaction.outputs[transaction.changeIndex]
            binding.changeAddressLabel.text = txOutput.first
            binding.changeAddressBTC.text = txOutput.second.getBTCAmount()
            binding.changeAddressUSD.text = txOutput.second.getUSDAmount()
        }
        binding.changeAddressLabel.isVisible = hasChange
        binding.changeAddressBTC.isVisible = hasChange
        binding.changeAddressUSD.isVisible = hasChange
    }

    private fun handleEvent(event: TransactionDetailsEvent) {
        when (event) {
            is SignTransactionSuccess -> showSignTransactionSuccess(event.roomId)
            is BroadcastTransactionSuccess -> showBroadcastTransactionSuccess(event.roomId)
            DeleteTransactionSuccess -> showTransactionDeleteSuccess()
            is ViewBlockchainExplorer -> openExternalLink(event.url)
            is TransactionDetailsError -> handleSignError(event)
            is PromptInputPassphrase -> requireInputPassphrase(event.func)
            is PromptTransactionOptions -> promptTransactionOptions(event.isPendingTransaction)
            LoadingEvent -> showLoading()
            NfcLoadingEvent -> showLoading(message = getString(R.string.nc_keep_holding_nfc))
            is ExportToFileSuccess -> showExportToFileSuccess(event)
            is ExportTransactionError -> showExportToFileError(event)
        }
    }

    private fun handleSignError(event: TransactionDetailsError) {
        hideLoading()
        if (nfcViewModel.handleNfcError(event.e).not()) showError(event.message)
    }

    private fun showExportToFileError(event: ExportTransactionError) {
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

    private fun promptTransactionOptions(isPending: Boolean) {
        TransactionOptionsBottomSheet.show(supportFragmentManager, isPending)
            .setListener {
                when (it) {
                    CANCEL -> promptCancelTransactionConfirmation()
                    EXPORT -> openExportTransactionScreen(EXPORT)
                    IMPORT_KEYSTONE -> openImportTransactionScreen(IMPORT_KEYSTONE)
                    EXPORT_PASSPORT -> openExportTransactionScreen(EXPORT_PASSPORT)
                    IMPORT_PASSPORT -> openImportTransactionScreen(IMPORT_PASSPORT)
                    EXPORT_PSBT -> viewModel.exportTransactionToFile()
                }
            }
    }

    private fun openExportTransactionScreen(transactionOption: TransactionOption) {
        ExportTransactionActivity.start(
            activityContext = this,
            walletId = args.walletId,
            txId = args.txId,
            transactionOption = transactionOption
        )
    }

    private fun openImportTransactionScreen(transactionOption: TransactionOption) {
        ImportTransactionActivity.start(
            activityContext = this,
            walletId = args.walletId,
            transactionOption = transactionOption
        )
    }

    private fun requireInputPassphrase(func: (String) -> Unit) {
        NCInputDialog(this).showDialog(
            title = getString(R.string.nc_transaction_enter_passphrase),
            onConfirmed = func
        )
    }

    private fun showSignTransactionSuccess(roomId: String) {
        hideLoading()
        NCToastMessage(this).show(getString(R.string.nc_transaction_signed_successful))
        if (roomId.isNotEmpty()) {
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

    private fun showTransactionDeleteSuccess() {
        finish()
        NCToastMessage(this).show(getString(R.string.nc_transaction_deleted))
    }

    private fun showError(message: String) {
        hideLoading()
        NCToastMessage(this).showError(message)
    }

    private fun returnActiveRoom() {
        finish()
    }

    companion object {

        fun start(activityContext: Activity, walletId: String, txId: String, initEventId: String = "", roomId: String = "") {
            activityContext.startActivity(
                TransactionDetailsArgs(walletId = walletId, txId = txId, initEventId = initEventId, roomId = roomId).buildIntent(activityContext)
            )
        }

    }

}