/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.transaction.components.receive.address.used

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.view.AddressWithInspect
import com.nunchuk.android.transaction.components.details.view.InspectAddressBottomSheet
import com.nunchuk.android.transaction.components.receive.address.UsedAddressModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UsedAddressContent(
    addresses: List<UsedAddressModel> = emptyList(),
    onAddressClick: (UsedAddressModel) -> Unit = {},
    onCopyAddress: (String) -> Unit = {},
) {
    var inspectAddress by remember { mutableStateOf<String?>(null) }

    if (inspectAddress != null) {
        InspectAddressBottomSheet(
            address = inspectAddress!!,
            onCopy = { address ->
                onCopyAddress(address)
                inspectAddress = null
            },
            onDismiss = { inspectAddress = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            text = stringResource(R.string.nc_address_used_introduction),
            style = NunchukTheme.typography.body,
            color = MaterialTheme.colorScheme.textPrimary,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(addresses) { item ->
                UsedAddressItem(
                    model = item,
                    onClick = { onAddressClick(item) },
                    onInspectClick = { inspectAddress = item.address },
                )
            }
        }
    }
}

@Composable
private fun UsedAddressItem(
    model: UsedAddressModel,
    onClick: () -> Unit = {},
    onInspectClick: () -> Unit = {},
) {
    val qrSize = with(LocalDensity.current) { 60.dp.toPx().toInt() }
    val qrCode = produceState<Bitmap?>(initialValue = null, model.address) {
        value = model.address.convertToQRCode(width = qrSize, height = qrSize)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        qrCode.value?.let { bitmap ->
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        color = MaterialTheme.colorScheme.border,
                        RoundedCornerShape(4.dp),
                    ),
                bitmap = imageBitmap,
                contentDescription = "QR Code",
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            AddressWithInspect(
                address = model.address,
                onCopyText = {},
                onInspectAddress = { onInspectClick() },
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = model.balance.getBTCAmount(),
                style = NunchukTheme.typography.title,
                color = MaterialTheme.colorScheme.textPrimary,
            )
        }
        Icon(
            painter = painterResource(id = com.nunchuk.android.widget.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary,
        )
    }
}

@PreviewLightDark
@Composable
private fun UsedAddressContentPreview() {
    NunchukTheme {
        UsedAddressContent(
            addresses = listOf(
                UsedAddressModel(
                    address = "bc1qepuayeutds0ys0q82g3ucad7r0eqk0dpusmxwvlpn5wkjcmhv6sqyulylk",
                ),
                UsedAddressModel(
                    address = "bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq",
                ),
            ),
        )
    }
}
