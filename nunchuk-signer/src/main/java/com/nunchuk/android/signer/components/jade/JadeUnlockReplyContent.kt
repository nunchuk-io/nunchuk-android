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
package com.nunchuk.android.signer.components.jade

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.DELAY_DYNAMIC_QR
import com.nunchuk.android.signer.R
import kotlinx.coroutines.delay

/**
 * Shows the PIN-server reply back to Jade. Jade may need more than one exchange, so the primary
 * action returns to the scanner rather than closing the flow.
 */
@Composable
internal fun JadeUnlockReplyContent(
    qrs: List<Bitmap> = emptyList(),
    onScanNextClicked: () -> Unit = {},
    onDoneClicked: () -> Unit = {},
) {
    var index by remember(qrs) { mutableIntStateOf(0) }
    LaunchedEffect(qrs) {
        if (qrs.size <= 1) return@LaunchedEffect
        while (true) {
            delay(DELAY_DYNAMIC_QR)
            index = (index + 1) % qrs.size
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_jade_qr_unlock),
                    onBackPress = onDoneClicked,
                )
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp)) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onScanNextClicked,
                    ) { Text(text = stringResource(R.string.nc_jade_qr_unlock_scan_next)) }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.nc_jade_qr_unlock_show_desc),
                    style = NunchukTheme.typography.body,
                    textAlign = TextAlign.Center,
                )
                qrs.getOrNull(index)?.let { bitmap ->
                    Image(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .size(280.dp),
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                    )
                }
                if (qrs.size > 1) {
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = "${index + 1}/${qrs.size}",
                        style = NunchukTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
