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

package com.nunchuk.android.transaction.components.receive.address.unused

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.view.InspectAddressBottomSheet

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun UnusedAddressContent(
    addresses: List<String> = emptyList(),
    onAddressClick: (String) -> Unit = {},
    onGenerateAddressClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onCopyAddress: (String) -> Unit = {},
    onMoreClick: () -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
) {
    // pageCount = addresses + 1 for the "generate new" card
    val pageCount = addresses.size + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val hasAddresses = addresses.isNotEmpty()

    var inspectAddress by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPage) {
        onPageChanged(currentPage)
    }

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

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) { page ->
                if (page < addresses.size) {
                    AddressCard(
                        address = addresses[page],
                        onClick = { onAddressClick(addresses[page]) },
                        onInspectClick = { inspectAddress = addresses[page] },
                    )
                } else {
                    GenerateAddressCard(
                        onClick = onGenerateAddressClick,
                    )
                }
            }

            if (hasAddresses) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${currentPage + 1}/${addresses.size} address",
                        style = NunchukTheme.typography.body,
                        color = MaterialTheme.colorScheme.textPrimary,
                    )
                    Icon(
                        modifier = Modifier.clickable(onClick = onMoreClick),
                        painter = painterResource(id = com.nunchuk.android.widget.R.drawable.ic_more_white),
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.textPrimary,
                    )
                }
            }
        }

        if (hasAddresses) {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                onClick = onShareClick,
            ) {
                Icon(
                    painter = painterResource(id = com.nunchuk.android.widget.R.drawable.ic_share),
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = stringResource(com.nunchuk.android.core.R.string.nc_address_share),
                )
            }

            NcOutlineButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                onClick = onCopyClick,
            ) {
                Icon(
                    painter = painterResource(id = com.nunchuk.android.widget.R.drawable.ic_copy),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary,
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = stringResource(com.nunchuk.android.core.R.string.nc_address_copy),
                    color = MaterialTheme.colorScheme.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun AddressCard(
    address: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onInspectClick: () -> Unit = {},
) {
    val qrSize = with(LocalDensity.current) { 300.dp.toPx().toInt() }
    val qrCode = produceState<Bitmap?>(initialValue = null, address) {
        value = address.convertToQRCode(width = qrSize, height = qrSize)
    }
    val formattedAddress = remember(address) {
        address.chunked(4).joinToString(" ")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.strokePrimary,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            qrCode.value?.let { bitmap ->
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                Image(
                    modifier = Modifier.size(300.dp),
                    bitmap = imageBitmap,
                    contentDescription = "QR Code",
                )
            }
            Text(
                modifier = Modifier
                    .width(300.dp)
                    .padding(vertical = 8.dp),
                text = formattedAddress,
                style = NunchukTheme.typography.body,
                color = MaterialTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Center,
            )
            NcOutlineButton(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = onInspectClick,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(com.nunchuk.android.core.R.string.nc_inspect),
                    color = MaterialTheme.colorScheme.textPrimary,
                    style = NunchukTheme.typography.title
                )
            }
        }
    }
}

@Composable
private fun GenerateAddressCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.strokePrimary,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(
                        color = MaterialTheme.colorScheme.whisper,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus_big),
                    contentDescription = "Generate address",
                    tint = MaterialTheme.colorScheme.textPrimary,
                )
            }
            Text(
                modifier = Modifier
                    .padding(vertical = 8.dp),
                text = stringResource(R.string.nc_address_generate_address),
                style = NunchukTheme.typography.title,
                color = MaterialTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun UnusedAddressContentPreview() {
    NunchukTheme {
        UnusedAddressContent(
            addresses = listOf(
                "bc1qepuayeutds0ys0q82g3ucad7r0eqk0dpusmxwvlpn5wkjcmhv6sqyulylk",
                "bc1qft5swsj3nnm48qcpwvsqmht899gf6zk28tvvq6pdkjekaq3rltaqkztttq",
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun UnusedAddressContentEmptyPreview() {
    NunchukTheme {
        UnusedAddressContent()
    }
}
