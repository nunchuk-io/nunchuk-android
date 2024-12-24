@file:OptIn(ExperimentalComposeUiApi::class)

package com.nunchuk.android.transaction.components.invoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.wallet.InvoiceInfo
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.transaction.R
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InvoiceFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: InvoiceViewModel by viewModels()

    private val invoiceInfo: InvoiceInfo by lazy {
        requireNotNull(arguments?.parcelable(InvoiceActivity.EXTRA_INVOICE_INFO))
    }

    private val controller: IntentSharingController by lazy(LazyThreadSafetyMode.NONE) {
        IntentSharingController.from(
            requireActivity()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                InvoiceScreen(viewModel, invoiceInfo) {
                    viewModel.exportInvoice(invoiceInfo, invoiceInfo.transactionId)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InvoiceEvent.Error -> {
                    showOrHideLoading(false)
                    showError(event.message)
                }

                is InvoiceEvent.Loading -> {
                    showOrHideLoading(event.loading)
                }

                is InvoiceEvent.ShareFile -> {
                    controller.shareFile(event.filePath)
                }
            }
        }
    }
}

@Composable
fun InvoiceScreen(
    viewModel: InvoiceViewModel = viewModel(),
    invoiceInfo: InvoiceInfo,
    onSaveClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InvoiceScreenContent(
        invoiceInfo = invoiceInfo,
        onSaveClick = onSaveClick,
    )
}

@Composable
fun InvoiceScreenContent(
    invoiceInfo: InvoiceInfo,
    onSaveClick: () -> Unit = {},
) {

    val scope = rememberCoroutineScope()

    NunchukTheme {
        Scaffold(modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
            topBar = {
                NcTopAppBar(title = stringResource(id = R.string.nc_invoice),
                    isBack = false,
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            },
            bottomBar = {
                Column {
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onSaveClick,
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_save_pdf),
                        )
                    }
                }
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .fillMaxSize()
            ) {
                LazyColumn {
                    item {
                        Column(
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.greyLight)
                                .fillMaxSize()
                        ) {
                            Text(
                                text = if (invoiceInfo.isReceive) stringResource(id = R.string.nc_amount_receive) else stringResource(id = R.string.nc_amount_sent),
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                            )

                            Text(
                                text = invoiceInfo.amountSent,
                                style = NunchukTheme.typography.heading,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                            )

                            Text(
                                text = invoiceInfo.confirmTime,
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 24.dp
                                )
                            )

                            Text(
                                text = stringResource(id = R.string.nc_transaction_id),
                                style = NunchukTheme.typography.titleSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = colorResource(id = R.color.nc_bg_mid_gray))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            Text(
                                text = invoiceInfo.transactionId,
                                style = NunchukTheme.typography.title,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            )

                            Text(
                                text = if (invoiceInfo.isReceive) stringResource(id = R.string.nc_transaction_receive_at) else stringResource(id = R.string.nc_transaction_send_to_address),
                                style = NunchukTheme.typography.titleSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = colorResource(id = R.color.nc_bg_mid_gray))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            invoiceInfo.txOutputs.forEach { txOutput ->
                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 24.dp
                                    )
                                ) {
                                    Text(
                                        text = txOutput.first,
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .padding(end = 16.dp)
                                    )

                                    Text(
                                        text = txOutput.second.getBTCAmount(),
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                    )
                                }
                            }

                            Divider(
                                color = MaterialTheme.colorScheme.strokePrimary,
                                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            )

                            if (invoiceInfo.estimatedFee.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.nc_transaction_fee),
                                        style = NunchukTheme.typography.body,
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .padding(end = 16.dp)
                                    )

                                    Text(
                                        text = invoiceInfo.estimatedFee,
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                    )
                                }
                            }
                            
                            if (invoiceInfo.isReceive.not()) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 16.dp,
                                        bottom = 24.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.nc_transaction_total_amount),
                                        style = NunchukTheme.typography.body,
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .padding(end = 16.dp)
                                    )

                                    Text(
                                        text = invoiceInfo.amountSent,
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .background(color = MaterialTheme.colorScheme.greyLight)
                                .fillMaxWidth()
                        ) {
                            if (invoiceInfo.changeAddress.isNotEmpty() && invoiceInfo.changeAddressAmount.isNotEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.nc_transaction_change_address),
                                    style = NunchukTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = colorResource(id = R.color.nc_bg_mid_gray))
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 24.dp,
                                        bottom = 24.dp
                                    )
                                ) {
                                    Text(
                                        text = invoiceInfo.changeAddress,
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                            .weight(1f, fill = true)
                                            .padding(end = 16.dp)
                                    )

                                    Text(
                                        text = invoiceInfo.changeAddressAmount,
                                        style = NunchukTheme.typography.title,
                                        modifier = Modifier
                                    )
                                }
                            }

                            Text(
                                text = "Transaction note",
                                style = NunchukTheme.typography.titleSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = colorResource(id = R.color.nc_bg_mid_gray))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            Text(
                                text = invoiceInfo.note,
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 24.dp,
                                    bottom = 24.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun InvoiceScreenContentPreview() {
    InvoiceScreenContent(
        InvoiceInfo(
            amountSent = "1.01000001 BTC",
            confirmTime = "07/22/2020 at 11:32 PM",
            transactionId = "22fb08b6ffc25cea49cd649710cfeb3923e21eadc44dd8243f93e13e5c3ed413",
            estimatedFee = "1.00000001 BTC",
            changeAddress = "22fb08b6ffc25cea49cd649710cfeb3923e21eadc44dd8243f93e13e5c3ed413",
            changeAddressAmount = "1.00000001 BTC",
            note = "Private note",
            txOutputs = emptyList(),
            isReceive = true
        )
    )
}
