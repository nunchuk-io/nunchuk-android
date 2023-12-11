package com.nunchuk.android.main.membership.byzantine.payment.address.wallet

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcExpandableText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.AddressWithQrView
import com.nunchuk.android.main.membership.byzantine.payment.RecurringPaymentViewModel


@Composable
fun PaymentWalletAddressRoute(
    viewModel: RecurringPaymentViewModel,
    openPaymentFrequencyScreen: () -> Unit,
    onOpenQrDetailScreen: (address: String) -> Unit,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val onBackPressDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    PaymentWalletAddressScreen(
        openPaymentFrequencyScreen = openPaymentFrequencyScreen,
        address = config.addresses.firstOrNull().orEmpty(),
        bsms = config.bsms.orEmpty(),
        onOpenQrDetailScreen = onOpenQrDetailScreen,
        onRemoveWalletConfig = {
            viewModel.clearAddressInfo()
            onBackPressDispatcher?.onBackPressed()
        },
    )
}

@Composable
fun PaymentWalletAddressScreen(
    address: String = "",
    bsms: String = "",
    openPaymentFrequencyScreen: () -> Unit = {},
    onOpenQrDetailScreen: (address: String) -> Unit = {},
    onRemoveWalletConfig: () -> Unit = {},
) {
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
                AddressWithQrView(address = address, onOpenQrDetailScreen)

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_configuration_details),
                    style = NunchukTheme.typography.title,
                )

                NcExpandableText(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = NunchukTheme.shape.medium
                        )
                        .padding(16.dp),
                    text = bsms,
                    style = NunchukTheme.typography.body,
                    maxLines = 6
                )

                TextButton(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(alignment = Alignment.CenterHorizontally),
                    onClick = onRemoveWalletConfig
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Remove icon",
                    )

                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(R.string.nc_remove_wallet_configuration),
                        style = NunchukTheme.typography.title,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PaymentWalletAddressScreenPreview() {
    PaymentWalletAddressScreen(
        address = "bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq",
        bsms = """
            BSMS 1.0
            wsh(sortedmulti(2,[1ca93a6f/48'/0'/0']xpub6BemYiVNp19a2U7mfcsvLqc2a21uB4uzb5N29ZkWLkC9GvqBnx78gyMEWYXWP2HQ5n5GBfrASjSGCeAeQyp2Mzp8Q6TqiWFJm18mMnLFwou/**,[7b71b9cb/48'/0'/0']xpub6BemYiVNp19a2XHMJVJ7PfezcasBxay9xHQARCJhs2JF17peMXaFSmLZgSBLYdHnQ1PEsdhcGgMBCAtiyeExuwCPX6GCpbiGaoCiRY9zCFB/**,[d9f7fd94/48'/0'/0'/2']xpub6DfbWHcDHu8KZFbkVmkbRLwHC3nrTBjfnhi6PraWLFUZXC2bUpDVGxdqSsMa9pf8uqBtTS1ubvSPQ7Y8ekCcFw49wRr3zTZe6zLgQMWkbE1/**,[3ffb11f0/48'/0'/0']xpub6CoG2ePDNbvxercgQyLsgPaGEiJHueeFKgkmJfmwLqthzWYU94gtiQDxn5wM93vGvBwpEoomiiAHKSkDX63jqnQ5rLMQCtoCz3g8iEBqBQf/**))
            /0/*,/1/*
            bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq
        """.trimIndent(),
    )
}