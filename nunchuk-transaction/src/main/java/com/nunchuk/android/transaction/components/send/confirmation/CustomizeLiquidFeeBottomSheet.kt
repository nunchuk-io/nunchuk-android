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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.formatFiatDecimal
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.fromBTCtoSAT
import com.nunchuk.android.core.util.MAX_FRACTION_DIGITS
import com.nunchuk.android.core.util.getDisplayCurrency
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.transaction.R
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomizeLiquidFeeBottomSheet(
    currentFee: Amount,
    minimumFeeSats: Long = 0L,
    error: String? = null,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    val initial = remember(currentFee) {
        currentFee.pureBTC()
            .formatDecimalWithoutZero(maxFractionDigits = MAX_FRACTION_DIGITS)
    }
    var text by rememberSaveable(initial) { mutableStateOf(initial) }
    // Set when the user taps Apply with a fee below the minimum; cleared as soon as they edit.
    var belowMinimumError by remember { mutableStateOf(false) }
    val lbtcValue = text.replace(',', '.').toDoubleOrNull() ?: 0.0
    val feeSats = round(lbtcValue.fromBTCtoSAT()).toLong()
    val displayError = if (belowMinimumError) {
        stringResource(R.string.nc_input_fee_invalid_error)
    } else {
        error
    }
    val usdLabel = "${getDisplayCurrency()}${lbtcValue.fromBTCToCurrency().formatFiatDecimal()}"

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        dragHandle = { },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.nc_customize_fee_in_lbtc),
                style = NunchukTheme.typography.title,
            )
            NcTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                title = "",
                value = text,
                error = displayError,
                onValueChange = {
                    text = sanitizeLbtcInput(it)
                    belowMinimumError = false
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                rightContent = {
                    Text(
                        modifier = Modifier.padding(end = 12.dp),
                        text = usdLabel,
                        style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary),
                    )
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NcOutlineButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    onClick = onDismiss,
                ) {
                    Text(text = stringResource(R.string.nc_cancel))
                }
                NcPrimaryDarkButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (minimumFeeSats > 0 && feeSats < minimumFeeSats) {
                            belowMinimumError = true
                        } else {
                            belowMinimumError = false
                            onApply(feeSats)
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.nc_apply))
                }
            }
        }
    }
}

private fun sanitizeLbtcInput(raw: String): String {
    val normalized = raw.replace(',', '.')
    val builder = StringBuilder()
    var seenDot = false
    var fractionDigits = 0
    for (ch in normalized) {
        when {
            ch.isDigit() -> {
                if (seenDot) {
                    if (fractionDigits < MAX_FRACTION_DIGITS) {
                        builder.append(ch)
                        fractionDigits++
                    }
                } else {
                    builder.append(ch)
                }
            }

            ch == '.' && !seenDot -> {
                if (builder.isEmpty()) builder.append('0')
                builder.append('.')
                seenDot = true
            }
        }
    }
    return builder.toString()
}
