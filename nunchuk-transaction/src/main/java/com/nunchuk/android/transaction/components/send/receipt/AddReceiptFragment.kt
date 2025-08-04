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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.data.model.isInheritanceClaimFlow
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.util.MAX_NOTE_LENGTH
import com.nunchuk.android.core.wallet.WalletBottomSheetResult
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.model.Amount
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmViewModel
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeViewModel
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AcceptedAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.AddressRequiredEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.InvalidAddressEvent
import com.nunchuk.android.transaction.components.send.receipt.AddReceiptEvent.ShowError
import com.nunchuk.android.transaction.components.utils.toTitle
import com.nunchuk.android.transaction.databinding.ActivityTransactionAddReceiptBinding
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.textChanges
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setMaxLength
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class AddReceiptFragment : BaseFragment<ActivityTransactionAddReceiptBinding>() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: AddReceiptArgs by lazy { AddReceiptArgs.deserializeFrom(Intent().putExtras(requireArguments())) }

    private val viewModel: AddReceiptViewModel by activityViewModels()
    private val estimateFeeViewModel: EstimatedFeeViewModel by activityViewModels()
    private val transactionConfirmViewModel by activityViewModels<TransactionConfirmViewModel>()

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            viewModel.parseBtcUri(content)
        }
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ActivityTransactionAddReceiptBinding = ActivityTransactionAddReceiptBinding.inflate(
        inflater,
        container,
        false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
        viewModel.init(args)

        childFragmentManager.setFragmentResultListener(
            WalletComposeBottomSheet.TAG,
            this
        ) { _, bundle ->
            val result = bundle.parcelable<WalletBottomSheetResult>(WalletComposeBottomSheet.RESULT)
                ?: return@setFragmentResultListener
            if (result.walletId != null) {
                viewModel.getFirstUnusedAddress(walletId = result.walletId!!)
            } else {
                viewModel.updateAddress(result.savedAddress?.address.orEmpty())
            }
            updateSelectAddressView(
                isSelectMode = false,
                walletName = result.walletName,
                savedAddressLabel = result.savedAddress?.label
            )
            childFragmentManager.clearFragmentResult(WalletComposeBottomSheet.TAG)
        }
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun setupViews() {
        binding.toolbarTitle.text =
            args.sweepType.toTitle(requireActivity(), getString(R.string.nc_transaction_new))
        if (args.sweepType != SweepType.NONE) {
            binding.receiptLabel.text = getString(R.string.nc_enter_recipient_address)
        }
        if (isWithdrawFlow().not()) {
            binding.toolbar.inflateMenu(R.menu.menu_scan_qr)
        }
        binding.receiptInput.onTextPaste = {
            viewModel.parseBtcUri(binding.receiptInput.text.toString())
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
        binding.containerPrivateNote.isVisible = isWithdrawFlow().not()
        binding.privateNoteInput.setMaxLength(MAX_NOTE_LENGTH)

        binding.toolbar.setOnMenuItemClickListener {
            startQRCodeScan(launcher)
            true
        }
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }
        binding.btnCreateTransaction.setOnDebounceClickListener {
            viewModel.handleContinueEvent(true)
            transactionConfirmViewModel.setCustomizeTransaction(false)
        }
        binding.btnCustomFee.setOnDebounceClickListener {
            transactionConfirmViewModel.setCustomizeTransaction(true)
            viewModel.handleContinueEvent(false)
        }
        binding.receiptInputDropdown.setOnDebounceClickListener {
            if (binding.receiptSelectLayout.isVisible) {
                updateSelectAddressView(true)
            } else {
                WalletComposeBottomSheet.show(
                    childFragmentManager,
                    exclusiveAssistedWalletIds = arrayListOf(args.walletId),
                    exclusiveAddresses = arrayListOf(viewModel.getAddReceiptState().address),
                    configArgs = WalletComposeBottomSheet.ConfigArgs(
                        flags = WalletComposeBottomSheet.fromFlags(
                            WalletComposeBottomSheet.SHOW_ADDRESS,
                        )
                    )
                )
            }
        }

        binding.receiptInputDropdown.isVisible = isWithdrawFlow().not()
        binding.receiptInputQrCode.isVisible = isWithdrawFlow()
        binding.receiptInputQrCode.setOnDebounceClickListener {
            startQRCodeScan(launcher)
        }
    }

    private fun isWithdrawFlow(): Boolean {
        return args.slots.isNotEmpty() || args.claimInheritanceTxParam.isInheritanceClaimFlow()
    }

    private fun updateSelectAddressView(
        isSelectMode: Boolean,
        walletName: String? = null,
        savedAddressLabel: String? = null,
        isFromParse: Boolean = false
    ) {
        binding.receiptSelectLayout.isVisible = isSelectMode.not()
        binding.receiptSelectLabel.isVisible = isSelectMode.not()
        binding.receiptInput.isVisible = isSelectMode
        if (isSelectMode) {
            if (isFromParse.not()) viewModel.updateAddress("")
            binding.receiptInputDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
        } else {
            binding.receiptSelectLabel.text = walletName ?: savedAddressLabel
            if (walletName != null) {
                binding.receiptInputImage.setImageResource(R.drawable.ic_wallet_small)
            } else {
                binding.receiptInputImage.setImageResource(R.drawable.ic_saved_address)
            }
            binding.receiptInputDropdown.setImageResource(R.drawable.ic_close_circle)
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
                if (event.isCreateTransaction || event.isMiniscript) {
                    handleCreateTransaction()
                } else {
                    openEstimatedFeeScreen(event.address, event.privateNote, event.amount)
                }
            }

            AddressRequiredEvent -> showAddressRequiredError()
            InvalidAddressEvent -> showInvalidAddressError()
            is ShowError -> NCToastMessage(requireActivity()).showError(event.message)
            AddReceiptEvent.ParseBtcUriEvent -> updateSelectAddressView(
                isSelectMode = true,
                isFromParse = true
            )

            AddReceiptEvent.NoOp -> Unit
        }
        if (event !is AddReceiptEvent.NoOp) {
            viewModel.setEventHandled()
        }
    }

    private fun handleCreateTransaction() {
        estimateFeeViewModel.getEstimateFeeRates(false)
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
        (activity as? AddReceiptActivity)?.openEstimatedFeeScreen(
            address = address,
            privateNote = privateNote,
            amount = amount,
        )
    }
}