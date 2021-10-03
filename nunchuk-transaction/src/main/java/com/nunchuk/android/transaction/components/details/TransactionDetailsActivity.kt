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
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.bindTransactionStatus
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.extensions.canBroadCast
import com.nunchuk.android.extensions.isCompleted
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.TransactionDetailsEvent.*
import com.nunchuk.android.transaction.components.details.TransactionOption.EXPORT
import com.nunchuk.android.transaction.components.details.TransactionOption.IMPORT
import com.nunchuk.android.transaction.databinding.ActivityTransactionDetailsBinding
import com.nunchuk.android.type.TransactionStatus
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

    override fun initializeBinding() = ActivityTransactionDetailsBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(walletId = args.walletId, txId = args.txId, initEventId = args.initEventId)
    }

    override fun onResume() {
        super.onResume()
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
                    onMoreClicked()
                    true
                }
                else -> false
            }
        }
    }

    private fun onMoreClicked() {
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
        binding.sendingTo.text = transaction.outputs.first().first
        val pendingSigners = transaction.signers.count { !it.value }
        if (pendingSigners > 0) {
            binding.signatureStatus.text = getString(R.string.nc_transaction_pending_signature, pendingSigners)
        } else {
            binding.signatureStatus.text = getString(R.string.nc_transaction_enough_signers)
        }
        if (transaction.status == TransactionStatus.CONFIRMED) {
            val confirmText = "${transaction.height} ${getString(R.string.nc_transaction_confirmations)}"
            binding.status.text = confirmText
        } else {
            binding.status.bindTransactionStatus(transaction.status)
        }
        binding.sendingBTC.text = transaction.subAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive
        binding.btnBroadcast.isVisible = transaction.status.canBroadCast()
        binding.btnViewBlockChain.isVisible = transaction.isReceive || transaction.status.isCompleted()

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
        binding.sendAddressLabel.text = transaction.outputs.first().first
        binding.sendAddressBTC.text = transaction.outputs.first().second.getBTCAmount()
        binding.sendAddressUSD.text = transaction.outputs.first().second.getUSDAmount()

        binding.sendingToLabel.text = if (transaction.isReceive) getString(R.string.nc_transaction_received_to) else getString(R.string.nc_transaction_sending_to)
        binding.sendToAddress.text = if (transaction.isReceive) getString(R.string.nc_transaction_receive_address) else getString(R.string.nc_transaction_send_to_address)
    }

    private fun bindingTotalAmount(transaction: Transaction) {
        binding.totalAmountBTC.text = transaction.subAmount.getBTCAmount()
        binding.totalAmountUSD.text = transaction.subAmount.getUSDAmount()
    }

    private fun bindTransactionFee(transaction: Transaction) {
        binding.estimatedFeeBTC.text = transaction.fee.getBTCAmount()
        binding.estimatedFeeUSD.text = transaction.fee.getUSDAmount()
    }

    private fun bindChangeAddress(transaction: Transaction) {
        if (transaction.changeIndex > 0) {
            val txOutput = transaction.outputs[transaction.changeIndex]
            binding.changeAddressLabel.text = txOutput.first
            binding.changeAddressBTC.text = txOutput.second.getBTCAmount()
            binding.changeAddressUSD.text = txOutput.second.getUSDAmount()
        }
        binding.changeAddressLabel.isVisible = transaction.changeIndex > 0
        binding.changeAddressBTC.isVisible = transaction.changeIndex > 0
        binding.changeAddressUSD.isVisible = transaction.changeIndex > 0
    }

    private fun handleEvent(event: TransactionDetailsEvent) {
        when (event) {
            is SignTransactionSuccess -> showSignTransactionSuccess(event.roomId)
            is BroadcastTransactionSuccess -> showBroadcastTransactionSuccess(event.roomId)
            DeleteTransactionSuccess -> showTransactionDeleteSuccess()
            is ViewBlockchainExplorer -> openExternalLink(event.url)
            is TransactionDetailsError -> showError(event.message)
            is PromptInputPassphrase -> requireInputPassphrase(event.func)
            ImportOrExportTransaction -> importOrExportTransaction()
            PromptDeleteTransaction -> promptDeleteTransaction()
            LoadingEvent -> showLoading()
        }
    }

    private fun promptDeleteTransaction() {
        TransactionDetailsBottomSheet.show(supportFragmentManager)
            .setListener(::promptCancelTransactionConfirmation)
    }

    private fun promptCancelTransactionConfirmation() {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_confirmation),
            message = getString(R.string.nc_transaction_confirmation),
            onYesClick = viewModel::handleDeleteTransactionEvent
        )
    }

    private fun importOrExportTransaction() {
        TransactionSignBottomSheet.show(supportFragmentManager)
            .setListener {
                when (it) {
                    EXPORT -> openExportTransactionScreen()
                    IMPORT -> openImportTransactionScreen()
                }
            }
    }

    private fun openExportTransactionScreen() {
        navigator.openExportTransactionScreen(this, args.walletId, args.txId)
    }

    private fun openImportTransactionScreen() {
        navigator.openImportTransactionScreen(this, args.walletId)
    }

    private fun requireInputPassphrase(func: (String) -> Unit) {
        NCInputDialog(this).showDialog(
            title = getString(R.string.nc_transaction_enter_passphrase),
            onConfirmed = func
        )
    }

    private fun showSignTransactionSuccess(roomId: String) {
        hideLoading()
        NCToastMessage(this).show("Transaction signed successful")
        if (roomId.isNotEmpty()) {
            returnActiveRoom(roomId)
        }
    }

    private fun showBroadcastTransactionSuccess(roomId: String) {
        hideLoading()
        NCToastMessage(this).show("Transaction broadcast successful")
        if (roomId.isEmpty()) {
            navigator.openMainScreen(this)
        } else {
            returnActiveRoom(roomId)
        }
    }

    private fun openExternalLink(url: String) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(webIntent)
        } catch (e: ActivityNotFoundException) {
            NCToastMessage(this).showWarning("There is no apps found to open link")
        }
    }

    private fun showTransactionDeleteSuccess() {
        finish()
        NCToastMessage(this).show("Delete transaction success")
    }

    private fun showError(message: String) {
        hideLoading()
        NCToastMessage(this).showError(message)
    }

    private fun returnActiveRoom(roomId: String) {
        ActivityManager.instance.popUntilRoot()
        navigator.openRoomDetailActivity(this, roomId)
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            txId: String,
            initEventId: String = ""
        ) {
            activityContext.startActivity(
                TransactionDetailsArgs(
                    walletId = walletId,
                    txId = txId,
                    initEventId = initEventId
                ).buildIntent(activityContext)
            )
        }

    }

}
