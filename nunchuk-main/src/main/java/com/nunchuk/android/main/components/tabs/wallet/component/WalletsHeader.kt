package com.nunchuk.android.main.components.tabs.wallet.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundLightGray
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.main.R
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus

@Composable
fun WalletsHeader(
    modifier: Modifier = Modifier,
    status: ConnectionStatus?,
    chain: Chain,
    onAddWalletClick: () -> Unit
) {
    val context = LocalContext.current
    val statusText = stringResource(
        R.string.nc_text_home_wallet_connection,
        getStatusText(context, status),
        getChainText(context, chain)
    )
    val statusColor = when (status) {
        ConnectionStatus.OFFLINE -> colorResource(R.color.nc_color_connection_offline)
        ConnectionStatus.ONLINE -> colorResource(R.color.nc_color_connection_online)
        else -> colorResource(R.color.nc_color_connection_syncing)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.nc_title_wallets),
            style = NunchukTheme.typography.titleLarge,
        )

        if (status != null) {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.backgroundLightGray,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = statusColor,
                            shape = CircleShape
                        )
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = statusText,
                    style = NunchukTheme.typography.caption,
                    color = MaterialTheme.colorScheme.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        NcIcon(
            modifier = Modifier
                .clickable(onClick = onAddWalletClick)
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_plus),
            contentDescription = "Add Wallet"
        )
    }
}

private fun getChainText(context: Context, chain: Chain): String {
    return when (chain) {
        Chain.TESTNET -> context.getString(R.string.nc_text_home_wallet_chain_testnet)
        Chain.SIGNET -> context.getString(R.string.nc_text_home_wallet_chain_signet)
        else -> ""
    }
}

private fun getStatusText(context: Context, status: ConnectionStatus?): String {
    return when (status) {
        ConnectionStatus.OFFLINE -> context.getString(R.string.nc_text_connection_status_offline)
        ConnectionStatus.ONLINE -> context.getString(R.string.nc_text_connection_status_online)
        ConnectionStatus.SYNCING -> context.getString(R.string.nc_text_connection_status_syncing)
        else -> ""
    }
}

@Preview(showBackground = true)
@Composable
fun WalletsHeaderPreview() {
    NunchukTheme {
        WalletsHeader(
            status = ConnectionStatus.ONLINE,
            chain = Chain.MAIN,
            onAddWalletClick = {}
        )
    }
}