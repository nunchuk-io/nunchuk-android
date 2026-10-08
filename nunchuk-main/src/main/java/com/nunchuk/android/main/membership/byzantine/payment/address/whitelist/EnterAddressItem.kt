package com.nunchuk.android.main.membership.byzantine.payment.address.whitelist

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.main.R

@Composable
fun EnterAddressItem(
    modifier: Modifier = Modifier,
    index: Int = 0,
    value: String = "",
    onValueChange: (String) -> Unit = { },
    onRemoveAddress: (Int) -> Unit = {},
    openScanQrCode: (Int) -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = stringResource(R.string.nc_membership_address_number, index.inc()), style = NunchukTheme.typography.title)
            Text(
                modifier = Modifier
                    .clickable { onRemoveAddress(index) },
                text = stringResource(R.string.nc_remove),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
            )
        }

        NcTextField(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.greyLight,
                    shape = NunchukTheme.shape.medium
                )
                .padding(16.dp),
            title = "",
            value = value,
            onValueChange = onValueChange,
            minLines = 3,
            rightContent = {
                Image(
                    modifier = Modifier
                        .clickable { openScanQrCode(index) }
                        .padding(12.dp),
                    painter = painterResource(id = R.drawable.ic_qr),
                    contentDescription = stringResource(R.string.nc_membership_cd_qr_code)
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EnterAddressItemPreview() {
    NunchukTheme {
        EnterAddressItem()
    }
}