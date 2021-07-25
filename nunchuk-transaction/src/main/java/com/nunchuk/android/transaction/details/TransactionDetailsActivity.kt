package com.nunchuk.android.transaction.details

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.toDisplayedText
import com.nunchuk.android.extensions.canBroadCast
import com.nunchuk.android.extensions.isCompleted
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.ActivityTransactionDetailsBinding
import com.nunchuk.android.transaction.details.TransactionDetailsEvent.*
import com.nunchuk.android.transaction.details.TransactionOption.EXPORT
import com.nunchuk.android.transaction.details.TransactionOption.IMPORT
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
        viewModel.init(walletId = args.walletId, txId = args.txId)
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
            binding.status.text = transaction.status.toDisplayedText(this)
        }
        binding.sendingBTC.text = transaction.subAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive
        binding.btnBroadcast.isVisible = transaction.status.canBroadCast()
        binding.btnViewBlockChain.isVisible = transaction.isReceive || transaction.status.isCompleted()
    }

    private fun handleEvent(event: TransactionDetailsEvent) {
        when (event) {
            SignTransactionSuccess -> showSignTransactionSuccess()
            BroadcastTransactionSuccess -> showBroadcastTransactionSuccess()
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

    private fun showSignTransactionSuccess() {
        hideLoading()
        NCToastMessage(this).show("Transaction signed successful")
    }

    private fun showBroadcastTransactionSuccess() {
        hideLoading()
        NCToastMessage(this).show("Transaction broadcast successful")
        navigator.openMainScreen(this)
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

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            txId: String
        ) {
            activityContext.startActivity(
                TransactionDetailsArgs(
                    walletId = walletId,
                    txId = txId
                ).buildIntent(activityContext)
            )
        }

    }

}
