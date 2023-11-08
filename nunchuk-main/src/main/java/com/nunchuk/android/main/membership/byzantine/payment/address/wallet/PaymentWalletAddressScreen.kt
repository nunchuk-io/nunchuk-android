package com.nunchuk.android.main.membership.byzantine.payment.address.wallet

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel


@Composable
fun PaymentWalletAddressRoute(
    viewModel: RecurringPaymentViewModel,
    address: String = "bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq",
    walletContent: String = "",
    openPaymentFrequencyScreen: () -> Unit,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    PaymentWalletAddressScreen(
        openPaymentFrequencyScreen = openPaymentFrequencyScreen,
        address = address,
        walletContent = walletContent,
    )
}

@Composable
fun PaymentWalletAddressScreen(
    address: String = "",
    walletContent: String = "",
    openPaymentFrequencyScreen: () -> Unit = {},
) {
    val qrSize = with(LocalDensity.current) { 40.dp.toPx().toInt() }
    val qrCode = produceState<Bitmap?>(initialValue = null, address) {
        value = address.convertToQRCode(width = qrSize, height = qrSize)
    }
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_add_recurring_payments),
                textStyle = NunchukTheme.typography.titleLarge,
            )
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = openPaymentFrequencyScreen,
            ) {
                Text(text = stringResource(R.string.nc_text_continue))
            }
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.nc_first_address_of_wallet),
                    style = NunchukTheme.typography.title
                )
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = NunchukTheme.shape.medium
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    qrCode.value?.let {
                        Image(
                            modifier = Modifier
                                .size(40.dp)
                                .border(
                                    1.dp,
                                    color = MaterialTheme.colorScheme.border,
                                    RoundedCornerShape(8.dp),
                                ),
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Qr Code",
                        )
                    }
                    Text(
                        modifier = Modifier.padding(start = 12.dp),
                        text = address, style = NunchukTheme.typography.body
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_configuration_details),
                    style = NunchukTheme.typography.title,
                )


            }
        }
    }
}

@Preview
@Composable
fun PaymentWalletAddressScreenPreview() {
    PaymentWalletAddressScreen(
        address = "bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq",
        walletContent = "0x1234567890",
    )
}