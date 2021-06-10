package com.nunchuk.android.wallet.details

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.*
import com.nunchuk.android.qr.convertToQRCode
import com.nunchuk.android.utils.setUnderline
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ActivityWalletDetailBinding
import com.nunchuk.android.wallet.details.WalletDetailsEvent.*
import com.nunchuk.android.wallet.details.WalletDetailsOption.*
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

class WalletDetailsActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var controller: IntentSharingController

    private val viewModel: WalletDetailsViewModel by lazy {
        ViewModelProviders.of(this, factory).get(WalletDetailsViewModel::class.java)
    }

    private lateinit var binding: ActivityWalletDetailBinding

    private lateinit var adapter: TransactionAdapter

    private val args: WalletDetailsArgs by lazy { WalletDetailsArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWalletDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        observeEvent()
        viewModel.init(args.walletId)
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: WalletDetailsEvent) {
        when (event) {
            is WalletDetailsError -> Toast.makeText(applicationContext, event.message, Toast.LENGTH_SHORT).show()
            is SendMoneyEvent -> navigator.openInputAmountScreen(this, args.walletId, event.amount.pureBTC())
            is UpdateUnusedAddress -> bindUnusedAddress(event.address)
            is OpenDynamicQRScreen -> navigator.openDynamicQRScreen(this, event.descriptors)
            is UploadWalletEvent -> shareConfigurationFile(event.filePath)
            DeleteWalletSuccess -> walletDeleted()
        }
    }

    private fun walletDeleted() {
        NCToastMessage(this).showMessage(getString(R.string.nc_wallet_delete_wallet_success))
        finish()
    }

    private fun bindUnusedAddress(address: String) {
        if (address.isEmpty()) {
            binding.emptyTxContainer.isVisible = false
        } else {
            binding.emptyTxContainer.isVisible = true
            binding.addressQR.setImageBitmap(address.convertToQRCode())
            binding.addressText.text = address
        }
    }

    private fun handleState(state: WalletDetailsState) {
        val wallet = state.wallet

        val multisigConfiguration = "${wallet.getConfiguration()} ${getString(R.string.nc_wallet_multisig)}"
        binding.multisigConfiguration.text = multisigConfiguration

        binding.btcAmount.text = wallet.getBTCAmount()
        binding.cashAmount.text = wallet.getUSDAmount()
        binding.btnSend.isClickable = state.wallet.balance.value > 0

        adapter.items = state.transactions
        val emptyTransactions = state.transactions.isEmpty()
        binding.emptyTxContainer.isVisible = emptyTransactions
        binding.transactionTitle.isVisible = !emptyTransactions
        binding.transactionList.isVisible = !emptyTransactions
    }

    private fun setupViews() {
        adapter = TransactionAdapter {
            navigator.openTransactionDetailsScreen(this, args.walletId, it.txId)
        }
        binding.transactionList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.transactionList.adapter = adapter

        binding.viewWalletConfig.setUnderline()
        binding.viewWalletConfig.setOnClickListener {
            navigator.openWalletConfigScreen(this, args.walletId)
        }
        binding.btnReceive.setOnClickListener { navigator.openReceiveTransactionScreen(this, args.walletId) }
        binding.btnSend.setOnClickListener { viewModel.handleSendMoneyEvent() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.toolbar.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                R.id.menu_search -> {
                    Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_more -> {
                    onMoreClicked()
                    true
                }
                else -> false
            }
        }
    }

    private fun shareDescriptor(descriptor: String) {
        controller.shareText(descriptor)
    }

    private fun shareConfigurationFile(filePath: String) {
        controller.shareFile(filePath)
    }

    private fun onMoreClicked() {
        val bottomSheet = WalletUpdateBottomSheet.show(fragmentManager = supportFragmentManager)
        bottomSheet.setListener {
            when (it) {
                BACKUP -> viewModel.handleBackupWallet()
                UPLOAD -> viewModel.handleUploadWallet()
                QR -> viewModel.handleExportWalletQR()
                DELETE -> viewModel.handleDeleteWallet()
            }
        }
    }

    companion object {

        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(WalletDetailsArgs(walletId = walletId).buildIntent(activityContext))
        }

    }

}