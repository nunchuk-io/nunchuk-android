package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.wallet.R

@Composable
internal fun EmptyTransactionFacilitatorAdminView(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        text = stringResource(R.string.nc_you_dont_have_any_transactions),
        style = NunchukTheme.typography.body.copy(
            color = MaterialTheme.colorScheme.textPrimary
        ),
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun EmptyTransactionView(
    address: String,
    onCopyAddress: (String) -> Unit,
    onShareAddress: (String) -> Unit,
    isStableWallet: Boolean = false,
) {
    val qrBitmap = remember(address) {
        if (address.isNotEmpty()) address.convertToQRCode() else null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NcHighlightText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 4.dp),
            text = stringResource(
                if (isStableWallet) R.string.nc_stable_transaction_empty_title
                else R.string.nc_transaction_empty_title
            ),
            style = NunchukTheme.typography.body.copy(
                color = MaterialTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.size(16.dp))
        if (qrBitmap != null) {
            Box(
                modifier = Modifier
                    .background(color = Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(10.dp),
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Receive address QR",
                    modifier = Modifier.size(180.dp),
                )
            }
        }
        Spacer(Modifier.size(16.dp))
        if (address.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 4.dp),
                text = address,
                style = NunchukTheme.typography.body.copy(
                    color = MaterialTheme.colorScheme.textPrimary
                ),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.size(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val textContent = MaterialTheme.colorScheme.textPrimary
            val primaryContent = colorResource(com.nunchuk.android.core.R.color.nc_control_text_primary)
            Row(
                modifier = Modifier
                    .clickable { onCopyAddress(address) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                    tint = textContent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.nc_address_copy),
                    style = NunchukTheme.typography.captionTitle.copy(color = textContent),
                )
            }
            NcPrimaryDarkButton(
                height = 44.dp,
                onClick = { onShareAddress(address) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    tint = primaryContent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.nc_address_share),
                    style = NunchukTheme.typography.captionTitle.copy(color = primaryContent),
                )
            }
        }
    }
}
