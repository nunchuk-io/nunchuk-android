package com.nunchuk.android.transaction.details

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.extensions.canBroadCast
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.ActivityTransactionDetailsBinding
import com.nunchuk.android.transaction.details.TransactionDetailsEvent.*
import com.nunchuk.android.utils.toDisplayedText
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class TransactionDetailsActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: TransactionDetailsArgs by lazy { TransactionDetailsArgs.deserializeFrom(intent) }

    private val viewModel: TransactionDetailsViewModel by lazy {
        ViewModelProviders.of(this, factory).get(TransactionDetailsViewModel::class.java)
    }

    private lateinit var binding: ActivityTransactionDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityTransactionDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()
        viewModel.init(walletId = args.walletId, txId = args.txId)
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
    }

    private fun handleState(state: TransactionDetailsState) {
        binding.transactionDetailsContainer.isVisible = state.viewMore
        bindTransaction(state.transaction)
        bindSigners(state.transaction.signers, state.signers)
    }

    private fun bindSigners(signerMap: Map<String, Boolean>, signers: List<SignerModel>) {
        TransactionSignersViewBinder(
            container = binding.signersContainer,
            signerMap = signerMap,
            signers = signers,
            listener = viewModel::handleSignEvent
        ).bindItems()
    }

    private fun bindTransaction(transaction: Transaction) {
        val pendingSigners = transaction.signers.count { !it.value }
        if (pendingSigners > 0) {
            binding.signatureStatus.text = getString(R.string.nc_transaction_pending_signature, pendingSigners)
        } else {
            binding.signatureStatus.text = getString(R.string.nc_transaction_enough_signers)
        }
        binding.status.text = transaction.status.toDisplayedText(this)
        binding.sendingBTC.text = transaction.subAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive
        binding.btnBroadcast.isVisible = transaction.status.canBroadCast()
        binding.btnViewBlockChain.isVisible = transaction.isReceive
    }

    private fun handleEvent(event: TransactionDetailsEvent) {
        when (event) {
            SignTransactionSuccess -> showSignTransactionSuccess()
            BroadcastTransactionSuccess -> showBroadcastTransactionSuccess()
            DeleteTransactionSuccess -> showTransactionDeleteSuccess()
            is ViewBlockchainExplorer -> openExternalLink(event.url)
            is TransactionDetailsError -> showError(event.message)
        }
    }

    private fun showSignTransactionSuccess() {
        NCToastMessage(this).show("Transaction signed successful")
    }

    private fun showBroadcastTransactionSuccess() {
        NCToastMessage(this).show("Transaction broadcast successful")
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
        NCToastMessage(this).show("Delete transaction success")
    }

    private fun showError(message: String) {
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

