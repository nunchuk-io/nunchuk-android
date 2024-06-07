@file:OptIn(ExperimentalComposeUiApi::class)

package com.nunchuk.android.transaction.components.invoice

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.transaction.R
import com.nunchuk.android.utils.BitmapUtil.combineBitmapsVertically
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
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
                InvoiceScreen(viewModel, invoiceInfo) { bitmaps ->
                    viewModel.saveBitmapToPDF(bitmaps.map { it.asAndroidBitmap() }, invoiceInfo.transactionId)
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
    onSaveClick: (List<ImageBitmap>) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InvoiceScreenContent(
        invoiceInfo = invoiceInfo,
        onSaveClick = onSaveClick,
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeApi::class)
@Composable
fun InvoiceScreenContent(
    invoiceInfo: InvoiceInfo,
    onSaveClick: (List<ImageBitmap>) -> Unit = {},
) {

    val captureController = listOf(rememberCaptureController(), rememberCaptureController())
    val scope = rememberCoroutineScope()

    NunchukTheme {
        Scaffold(modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
            topBar = {
                NcTopAppBar(title = "Invoice",
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
                        onClick = {
                            scope.launch {
                                val bitmaps = captureController.map { it.captureAsync(Bitmap.Config.ARGB_8888) }.awaitAll()
                                try {
                                    onSaveClick(bitmaps)
                                } catch (_: Throwable) {
                                }
                            }
                        },
                    ) {
                        Text(
                            text = "Save PDF",
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
//                    .verticalScroll(rememberScrollState())
            ) {
                LazyColumn {
                    item {
                        Column(
                            modifier = Modifier
                                .background(color = NcColor.greyLight)
                                .capturable(captureController[0])
                                .fillMaxSize()
                        ) {
                            Text(
                                text = "Amount sent",
                                style = NunchukTheme.typography.body.copy(color = Color.Black),
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
                                text = "Transaction ID",
                                style = NunchukTheme.typography.titleSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = colorResource(id = R.color.nc_whisper_color))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            Text(
                                text = invoiceInfo.transactionId,
                                style = NunchukTheme.typography.title,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                            )

                            Text(
                                text = "Send to address",
                                style = NunchukTheme.typography.titleSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = colorResource(id = R.color.nc_whisper_color))
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
                                color = NcColor.border,
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
                                        text = "Transaction fee",
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

                            Row(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 24.dp
                                )
                            ) {
                                Text(
                                    text = "Total amount",
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

                    item {
                        Column(
                            modifier = Modifier
                                .background(color = NcColor.greyLight)
                                .capturable(captureController[1])
                                .fillMaxWidth()
                        ) {
                            if (invoiceInfo.changeAddress.isNotEmpty() && invoiceInfo.changeAddressAmount.isNotEmpty()) {
                                Text(
                                    text = "Change address",
                                    style = NunchukTheme.typography.titleSmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(color = colorResource(id = R.color.nc_whisper_color))
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
                                    .background(color = colorResource(id = R.color.nc_whisper_color))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )

                            Text(
                                text = "Private note",
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                            )
                            Text(
                                text = invoiceInfo.note,
                                style = NunchukTheme.typography.body,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
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
            txOutputs = emptyList()
        )
    )
}
