/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.main.membership.authentication.dummytx

import android.app.Activity
import android.content.Intent
import android.nfc.tech.Ndef
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.FragmentTransactionDetailsBinding
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationEvent
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationState
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationViewModel
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class DummyTransactionDetailsFragment : BaseFragment<FragmentTransactionDetailsBinding>(),
    BottomSheetOptionListener {
    private val viewModel: DummyTransactionDetailsViewModel by viewModels()
    private val walletAuthenticationViewModel: WalletAuthenticationViewModel by activityViewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    // TODO Hai
    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            TransactionOption.EXPORT_KEYSTONE.ordinal -> openExportTransactionScreen(
                TransactionOption.EXPORT_KEYSTONE
            )
            TransactionOption.IMPORT_KEYSTONE.ordinal -> openImportTransactionScreen(
                TransactionOption.IMPORT_KEYSTONE,
                walletAuthenticationViewModel.getInteractSingleSigner()?.masterSignerId.orEmpty()
            )
            TransactionOption.EXPORT_PASSPORT.ordinal -> openExportTransactionScreen(
                TransactionOption.EXPORT_PASSPORT
            )
            TransactionOption.IMPORT_PASSPORT.ordinal -> openImportTransactionScreen(
                TransactionOption.IMPORT_PASSPORT,
                walletAuthenticationViewModel.getInteractSingleSigner()?.masterSignerId.orEmpty()
            )
        }
    }

    private fun observeEvent() {
        flowObserver(walletAuthenticationViewModel.state, ::handleState)
        flowObserver(viewModel.state) {
            handleViewMore(it.viewMore)
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            walletAuthenticationViewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is WalletAuthenticationEvent.WalletAuthenticationSuccess -> {
                            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(
                                    GlobalResultKey.SIGNATURE_EXTRA,
                                    event.signatures
                                )
                            })
                            requireActivity().finish()
                        }
                        is WalletAuthenticationEvent.Loading -> showOrHideLoading(event.isLoading)
                        is WalletAuthenticationEvent.ScanTapSigner -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                        )
                        WalletAuthenticationEvent.ScanColdCard -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG
                        )
                        is WalletAuthenticationEvent.ProcessFailure -> showError(event.message)
                        WalletAuthenticationEvent.GenerateColdcardHealthMessagesSuccess -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE
                        )
                        is WalletAuthenticationEvent.NfcLoading -> showOrHideNfcLoading(
                            event.isLoading,
                            event.isColdCard
                        )
                        is WalletAuthenticationEvent.ShowError -> showError(event.message)
                        WalletAuthenticationEvent.ShowAirgapOption -> handleMenuMore()
                    }
                }
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION }) { info ->
            walletAuthenticationViewModel.getInteractSingleSigner()?.let {
                walletAuthenticationViewModel.handleTapSignerSignCheckMessage(
                    it,
                    info,
                    nfcViewModel.inputCvc.orEmpty()
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_GENERATE_HEAL_CHECK_MSG }) { scanInfo ->
            walletAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                walletAuthenticationViewModel.generateColdcardHealthMessages(
                    Ndef.get(scanInfo.tag),
                    signer.derivationPath
                )
            }
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE }) {
            walletAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                walletAuthenticationViewModel.healthCheckColdCard(signer, it.records)
            }
            nfcViewModel.clearScanInfo()
        }
    }

    private fun setupViews() {
        binding.viewMore.setOnClickListener {
            viewModel.handleViewMoreEvent()
        }
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = TransactionOption.IMPORT_KEYSTONE.ordinal,
                    resId = R.drawable.ic_import,
                    label = getString(R.string.nc_transaction_import_signature),
                ),
                SheetOption(
                    type = TransactionOption.EXPORT_KEYSTONE.ordinal,
                    resId = R.drawable.ic_export,
                    label = getString(R.string.nc_transaction_export_transaction),
                ),
                SheetOption(
                    type = TransactionOption.IMPORT_PASSPORT.ordinal,
                    resId = R.drawable.ic_import,
                    label = getString(R.string.nc_transaction_import_passport_signature),
                ),
                SheetOption(
                    type = TransactionOption.EXPORT_PASSPORT.ordinal,
                    resId = R.drawable.ic_export,
                    label = getString(R.string.nc_transaction_export_passport_transaction),
                ),
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun handleState(state: WalletAuthenticationState) {
        val transaction = state.transaction ?: return
        bindTransaction(transaction)
        bindSigners(
            transaction.signers,
            state.walletSigner.sortedByDescending(SignerModel::localKey),
            transaction.status
        )
        hideLoading()
    }

    private fun handleViewMore(viewMore: Boolean) {
        binding.viewMore.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            if (viewMore) ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_collapse
            ) else ContextCompat.getDrawable(requireContext(), R.drawable.ic_expand),
            null
        )
        binding.viewMore.text = if (viewMore) {
            getString(R.string.nc_transaction_less_details)
        } else {
            getString(R.string.nc_transaction_more_details)
        }

        binding.transactionDetailsContainer.isVisible = viewMore
    }

    private fun bindSigners(
        signerMap: Map<String, Boolean>,
        signers: List<SignerModel>,
        status: TransactionStatus
    ) {
        TransactionSignersViewBinder(
            container = binding.signerListView,
            signerMap = signerMap,
            signers = signers,
            txStatus = status,
            listener = { signer ->
                walletAuthenticationViewModel.onSignerSelect(signer)
            }
        ).bindItems()
    }

    private fun bindTransaction(transaction: Transaction) {
        binding.tvReplaceByFee.isVisible = transaction.replacedTxid.isNotEmpty()
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
        binding.noteContent.text = transaction.memo.ifEmpty { getString(R.string.nc_none) }
        binding.sendingTo.text = output?.first.orEmpty().truncatedAddress()
        binding.signatureStatus.isVisible = !transaction.status.hadBroadcast()
        val pendingSigners = transaction.getPendingSignatures()
        if (pendingSigners > 0) {
            binding.signatureStatus.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pending_signatures,
                0,
                0,
                0
            )
            binding.signatureStatus.text = resources.getQuantityString(
                R.plurals.nc_transaction_pending_signature,
                pendingSigners,
                pendingSigners
            )
        } else {
            binding.signatureStatus.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_check_circle,
                0,
                0,
                0
            )
            binding.signatureStatus.text = getString(R.string.nc_transaction_enough_signers)
        }
        binding.confirmTime.text = transaction.getFormatDate()
        binding.status.bindTransactionStatus(transaction)
        binding.sendingBTC.text = transaction.totalAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive

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

        binding.sendingToLabel.text = getString(R.string.nc_transaction_sending_to)
        binding.sendToAddress.text = getString(R.string.nc_transaction_send_to_address)
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

    //
