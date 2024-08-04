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

package com.nunchuk.android.transaction.components.send.receipt

import android.content.Context
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.util.MAX_NOTE_LENGTH
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.wallet.AssistedWalletBottomSheet
import com.nunchuk.android.core.wallet.WalletBottomSheetResult
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmViewModel
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeViewModel
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AcceptedAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AddressRequiredEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.InvalidAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ShowError
import com.nunchuk.android.transaction.components.utils.openTransactionDetailScreen
import com.nunchuk.android.transaction.components.utils.returnActiveRoom
import com.nunchuk.android.transaction.components.utils.showCreateTransactionError
import com.nunchuk.android.transaction.components.utils.toTitle
import com.nunchuk.android.transaction.databinding.ActivityTransactionAddReceiptBinding
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.textChanges
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setMaxLength
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class AddReceiptActivity : BaseNfcActivity<ActivityTransactionAddReceiptBinding>() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: AddReceiptArgs by lazy { AddReceiptArgs.deserializeFrom(intent) }

    private val viewModel: AddReceiptViewModel by viewModels()
    private val estimateFeeViewModel: EstimatedFeeViewModel by viewModels()
    private val transactionConfirmViewModel: TransactionConfirmViewModel by viewModels()
    private val sweepSatscardViewModel: SweepSatscardViewModel by viewModels()

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            viewModel.parseBtcUri(content)
        }
    }

    override fun initializeBinding() = ActivityTransactionAddReceiptBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.address, args.privateNote)

        supportFragmentManager.setFragmentResultListener(WalletComposeBottomSheet.TAG, this) { _, bundle ->
            val result = bundle.parcelable<WalletBottomSheetResult>(WalletComposeBottomSheet.RESULT) ?: return@setFragmentResultListener
            if (result.walletId != null) {
                viewModel.getFirstUnusedAddress(walletId = result.walletId!!)
            } else {
                viewModel.updateAddress(result.savedAddress?.address.orEmpty())
            }
            updateSelectAddressView(isSelectMode = false, walletName = result.walletName, savedAddressLabel = result.savedAddress?.label)
            supportFragmentManager.clearFragmentResult(WalletComposeBottomSheet.TAG)
        }
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
        estimateFeeViewModel.event.observe(this, ::handleEstimateFeeEvent)
        transactionConfirmViewModel.event.observe(this, ::handleCreateTransactionEvent)
        observerSweepSatscard(sweepSatscardViewModel, nfcViewModel) { args.walletId }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_SATSCARD_SWEEP_SLOT }) {
            sweepSatscardViewModel.init(
                viewModel.getAddReceiptState().address,
                estimateFeeViewModel.defaultRate
            )
            sweepSatscardViewModel.handleSweepBalance(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
                args.slots.toList(),
                args.sweepType
            )
            nfcViewModel.clearScanInfo()
        }
    }

    private fun setupViews() {
        binding.toolbarTitle.text = args.sweepType.toTitle(this, getString(R.string.nc_transaction_new))
        if (args.sweepType != SweepType.NONE) {
            binding.receiptLabel.text = getString(R.string.nc_enter_recipient_address)
        }
        lifecycleScope.launch {
            binding.receiptInput.textChanges()
                .map { it }
                .distinctUntilChanged()
                .collect(viewModel::handleReceiptChanged)
        }
        lifecycleScope.launch {
            binding.privateNoteInput.textChanges()
                .map { it }
                .distinctUntilChanged()
                .collect(viewModel::handlePrivateNoteChanged)
        }
        binding.containerPrivateNote.isVisible = args.slots.isEmpty()
        binding.privateNoteInput.setMaxLength(MAX_NOTE_LENGTH)

        binding.toolbar.setOnMenuItemClickListener {
            startQRCodeScan(launcher)
            true
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnCreateTransaction.setOnDebounceClickListener {
            viewModel.handleContinueEvent(true)
        }
        binding.btnCustomFee.setOnDebounceClickListener {
            viewModel.handleContinueEvent(false)
        }
        binding.receiptInputDropdown.setOnDebounceClickListener {
            if (binding.receiptSelectLayout.isVisible) {
                updateSelectAddressView(true)
            } else {
                WalletComposeBottomSheet.show(
                    supportFragmentManager,
                    exclusiveAssistedWalletIds = arrayListOf(args.walletId),
                    exclusiveAddresses = arrayListOf(viewModel.getAddReceiptState().address),
                    configArgs = WalletComposeBottomSheet.ConfigArgs(
                        isShowAddress = true,
                    )
                )
            }
        }
    }

    private fun updateSelectAddressView(isSelectMode: Boolean, walletName: String? = null, savedAddressLabel: String? = null, isFromParse: Boolean = false) {
        binding.receiptSelectLayout.isVisible = isSelectMode.not()
        binding.receiptSelectLabel.isVisible = isSelectMode.not()
        binding.receiptInput.isVisible = isSelectMode
        if (isSelectMode) {
            if (isFromParse.not()) viewModel.updateAddress("")
            binding.receiptInputDropdown.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.ic_arrow_drop_down))
        } else {
            binding.receiptSelectLabel.text = walletName ?: savedAddressLabel
            if (walletName != null) {
                binding.receiptInputImage.setImageResource(R.drawable.ic_wallet_small)
            } else {
                binding.receiptInputImage.setImageResource(R.drawable.ic_saved_address)
            }
            binding.receiptInputDropdown.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_close_circle
                )
            )
        }
    }

    private fun handleState(state: AddReceiptState) {
        val privateNoteCounter = "${state.privateNote.length}/$MAX_NOTE_LENGTH"
        hideError()
        if (binding.receiptInput.text.toString() != state.address) {
            binding.receiptInput.setText(state.address)
            binding.receiptInput.setSelection(state.address.length)
        }
        if (binding.privateNoteInput.text.toString() != state.privateNote) {
            binding.privateNoteInput.setText(state.privateNote)
        }
        binding.privateNoteCounter.text = privateNoteCounter
    }

    private fun handleEvent(event: AddReceiptEvent) {
        when (event) {
            is AcceptedAddressEvent -> {
                if (event.isCreateTransaction) {
                    handleCreateTransaction()
                } else {
                    openEstimatedFeeScreen(event.address, event.privateNote, event.amount)
                }
            }
            AddressRequiredEvent -> showAddressRequiredError()
            InvalidAddressEvent -> showInvalidAddressError()
            is ShowError -> NCToastMessage(this).showError(event.message)
            AddReceiptEvent.ParseBtcUriEvent -> updateSelectAddressView(isSelectMode = true, isFromParse = true)
        }
    }

    private fun handleEstimateFeeEvent(event: EstimatedFeeEvent) {
        val state = viewModel.getAddReceiptState()
        val amount = state.amount
        val address = state.address
        if (event is EstimatedFeeEvent.GetFeeRateSuccess) {
            handleCreateTransaction(amount, address, state, event)
        } else if (event is EstimatedFeeEvent.EstimatedFeeErrorEvent) {
            showEventError(event.message)
        }
    }

    private fun handleCreateTransactionEvent(event: TransactionConfirmEvent) {
        when (event) {
            is TransactionConfirmEvent.CreateTxErrorEvent -> showCreateTransactionError(event.message)
            is TransactionConfirmEvent.CreateTxSuccessEvent -> {
                hideLoading()
                if (transactionConfirmViewModel.isInheritanceClaimingFlow()) {
                    ActivityManager.popUntilRoot()
                    navigator.openTransactionDetailsScreen(
                        activityContext = this,
                        walletId = "",
                        txId = event.transaction.txId,
                        initEventId = "",
                        roomId = "",
                        transaction = event.transaction,
                        isInheritanceClaimingFlow = true
                    )
                } else {
                    openTransactionDetailScreen(
                        event.transaction.txId,
                        args.walletId,
                        sessionHolder.getActiveRoomIdSafe(),
                        isInheritanceClaimingFlow = false
                    )
                }
            }
            is TransactionConfirmEvent.LoadingEvent -> showLoading(message = if (event.isClaimInheritance) getString(R.string.nc_withdrawal_in_progress) else null)
            is TransactionConfirmEvent.InitRoomTransactionError -> showCreateTransactionError(event.message)
            is TransactionConfirmEvent.InitRoomTransactionSuccess -> returnActiveRoom(event.roomId)
            is TransactionConfirmEvent.UpdateChangeAddress -> {}
            is TransactionConfirmEvent.AssignTagEvent -> {}
            is TransactionConfirmEvent.AssignTagError -> {
                hideLoading()
                NCToastMessage(this).showError(event.message)
            }
            is TransactionConfirmEvent.AssignTagSuccess -> {
                hideLoading()
                NCToastMessage(this).showMessage(getString(R.string.nc_tags_assigned))
                openTransactionDetailScreen(
                    event.txId,
                    args.walletId,
                    sessionHolder.getActiveRoomIdSafe(),
                    transactionConfirmViewModel.isInheritanceClaimingFlow()
                )
            }
            else -> {}
        }
    }

    private fun handleCreateTransaction(
        amount: Amount,
        address: String,
        state: AddReceiptState,
        event: EstimatedFeeEvent.GetFeeRateSuccess
    ) {
        if (args.slots.isNotEmpty()) {
            startNfcFlow(REQUEST_SATSCARD_SWEEP_SLOT)
        } else {
            val finalAmount = if (amount.value > 0) amount.pureBTC() else args.outputAmount
            val subtractFeeFromAmount = if (amount.value > 0) false else args.subtractFeeFromAmount
            val manualFeeRate =
                if (transactionConfirmViewModel.isInheritanceClaimingFlow()) event.estimateFeeRates.priorityRate else event.estimateFeeRates.defaultRate
            transactionConfirmViewModel.init(
                walletId = args.walletId,
                txReceipts = listOf(TxReceipt(address, finalAmount)),
                privateNote = state.privateNote,
                subtractFeeFromAmount = subtractFeeFromAmount,
                slots = args.slots,
                inputs = args.inputs,
                manualFeeRate = manualFeeRate,
                claimInheritanceTxParam = args.claimInheritanceTxParam
            )
            transactionConfirmViewModel.handleConfirmEvent(true)
        }
    }

    private fun showEventError(message: String) {
        NCToastMessage(this).showError(message)
    }

    private fun handleCreateTransaction() {
        estimateFeeViewModel.getEstimateFeeRates()
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

    private fun openEstimatedFeeScreen(address: String, privateNote: String, amount: Amount) {
        hideError()
        val finalAmount = if (amount.value > 0) amount.pureBTC() else args.outputAmount
        val subtractFeeFromAmount = if (amount.value > 0) false else args.subtractFeeFromAmount
        navigator.openEstimatedFeeScreen(
            activityContext = this,
            walletId = args.walletId,
            availableAmount = args.availableAmount,
            txReceipts = listOf(TxReceipt(address, finalAmount)),
            privateNote = privateNote,
            subtractFeeFromAmount = subtractFeeFromAmount,
            sweepType = args.sweepType,
            slots = args.slots,
            inputs = args.inputs,
            claimInheritanceTxParam = args.claimInheritanceTxParam
        )
    }

    companion object {

        fun start(
            activityContext: Context,
            walletId: String,
            outputAmount: Double,
            availableAmount: Double,
            address: String = "",
            privateNote: String = "",
            subtractFeeFromAmount: Boolean = false,
            slots: List<SatsCardSlot> = emptyList(),
            sweepType: SweepType = SweepType.NONE,
            inputs: List<UnspentOutput> = emptyList(),
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
        ) {
            activityContext.startActivity(
                AddReceiptArgs(
                    walletId = walletId,
                    outputAmount = outputAmount,
                    availableAmount = availableAmount,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    slots = slots,
                    address = address,
                    privateNote = privateNote,
                    sweepType = sweepType,
                    inputs = inputs,
                    claimInheritanceTxParam = claimInheritanceTxParam
                ).buildIntent(activityContext)
            )
        }

    }

}