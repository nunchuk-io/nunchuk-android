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

package com.nunchuk.android.transaction.components.details.fee

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.sheet.BottomSheetTooltip
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmContent
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmReplaceTransactionFragment : Fragment() {
    private val viewModel by viewModels<ConfirmReplaceTransactionViewModel>()
    private val activityArgs: ReplaceFeeArgs by lazy {
        ReplaceFeeArgs.deserializeFrom(requireActivity().intent)
    }
    private val args by navArgs<ConfirmReplaceTransactionFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ConfirmReplaceTransactionScreen(
                    viewModel = viewModel,
                    onBackPressed = { activity?.onBackPressedDispatcher?.onBackPressed() },
                    onConfirmClick = {
                        if (args.address.isNullOrEmpty()) {
                            viewModel.replaceTransaction(
                                walletId = activityArgs.walletId,
                                txId = activityArgs.transaction.txId,
                                newFee = args.newFee,
                                signingPath = activityArgs.signingPath,
                                isUseScriptPath = activityArgs.isUseSciptPath,
                            )
                        } else {
                            viewModel.createTransaction(
                                walletId = activityArgs.walletId,
                                oldTx = activityArgs.transaction,
                                newFee = args.newFee,
                                address = args.address.orEmpty()
                            )
                        }
                    },
                    onCopyText = { content ->
                        requireActivity().copyToClipboard(label = "Nunchuk", text = content)
                        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_copied_to_clipboard))
                    },
                    onEstimatedFeeInfoClick = {
                        BottomSheetTooltip.newInstance(
                            title = getString(R.string.nc_text_info),
                            message = getString(R.string.nc_estimated_fee_tooltip),
                        ).show(childFragmentManager, "BottomSheetTooltip")
                    },
                    onReplaceSuccess = { newTxId ->
                        requireActivity().setResult(
                            Activity.RESULT_OK,
                            activityArgs.copy(transaction = activityArgs.transaction.copy(txId = newTxId))
                                .buildIntent(requireActivity())
                        )
                        requireActivity().finish()
                    },
                    onShowError = { message ->
                        NCToastMessage(requireActivity()).showError(message)
                    },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(activityArgs.walletId, activityArgs.transaction, args.antiFeeSniping)
        if (args.address.isNullOrEmpty()) {
            viewModel.draftTransaction(
                walletId = activityArgs.walletId,
                oldTx = activityArgs.transaction,
                newFee = args.newFee,
                signingPath = activityArgs.signingPath,
                useSciptPath = activityArgs.isUseSciptPath
            )
        } else {
            viewModel.draftCancelTransaction(
                activityArgs.walletId,
                activityArgs.transaction,
                args.newFee,
                args.address.orEmpty()
            )
        }
    }
}

@Composable
private fun ConfirmReplaceTransactionScreen(
    viewModel: ConfirmReplaceTransactionViewModel,
    onBackPressed: () -> Unit,
    onConfirmClick: () -> Unit,
    onCopyText: (String) -> Unit,
    onEstimatedFeeInfoClick: () -> Unit,
    onReplaceSuccess: (String) -> Unit,
    onShowError: (String) -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var isLoading by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ReplaceFeeEvent.Loading -> isLoading = event.isLoading
                is ReplaceFeeEvent.ReplaceTransactionSuccess -> {
                    isLoading = false
                    onReplaceSuccess(event.newTxId)
                }
                is ReplaceFeeEvent.ShowError -> {
                    isLoading = false
                    onShowError(event.e?.message.orUnknownError())
                }
                is ReplaceFeeEvent.DraftTransactionSuccess -> Unit
            }
        }
    }

    val transaction = uiState.transaction ?: return
    val outputs = transaction.outputs.filterIndexed { index, _ -> index != transaction.changeIndex }
    val txOutput = transaction.outputs.getOrNull(transaction.changeIndex)
    val changeAddress = txOutput?.first.orEmpty()

    NunchukTheme {
        if (isLoading) {
            NcLoadingDialog()
        }
        TransactionConfirmContent(
            title = stringResource(R.string.nc_transaction_confirm_transaction),
            confirmButtonText = stringResource(R.string.nc_transaction_confirm_and_create_transaction),
            outputs = outputs,
            savedAddresses = uiState.savedAddress,
            fee = transaction.fee,
            totalAmountBtc = transaction.totalAmount.pureBTC().getBTCAmount(),
            totalAmountCurrency = transaction.totalAmount.pureBTC().getCurrencyAmount(),
            changeAddress = changeAddress,
            changeAmount = txOutput?.second ?: com.nunchuk.android.model.Amount(0),
            privateNote = transaction.memo,
            inputs = uiState.inputCoins,
            allTags = uiState.allTags,
            onBackPressed = onBackPressed,
            onConfirmClick = onConfirmClick,
            onCopyText = onCopyText,
            onEstimatedFeeInfoClick = onEstimatedFeeInfoClick,
        )
    }
}
