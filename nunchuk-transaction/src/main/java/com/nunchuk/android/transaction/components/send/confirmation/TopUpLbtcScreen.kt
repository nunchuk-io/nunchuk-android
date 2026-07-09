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

package com.nunchuk.android.transaction.components.send.confirmation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.getLbtcTokenAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.view.InspectAddressBottomSheet

/**
 * "Top up LBTC" screen shown when the user taps "Add funds" on the not-enough-LBTC banner of the
 * Liquid confirm screen. It surfaces a receive address (with QR) so the user can deposit at least
 * the required LBTC amount to continue the pending transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopUpLbtcScreen(
    fee: Amount = Amount(0),
    address: String = "",
    onBack: () -> Unit = {},
    onShareAddress: (String) -> Unit = {},
    onCopyAddress: (String) -> Unit = {},
) {
    var inspectAddress by rememberSaveable { mutableStateOf<String?>(null) }

    if (inspectAddress != null) {
        InspectAddressBottomSheet(
            address = inspectAddress.orEmpty(),
            onCopy = { onCopyAddress(it) },
            onDismiss = { inspectAddress = null },
        )
    }

    Scaffold(
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_top_up_lbtc),
                textStyle = NunchukTheme.typography.titleLarge,
                onBackPress = onBack,
            )
        },
        bottomBar = {
            if (address.isNotEmpty()) {
                Column {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        onClick = { onShareAddress(address) },
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
                        onClick = { onCopyAddress(address) },
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
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            NcHighlightText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                text = stringResource(R.string.nc_top_up_lbtc_desc, fee.getLbtcTokenAmount()),
                style = NunchukTheme.typography.body.copy(textAlign = TextAlign.Center),
            )

            if (address.isNotEmpty()) {
                TopUpAddressCard(
                    address = address,
                    modifier = Modifier.padding(top = 24.dp),
                    onInspectClick = { inspectAddress = address },
                )
            }
        }
    }
}

@Composable
private fun TopUpAddressCard(
    address: String,
    modifier: Modifier = Modifier,
    onInspectClick: () -> Unit = {},
) {
    val formattedAddress = remember(address) { address.chunked(4).joinToString(" ") }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.strokePrimary,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                text = stringResource(R.string.nc_network_liquid),
                style = NunchukTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textPrimary,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                val qrSize = with(LocalDensity.current) { maxWidth.toPx().toInt() }
                val qrCode = produceState<Bitmap?>(initialValue = null, address, qrSize) {
                    value = address.convertToQRCode(width = qrSize, height = qrSize)
                }
                qrCode.value?.let { bitmap ->
                    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = imageBitmap,
                        contentDescription = "QR Code",
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                text = formattedAddress,
                style = NunchukTheme.typography.body,
                color = MaterialTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Center,
            )
            NcOutlineButton(
                modifier = Modifier.padding(bottom = 8.dp),
                onClick = onInspectClick,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(com.nunchuk.android.core.R.string.nc_inspect),
                    color = MaterialTheme.colorScheme.textPrimary,
                    style = NunchukTheme.typography.title,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TopUpLbtcScreenPreview() {
    NunchukTheme {
        TopUpLbtcScreen(
            fee = Amount(200),
            address = "VJLHqyVovkmKLGmEFQjj6naTrFHMKbcxZ6FecTAw3Vi7Hg9FatCXKT5F7M6LZp7hvinMHoV3KEijtss",
        )
    }
}
