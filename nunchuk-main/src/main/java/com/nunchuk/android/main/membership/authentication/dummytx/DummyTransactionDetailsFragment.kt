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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nunchuk.android.main.databinding.FragmentDummyTransactionDetailsBinding
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationEvent
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationState
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationViewModel
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class DummyTransactionDetailsFragment : BaseFragment<FragmentDummyTransactionDetailsBinding>(),
    BottomSheetOptionListener {
    private val viewModel: DummyTransactionDetailsViewModel by viewModels()
    private val walletAuthenticationViewModel: WalletAuthenticationViewModel by activityViewModels()
    private val nfcViewModel: NfcViewModel by activityViewModels()

    private val importTxLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null && it.resultCode == Activity.RESULT_OK) {
                val transaction = data.parcelable<Transaction>(GlobalResultKey.TRANSACTION_EXTRA)
                    ?: return@registerForActivityResult
                walletAuthenticationViewModel.handleImportAirgapTransaction(transaction)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            TransactionOption.EXPORT_KEYSTONE.ordinal -> openExportTransactionScreen(
                TransactionOption.EXPORT_KEYSTONE
            )
            TransactionOption.IMPORT_KEYSTONE.ordinal -> openImportTransactionScreen(
                TransactionOption.IMPORT_KEYSTONE,
            )
            TransactionOption.EXPORT_PASSPORT.ordinal -> openExportTransactionScreen(
                TransactionOption.EXPORT_PASSPORT
            )
            TransactionOption.IMPORT_PASSPORT.ordinal -> openImportTransactionScreen(
                TransactionOption.IMPORT_PASSPORT,
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
                                    HashMap(event.signatures)
                                )
                            })
                            requireActivity().finish()
                        }
                        is WalletAuthenticationEvent.Loading -> showOrHideLoading(event.isLoading)
                        is WalletAuthenticationEvent.ScanTapSigner -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_NFC_SIGN_TRANSACTION
                        )
                        WalletAuthenticationEvent.ScanColdCard -> (requireActivity() as NfcActionListener).startNfcFlow(
                            BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION
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
                        WalletAuthenticationEvent.ExportTransactionToColdcardSuccess -> handleExportToColdcardSuccess()
                        WalletAuthenticationEvent.CanNotSignDummyTx -> showError(getString(R.string.nc_can_not_sign_please_try_again))
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

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_EXPORT_TRANSACTION }) { scanInfo ->
            walletAuthenticationViewModel.handleExportTransactionToMk4(Ndef.get(scanInfo.tag) ?: return@flowObserver)
            nfcViewModel.clearScanInfo()
        }

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE }) {
            walletAuthenticationViewModel.getInteractSingleSigner()?.let { signer ->
                walletAuthenticationViewModel.generateSignatureFromColdCardPsbt(signer, it.records)
            }
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleExportToColdcardSuccess() {
        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_MK4_IMPORT_SIGNATURE)
        showSuccess(getString(com.nunchuk.android.transaction.R.string.nc_transaction_exported))
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
            state.signatures.mapValues { true },
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
        val output = if (transaction.isReceive) {
            transaction.receiveOutputs.firstOrNull()
        } else {
            transaction.outputs.firstOrNull()
        }
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
        binding.status.bindTransactionStatus(transaction)
        binding.sendingBTC.text = transaction.totalAmount.getBTCAmount()
        binding.signersContainer.isVisible = !transaction.isReceive

        bindAddress(transaction)
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

    private fun openExportTransactionScreen(transactionOption: TransactionOption) {
        navigator.openExportTransactionScreen(
            launcher = importTxLauncher,
            activityContext = requireActivity(),
            txToSign = walletAuthenticationViewModel.getDataToSign(),
            transactionOption = transactionOption,
            isDummyTx = true,
            walletId = walletAuthenticationViewModel.getWalletId()
        )
    }

    private fun openImportTransactionScreen(
        transactionOption: TransactionOption,
    ) {
        navigator.openImportTransactionScreen(
            launcher = importTxLauncher,
            activityContext = requireActivity(),
            transactionOption = transactionOption,
            walletId = walletAuthenticationViewModel.getWalletId(),
            isDummyTx = true
        )
    }

    private fun showError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).showError(message)
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDummyTransactionDetailsBinding {
        return FragmentDummyTransactionDetailsBinding.inflate(inflater, container, false)
    }
}