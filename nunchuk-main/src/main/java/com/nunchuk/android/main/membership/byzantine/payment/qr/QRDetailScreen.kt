package com.nunchuk.android.main.membership.byzantine.payment.qr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.main.R


@Composable
fun QrDetailRoute(
    address: String = "",
) {
    QrDetailScreen(address = address)
}

@Composable
fun QrDetailScreen(
    address: String = "",
) {
    val context = LocalContext.current
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = "Address",
                textStyle = NunchukTheme.typography.titleLarge,
                isBack = false,
            )
        }, bottomBar = {
            NcOutlineButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .height(48.dp),
                onClick = {
                    context.copyToClipboard(label = "Nunchuk", text = address)
                }
            ) {
                Text(text = stringResource(id = R.string.nc_address_copy))
            }
        }) { innerpadding ->
            val qrCode = produceState<Bitmap?>(initialValue = null, address) {
                value = address.convertToQRCode()
            }
            Column(
                modifier = Modifier
                    .padding(innerpadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                qrCode.value?.let {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                color = MaterialTheme.colorScheme.border,
                                RoundedCornerShape(8.dp)
                            ),
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Qr code",
                        contentScale = ContentScale.FillWidth
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = address,
                    style = NunchukTheme.typography.body,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview
@Composable
private fun QrDetailScreenPreview() {
    QrDetailScreen()
}