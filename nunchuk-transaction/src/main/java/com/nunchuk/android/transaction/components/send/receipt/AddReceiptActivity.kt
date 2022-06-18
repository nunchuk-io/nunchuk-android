package com.nunchuk.android.transaction.components.send.receipt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.qr.QRCodeParser
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.*
import com.nunchuk.android.transaction.databinding.ActivityTransactionAddReceiptBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddReceiptActivity : BaseActivity<ActivityTransactionAddReceiptBinding>() {

    private val args: AddReceiptArgs by lazy { AddReceiptArgs.deserializeFrom(intent) }

    private val viewModel: AddReceiptViewModel by viewModels()

    override fun initializeBinding() = ActivityTransactionAddReceiptBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        binding.receiptInput.addTextChangedCallback(viewModel::handleReceiptChanged)
        binding.privateNoteInput.setMaxLength(MAX_NOTE_LENGTH)
        binding.privateNoteInput.addTextChangedCallback(viewModel::handlePrivateNoteChanged)

        binding.qrCode.setOnClickListener { startQRCodeScan() }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        QRCodeParser.parse(requestCode, resultCode, data)?.apply {
            binding.receiptInput.setText(this)
        }
    }

    private fun handleState(state: AddReceiptState) {
        val privateNoteCounter = "${state.privateNote.length}/$MAX_NOTE_LENGTH"
        binding.privateNoteCounter.text = privateNoteCounter
    }

    private fun handleEvent(event: AddReceiptEvent) {
        when (event) {
            is AcceptedAddressEvent -> openEstimatedFeeScreen(event.address, event.privateNote)
            AddressRequiredEvent -> showAddressRequiredError()
            InvalidAddressEvent -> showInvalidAddressError()
        }
    }

    private fun showInvalidAddressError() {
        showError(getString(R.string.nc_transaction_invalid_address))
    }

    private fun showAddressRequiredError() {
        showError(getString(R.string.nc_text_required))
    }

    private fun showError(message: String) {
        binding.errorText.isVisible = true
        binding.errorText.text = message
    }

    private fun hideError() {
        binding.errorText.isVisible = false
    }

    private fun openEstimatedFeeScreen(address: String, privateNote: String) {
        hideError()
        navigator.openEstimatedFeeScreen(
            activityContext = this,
            walletId = args.walletId,
            outputAmount = args.outputAmount,
            availableAmount = args.availableAmount,
            address = address,
            privateNote = privateNote,
            subtractFeeFromAmount = args.subtractFeeFromAmount
        )
    }

    companion object {
        private const val MAX_NOTE_LENGTH = 80

        fun start(activityContext: Context, walletId: String, outputAmount: Double, availableAmount: Double, subtractFeeFromAmount: Boolean = false) {
            activityContext.startActivity(
                AddReceiptArgs(
                    walletId = walletId,
                    outputAmount = outputAmount,
                    availableAmount = availableAmount,
                    subtractFeeFromAmount = subtractFeeFromAmount
                ).buildIntent(activityContext)
            )
        }

    }

}