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

package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun TransactionNoteView(modifier: Modifier = Modifier, note: String) {
    var isTextOverFlow by remember { mutableStateOf(false) }
    var isNoteExpand by remember { mutableStateOf(false) }
    val onTextClick = {
        if (isTextOverFlow) {
            isNoteExpand = isNoteExpand.not()
        }
    }
    Row(
        modifier = modifier
            .clickable(onClick = onTextClick),
    ) {
        NcIcon(
            modifier = Modifier
                .border(1.dp, color = MaterialTheme.colorScheme.strokePrimary, shape = CircleShape)
                .padding(4.dp),
            painter = painterResource(id = R.drawable.ic_transaction_note),
            contentDescription = "Transaction Note"
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            NcLinkifyText(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = note,
                maxLines = if (isNoteExpand) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                style = NunchukTheme.typography.bodySmall,
                onTextLayout = {
                    if (it.hasVisualOverflow) {
                        isTextOverFlow = true
                    }
                },
                onClick = onTextClick
            )
            if (isNoteExpand) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.greyLight,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.strokePrimary,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .clickable { isNoteExpand = false },
                    text = stringResource(R.string.nc_show_less),
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
        if (isTextOverFlow && isNoteExpand.not()) {
            Text(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.greyLight,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.strokePrimary,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .clickable { isNoteExpand = true },
                text = stringResource(R.string.nc_more),
                style = NunchukTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionNoteViewPreview() {
    NunchukTheme {
        TransactionNoteView(note = "My name is Jayce")
    }
}