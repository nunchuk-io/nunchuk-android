package com.nunchuk.android.transaction.components.send.batchtransaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.navArgs
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.compose.InputSwitchCurrencyView
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.core.util.MAX_FRACTION_DIGITS
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeArgs
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeViewModel
import com.nunchuk.android.utils.MaxLengthTransformation
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BatchTransactionFragment : Fragment() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: BatchTransactionViewModel by viewModels()
    private val estimatedFeeViewModel: EstimatedFeeViewModel by viewModels()
    private val args: BatchTransactionFragmentArgs by navArgs()

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            viewModel.parseBtcUri(content)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                BatchTransactionScreen(viewModel, onScanClick = {
                    startQRCodeScan(launcher)
                })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { event ->
                when (event) {
                    is BatchTransactionEvent.Error -> showError(message = event.message)
                    is BatchTransactionEvent.Loading -> showOrHideLoading(loading = event.loading)
                    BatchTransactionEvent.InsufficientFundsEvent -> {
                        if (args.unspentOutputs.isNotEmpty()) {
                            NCToastMessage(requireActivity()).showError(getString(R.string.nc_send_amount_too_large))
                        } else {
                            NCToastMessage(requireActivity()).showError(getString(R.string.nc_transaction_insufficient_funds))
                        }
                    }

                    BatchTransactionEvent.InsufficientFundsLockedCoinEvent -> showUnlockCoinBeforeSend()
                    is BatchTransactionEvent.CheckAddressSuccess -> {
                        if (event.isCustomTx) {
                            openEstimatedFeeScreen()
                        } else {
                            estimatedFeeViewModel.init(
                                EstimatedFeeArgs(
                                    walletId = args.walletId,
                                    txReceipts = viewModel.getTxReceiptList(),
                                    availableAmount = args.availableAmount.toDouble(),
                                    privateNote = viewModel.getNote(),
                                    subtractFeeFromAmount = false,
                                    slots = emptyList(),
                                    masterSignerId = "",
                                    magicalPhrase = "",
                                    inputs = args.unspentOutputs.toList()
                                )
                            )
                        }
                    }
                }
            }
        }
        estimatedFeeViewModel.event.observe(requireActivity()) { event ->
            when (event) {
                is EstimatedFeeEvent.EstimatedFeeErrorEvent -> NCToastMessage(requireActivity()).showError(
                    event.message
                )

                is EstimatedFeeEvent.EstimatedFeeCompletedEvent -> openTransactionConfirmScreen(
                    estimatedFee = event.estimatedFee,
                    subtractFeeFromAmount = event.subtractFeeFromAmount,
                    manualFeeRate = event.manualFeeRate
                )

                is EstimatedFeeEvent.Loading -> showOrHideLoading(event.isLoading)
                EstimatedFeeEvent.DraftTransactionSuccess -> estimatedFeeViewModel.handleContinueEvent()
                else -> {}
            }
        }

    }

    private fun showUnlockCoinBeforeSend() {
        NCInfoDialog(requireActivity())
            .showDialog(message = getString(R.string.nc_send_all_locked_coin_msg))
    }

    private fun openEstimatedFeeScreen() {
        navigator.openEstimatedFeeScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
            availableAmount = args.availableAmount.toDouble(),
            txReceipts = viewModel.getTxReceiptList(),
            privateNote = viewModel.getNote(),
            subtractFeeFromAmount = false,
            slots = emptyList(),
            masterSignerId = "",
            magicalPhrase = "",
            inputs = args.unspentOutputs.toList()
        )
    }

    private fun openTransactionConfirmScreen(
        estimatedFee: Double,
        subtractFeeFromAmount: Boolean,
        manualFeeRate: Int
    ) {
        navigator.openTransactionConfirmScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
            txReceipts = viewModel.getTxReceiptList(),
            availableAmount = args.availableAmount.toDouble(),
            privateNote = viewModel.getNote(),
            estimatedFee = estimatedFee,
            subtractFeeFromAmount = subtractFeeFromAmount,
            manualFeeRate = manualFeeRate,
            slots = emptyList(),
            masterSignerId = "",
            magicalPhrase = "",
            inputs = args.unspentOutputs.toList()
        )
    }

    companion object {
        const val LIMIT_NOTE = 280
    }
}

