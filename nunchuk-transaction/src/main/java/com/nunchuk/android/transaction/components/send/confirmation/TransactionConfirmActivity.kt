package com.nunchuk.android.transaction.components.send.confirmation

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.*
import com.nunchuk.android.transaction.databinding.ActivityTransactionConfirmBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionConfirmActivity : BaseActivity<ActivityTransactionConfirmBinding>() {

    private val args: TransactionConfirmArgs by lazy { TransactionConfirmArgs.deserializeFrom(intent) }

    private val viewModel: TransactionConfirmViewModel by viewModels()

    override fun initializeBinding() = ActivityTransactionConfirmBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(
            walletId = args.walletId,
            address = args.address,
            sendAmount = args.outputAmount,
            estimateFee = args.estimatedFee,
            subtractFeeFromAmount = args.subtractFeeFromAmount,
            privateNote = args.privateNote,
            manualFeeRate = args.manualFeeRate
        )
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun setupViews() {
        binding.sendAddressLabel.text = args.address
        binding.estimatedFeeBTC.text = args.estimatedFee.getBTCAmount()
        binding.estimatedFeeUSD.text = args.estimatedFee.getUSDAmount()
        val sendAmount: Double
        val totalAmount: Double
        if (args.subtractFeeFromAmount) {
            sendAmount = args.outputAmount - args.estimatedFee
            totalAmount = args.outputAmount
        } else {
            sendAmount = args.outputAmount
            totalAmount = args.outputAmount + args.estimatedFee
        }
        binding.sendAddressBTC.text = sendAmount.getBTCAmount()
        binding.sendAddressUSD.text = sendAmount.getUSDAmount()
        binding.totalAmountBTC.text = totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = totalAmount.getUSDAmount()
        binding.noteContent.text = args.privateNote

        binding.btnConfirm.setOnClickListener {
            viewModel.handleConfirmEvent()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleEvent(event: TransactionConfirmEvent) {
        when (event) {
            is CreateTxErrorEvent -> showCreateTransactionError(event.message)
            is CreateTxSuccessEvent -> openTransactionDetailScreen(event.txId)
            is UpdateChangeAddress -> bindChangAddress(event.address, event.amount)
            LoadingEvent -> showLoading()
            is InitRoomTransactionError -> showCreateTransactionError(event.message)
            is InitRoomTransactionSuccess -> returnActiveRoom(event.roomId)
        }
    }

    private fun returnActiveRoom(roomId: String) {
        hideLoading()
        finish()
        ActivityManager.popUntil(InputAmountActivity::class.java, true)
        navigator.openRoomDetailActivity(this, roomId)
    }

    private fun bindChangAddress(changeAddress: String, amount: Amount) {
        hideLoading()

        if (changeAddress.isNotBlank()) {
            binding.changeAddressLabel.text = changeAddress
            binding.changeAddressBTC.text = amount.getBTCAmount()
            binding.changeAddressUSD.text = amount.getUSDAmount()
        } else {
            binding.changeAddress.visibility = View.GONE
            binding.changeAddressLabel.visibility = View.GONE
            binding.changeAddressBTC.visibility = View.GONE
            binding.changeAddressUSD.visibility = View.GONE
        }
    }

    private fun openTransactionDetailScreen(txId: String) {
        hideLoading()
        ActivityManager.popUntil(InputAmountActivity::class.java, true)
        navigator.openTransactionDetailsScreen(
            activityContext = this,
            walletId = args.walletId,
            txId = txId,
            initEventId = "",
            roomId = SessionHolder.getActiveRoomIdSafe()
        )
        NCToastMessage(this).showMessage("Transaction created::$txId")
    }

    private fun showCreateTransactionError(message: String) {
        hideLoading()
        NCToastMessage(this).showError("Create transaction error due to $message")
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            outputAmount: Double,
            availableAmount: Double,
            address: String,
            privateNote: String,
            estimatedFee: Double,
            subtractFeeFromAmount: Boolean = false,
            manualFeeRate: Int = 0
        ) {
            activityContext.startActivity(
                TransactionConfirmArgs(
                    walletId = walletId,
                    outputAmount = outputAmount,
                    availableAmount = availableAmount,
                    address = address,
                    privateNote = privateNote,
                    estimatedFee = estimatedFee,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate
                ).buildIntent(activityContext)
            )
        }

    }

}