package com.nunchuk.android.compose.wallet

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.qr.convertToQRCode

@Composable
fun AddressWithQrView(
    address: String = "",
    openQRDetailScreen: (address: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val qrSize = with(LocalDensity.current) { 40.dp.toPx().toInt() }
    val qrCode = produceState<Bitmap?>(initialValue = null, address) {
        value = address.convertToQRCode(width = qrSize, height = qrSize)
    }
    Row(
        modifier = modifier
            .padding(top = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = NunchukTheme.shape.medium
            )
            .clickable { openQRDetailScreen(address) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        qrCode.value?.let {
            Image(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
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
}

@Composable
@Preview
private fun AddressWithQrViewPreview() {
    NunchukTheme {
        AddressWithQrView(
            address = "bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq"
        )
    }
}