@Composable
private fun BatchTransactionScreen(
    viewModel: BatchTransactionViewModel = viewModel(),
    onScanClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BatchTransactionContent(
        recipientList = state.recipients,
        note = state.note,
        isEnableRemoveRecipient = viewModel.isEnableRemoveRecipient(),
        onInputAmountChange = { index, amount ->
            viewModel.updateRecipient(index, amount = amount)
        },
        onSwitchBtcAndCurrency = { index, isBtc ->
            viewModel.updateRecipient(
                index, isBtc = isBtc
            )
        },
        onInputAddressChange = { index, address ->
            viewModel.updateRecipient(index, address = address)
        },
        onScanClick = {
            viewModel.setInteractingIndex(it)
            onScanClick()
        },
        isEnableCreateTransaction = viewModel.isEnableCreateTransaction(),
        onInputNoteChange = viewModel::updateNoteChange,
        onAddRecipient = viewModel::addRecipient,
        onRemoveRecipient = viewModel::removeRecipient,
        onCreateTransactionClick = {
            viewModel.createTransaction(false)
        },
        onCustomizeTransactionClick = {
            viewModel.createTransaction(true)
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BatchTransactionContent(
    recipientList: List<BatchTransactionState.Recipient> = emptyList(),
    note: String = "",
    isEnableCreateTransaction: Boolean = false,
    isEnableRemoveRecipient: Boolean = false,
    onAddRecipient: () -> Unit = {},
    onRemoveRecipient: (Int) -> Unit = {},
    onInputNoteChange: (String) -> Unit = {},
    onCreateTransactionClick: () -> Unit = {},
    onCustomizeTransactionClick: () -> Unit = {},
    onInputAmountChange: (Int, String) -> Unit = { _, _ -> },
    onScanClick: (Int) -> Unit = {},
    onSwitchBtcAndCurrency: (Int, Boolean) -> Unit = { _, _ -> },
    onInputAddressChange: (Int, String) -> Unit = { _, _ -> }
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_batched_transaction),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    }
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(recipientList) { index, recipient ->
                        RecipientView(index = index + 1,
                            address = recipient.address,
                            amount = recipient.amount,
                            isBtc = recipient.isBtc,
                            error = recipient.error,
                            enableRemove = isEnableRemoveRecipient,
                            onRemoveClick = {
                                onRemoveRecipient(index)
                            },
                            onScanClick = {
                                onScanClick(index)
                            },
                            onInputAmountChange = {
                                onInputAmountChange(
                                    index, CurrencyFormatter.format(it, MAX_FRACTION_DIGITS)
                                )
                            },
                            onSwitchBtcAndCurrency = {
                                onSwitchBtcAndCurrency(index, it)
                            },
                            onInputAddressChange = {
                                onInputAddressChange(index, it)
                            })
                    }
                    item {
                        NcOutlineButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(48.dp),
                            onClick = onAddRecipient,
                        ) {
                            Row {
                                Image(
                                    painterResource(id = R.drawable.ic_plus_dark),
                                    contentDescription = null,
                                )

                                Text(
                                    modifier = Modifier.padding(start = 6.dp),
                                    text = stringResource(id = R.string.nc_add_recipient)
                                )
                            }
                        }
                    }
                    item {
                        NcTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            title = stringResource(id = R.string.nc_transaction_note),
                            value = note,
                            maxLength = BatchTransactionFragment.LIMIT_NOTE,
                            enableMaxLength = true,
                            visualTransformation = MaxLengthTransformation(maxLength = BatchTransactionFragment.LIMIT_NOTE),
                            onValueChange = {
                                if (it.length <= BatchTransactionFragment.LIMIT_NOTE) {
                                    onInputNoteChange(it)
                                }
                            },
                            onFocusEvent = { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        delay(500L)
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                        )
                    }
                }
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .fillMaxWidth()
                        .padding(16.dp), onCreateTransactionClick,
                    enabled = isEnableCreateTransaction
                ) {
                    Text(text = stringResource(id = R.string.nc_create_transaction))
                }
                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .height(48.dp),
                    onClick = onCustomizeTransactionClick,
                    enabled = isEnableCreateTransaction
                ) {
                    Text(text = stringResource(R.string.nc_customize_transaction))
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecipientView(
    index: Int = 0,
    address: String = "",
    amount: String = "",
    isBtc: Boolean = true,
    error: String = "",
    enableRemove: Boolean = false,
    onRemoveClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onInputAmountChange: (String) -> Unit = {},
    onInputAddressChange: (String) -> Unit = {},
    onSwitchBtcAndCurrency: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.nc_recipient_data, index),
                style = NunchukTheme.typography.title
            )
            Text(
                modifier = Modifier.clickable(enabled = enableRemove) {
                    onRemoveClick()
                },
                text = stringResource(id = R.string.nc_remove),
                style = NunchukTheme.typography.title,
                textDecoration = TextDecoration.Underline,
                color = if (enableRemove) colorResource(id = R.color.nc_primary_color) else colorResource(
                    id = R.color.nc_grey_dark_color
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp, color = NcColor.kinglyCloud, shape = RoundedCornerShape(12.dp)
                )
                .background(color = NcColor.greyLight)
                .padding(16.dp)
        ) {
            Column {
                NcTextField(
                    title = stringResource(id = R.string.nc_address),
                    value = address,
                    rightContent = {
                        Image(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable {
                                    onScanClick()
                                },
                            painter = painterResource(id = R.drawable.ic_qrcode_dark),
                            contentDescription = ""
                        )
                    },
                    error = error,
                    onValueChange = onInputAddressChange
                )
                InputSwitchCurrencyView(
                    title = stringResource(id = R.string.nc_amount),
                    isBtc = isBtc,
                    currencyValue = amount,
                    onSwitchBtcAndCurrency = onSwitchBtcAndCurrency,
                    onValueChange = onInputAmountChange
                )
            }
        }
    }
}

@Preview
@Composable
private fun BatchTransactionScreenPreview() {
    BatchTransactionContent(

    )
}