//    private fun handleEvent(event: TransactionDetailsEvent) {
//        when (event) {
//            is SignTransactionSuccess -> showSignTransactionSuccess(event)
//            is BroadcastTransactionSuccess -> showBroadcastTransactionSuccess(event.roomId)
//            is DeleteTransactionSuccess -> showTransactionDeleteSuccess(event.isCancel)
//            is ViewBlockchainExplorer -> openExternalLink(event.url)
//            is TransactionDetailsError -> handleSignError(event)
//            is PromptTransactionOptions -> promptTransactionOptions(event)
//            LoadingEvent -> showLoading()
//            is NfcLoadingEvent -> showOrHideNfcLoading(true, event.isColdcard)
//            is ExportToFileSuccess -> showExportToFileSuccess(event)
//            is ExportTransactionError -> showExportToFileError(event)
//            ImportTransactionFromMk4Success -> handleImportTransactionFromMk4Success()
//            ExportTransactionToMk4Success -> handleExportTxToMk4Success()
//        }
//    }
//
//    private fun handleExportTxToMk4Success() {
//        hideLoading()
//        (requireActivity() as NfcActionListener).startNfcFlow(REQUEST_MK4_IMPORT_SIGNATURE)
//        NCToastMessage(requireActivity()).show(getString(R.string.nc_transaction_exported))
//    }
//
//    private fun handleImportTransactionFromMk4Success() {
//        hideLoading()
//        NCToastMessage(requireActivity()).show(getString(R.string.nc_signed_transaction))
//    }
//
//    private fun handleSignError(event: TransactionDetailsError) {
//        hideLoading()
//        if (nfcViewModel.handleNfcError(event.e).not()) showError(event.message)
//    }
//
//    private fun showExportToFileError(event: ExportTransactionError) {
//        hideLoading()
//        NCToastMessage(this).showError(event.message)
//    }
//
//    private fun showExportToFileSuccess(event: ExportToFileSuccess) {
//        hideLoading()
//        controller.shareFile(event.filePath)
//    }
//
//    private fun promptCancelTransactionConfirmation() {
//        NCWarningDialog(this).showDialog(
//            title = getString(R.string.nc_text_confirmation),
//            message = getString(R.string.nc_transaction_confirmation),
//            onYesClick = viewModel::handleDeleteTransactionEvent
//        )
//    }
//
//    private fun promptTransactionOptions(event: PromptTransactionOptions) {
//        TransactionOptionsBottomSheet.show(
//            fragmentManager = supportFragmentManager,
//            isPending = event.isPendingTransaction,
//            isPendingConfirm = event.isPendingConfirm,
//            isRejected = event.isRejected,
//            isAssistedWallet = viewModel.isAssistedWallet()
//        ).setListener {
//            when (it) {
//                CANCEL -> promptCancelTransactionConfirmation()
//                EXPORT -> openExportTransactionScreen(EXPORT)
//                IMPORT_KEYSTONE -> openImportTransactionScreen(
//                    IMPORT_KEYSTONE,
//                    event.masterFingerPrint
//                )
//                EXPORT_PASSPORT -> openExportTransactionScreen(EXPORT_PASSPORT)
//                IMPORT_PASSPORT -> openImportTransactionScreen(
//                    IMPORT_PASSPORT,
//                    event.masterFingerPrint
//                )
//                EXPORT_PSBT -> viewModel.exportTransactionToFile()
//                REPLACE_BY_FEE -> handleOpenEditFee()
//                COPY_TRANSACTION_ID -> handleCopyContent(args.txId)
//                REMOVE_TRANSACTION -> viewModel.handleDeleteTransactionEvent(false)
//            }
//        }
//    }
//
    private fun openExportTransactionScreen(transactionOption: TransactionOption) {
        navigator.openExportTransactionScreen(
            activityContext = requireActivity(),
            txToSign = walletAuthenticationViewModel.getDataToSign(),
            transactionOption = transactionOption
        )
    }

    //
    private fun openImportTransactionScreen(
        transactionOption: TransactionOption,
        masterFingerPrint: String
    ) {
//        navigator.openImportTransactionScreen(
//            activityContext = requireActivity(),
//            walletId = args.walletId,
//            transactionOption = transactionOption,
//            masterFingerPrint = if (viewModel.isSharedTransaction()) masterFingerPrint else "",
//            initEventId = viewModel.getInitEventId()
//        )
    }

