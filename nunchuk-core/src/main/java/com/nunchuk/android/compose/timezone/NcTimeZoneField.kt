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

package com.nunchuk.android.compose.timezone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.fillInputText
import com.nunchuk.android.core.R
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.TimeZoneSelectionDialog

@Composable
fun NcTimeZoneField(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.nc_time_zone),
    selectedTimeZone: TimeZoneDetail,
    onTimeZoneSelected: (TimeZoneDetail) -> Unit,
    placeholder: String = "Select Time zone"
) {
    var showTimeZoneDialog by remember { mutableStateOf(false) }

    val displayText = if (selectedTimeZone.city.isNotEmpty()) {
        "${selectedTimeZone.city} (${selectedTimeZone.offset})"
    } else {
        placeholder
    }

    NcTextField(
        modifier = modifier,
        title = title,
        value = displayText,
        readOnly = true,
        enabled = false,
        disableBackgroundColor = MaterialTheme.colorScheme.fillInputText,
        onClick = {
            showTimeZoneDialog = true
        },
        rightContent = {
            Icon(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable {
                        showTimeZoneDialog = true
                    },
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = ""
            )
        },
        onValueChange = {}
    )

    if (showTimeZoneDialog) {
        TimeZoneSelectionDialog(
            onDismissRequest = { showTimeZoneDialog = false },
            onTimeZoneSelected = { timeZone ->
                onTimeZoneSelected(timeZone)
                showTimeZoneDialog = false
            }
        )
    }
}





