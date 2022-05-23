package com.nunchuk.android.transaction.components.details

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
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
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class TransactionDetailsActivity : BaseActivity<ActivityTransactionDetailsBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: TransactionDetailsArgs by lazy { TransactionDetailsArgs.deserializeFrom(intent) }

    private val viewModel: TransactionDetailsViewModel by viewModels { factory }

    private val controller: IntentSharingController by lazy { IntentSharingController.from(this) }

    override fun initializeBinding() = ActivityTransactionDetailsBinding.inflate(layoutInflater)

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
        viewModel.init(walletId = args.walletId, txId = args.txId, initEventId = args.initEventId)
    }

    override fun onResume() {
        super.onResume()
        showLoading()
        viewModel.getTransactionInfo()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
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
        bindSigners(state.transaction.signers, state.signers)
        hideLoading()
    }

    private fun bindSigners(signerMap: Map<String, Boolean>, signers: List<SignerModel>) {
        TransactionSignersViewBinder(
            container = binding.signerListView,
            signerMap = signerMap,
            signers = signers,
            listener = viewModel::handleSignEvent
        ).bindItems()
    }

    private fun bindTransaction(transaction: Transaction) {
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
        binding.sendingTo.text = output?.first.orEmpty().truncatedAddress()
        val pendingSigners = transaction.getPendingSignatures()
        if (pendingSigners > 0) {
            binding.signatureStatus.text = getString(R.string.nc_transaction_pending_signature, pendingSigners)
        } else {
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
            is TransactionDetailsError -> showError(event.message)
            is PromptInputPassphrase -> requireInputPassphrase(event.func)
            is PromptTransactionOptions -> promptTransactionOptions(event.isPendingTransaction)
            LoadingEvent -> showLoading()
            is ExportToFileSuccess -> showExportToFileSuccess(event)
            is ExportTransactionError -> showExportToFileError(event)
        }
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

    private fun openExternalLink(url: String) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(webIntent)
        } catch (e: ActivityNotFoundException) {
            CrashlyticsReporter.recordException(e)
            NCToastMessage(this).showWarning(getString(R.string.nc_transaction_no_app_to_open_link))
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

        fun start(activityContext: Activity, walletId: String, txId: String, initEventId: String = "") {
            activityContext.startActivity(
                TransactionDetailsArgs(walletId = walletId, txId = txId, initEventId = initEventId).buildIntent(activityContext)
            )
        }

    }

}