//    private fun showSignTransactionSuccess(event: SignTransactionSuccess) {
//        hideLoading()
//        NCToastMessage(requireActivity()).show(getString(R.string.nc_transaction_signed_successful))
//        lifecycleScope.launch {
//            if (event.isAssistedWallet) {
//                if (event.status == TransactionStatus.READY_TO_BROADCAST) {
//                    delay(3000L)
//                    NCToastMessage(requireActivity()).show(getString(R.string.nc_server_key_signed))
//                }
//                if (event.status == TransactionStatus.PENDING_CONFIRMATION) {
//                    delay(3000L)
//                    NCToastMessage(requireActivity()).show(getString(R.string.nc_server_key_signed))
//                    delay(3000L)
//                    NCToastMessage(requireActivity()).show(getString(R.string.nc_transaction_has_succesfully_broadcast))
//                }
//            }
//        }
//    }

    private fun showError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).showError(message)
    }
//
//    private fun showSignByMk4Options() {
//        BottomSheetOption.newInstance(
//            listOf(
//                SheetOption(
//                    type = SheetOptionType.EXPORT_TX_TO_Mk4,
//                    resId = R.drawable.ic_export,
//                    label = getString(R.string.nc_export_transaction)
//                ),
//                SheetOption(
//                    type = SheetOptionType.IMPORT_TX_FROM_Mk4,
//                    resId = R.drawable.ic_import,
//                    label = getString(R.string.nc_import_signature)
//                ),
//            )
//        ).show(childFragmentManager, "BottomSheetOption")
//    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTransactionDetailsBinding {
        return FragmentTransactionDetailsBinding.inflate(inflater, container, false)
    }
}