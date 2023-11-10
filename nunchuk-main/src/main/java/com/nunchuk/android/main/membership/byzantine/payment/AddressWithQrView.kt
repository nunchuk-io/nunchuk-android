package com.nunchuk.android.main.membership.byzantine.payment

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.core.qr.convertToQRCode

@Composable
fun AddressWithQrView(
    address: String = "",
) {
    val qrSize = with(LocalDensity.current) { 40.dp.toPx().toInt() }
    val qrCode = produceState<Bitmap?>(initialValue = null, address) {
        value = address.convertToQRCode(width = qrSize, height = qrSize)
    }
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
}