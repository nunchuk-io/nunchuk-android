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

package com.nunchuk.android.transaction.components.send.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.controlFillTertiary
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.transaction.R

@Composable
internal fun InputStablecoinAmountScreen(
    state: InputStablecoinAmountState,
    onClose: () -> Unit,
    onScanQrClicked: () -> Unit,
    onTokenSelected: (StablecoinToken) -> Unit,
    onSendAllClicked: () -> Unit,
    onSwitchCurrencyClicked: () -> Unit,
    onContinueClicked: () -> Unit,
    onInputChanged: (String) -> Unit,
) {
    val unitLabel = if (state.useToken) state.selectedToken.label() else USD_CURRENCY
    val secondaryCurrency = if (state.useToken) {
        "$${state.amountUsd.formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)} $USD_CURRENCY"
    } else {
        "${state.amountToken.formatDecimalWithoutZero(maxFractionDigits = MAX_DECIMAL_DIGITS)} ${state.selectedToken.label()}"
    }
    val switchTargetUnit = if (state.useToken) USD_CURRENCY else state.selectedToken.label()

    NunchukTheme {
        NcScaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_transaction_new),
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = false,
                    onBackPress = onClose,
                    actions = {
                        IconButton(onClick = onScanQrClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_qr),
                                contentDescription = stringResource(R.string.nc_scan_qr),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                TokenSwitcher(
                    selected = state.selectedToken,
                    onSelected = onTokenSelected,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.CenterHorizontally),
                )

                CurrencyInputBlock(
                    inputText = state.inputText,
                    unitLabel = unitLabel,
                    maxDecimalDigits = if (state.useToken) state.selectedToken.maxDecimals() else USD_FRACTION_DIGITS,
                    secondaryCurrency = secondaryCurrency,
                    onInputChanged = onInputChanged,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .clickable(onClick = onSendAllClicked)
                            .padding(16.dp),
                        text = stringResource(R.string.nc_transaction_send_all),
                        style = NunchukTheme.typography.bodySmall,
                        textDecoration = TextDecoration.Underline,
                    )

                    Row(
                        modifier = Modifier
                            .clickable(onClick = onSwitchCurrencyClicked)
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.padding(end = 8.dp),
                            painter = painterResource(id = R.drawable.ic_switch),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.textPrimary,
                        )
                        Text(
                            text = stringResource(
                                R.string.nc_transaction_switch_to_currency_data,
                                switchTargetUnit,
                            ),
                            style = NunchukTheme.typography.bodySmall,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }

                BalanceFeeRow(
                    selectedToken = state.selectedToken,
                    usdtBalance = state.usdtBalance,
                    usdtBalanceUsd = state.usdtBalanceUsd,
                    lbtcBalance = state.lbtcBalance,
                    lbtcBalanceUsd = state.lbtcBalanceUsd,
                    networkFeeLbtc = state.networkFeeLbtc,
                    networkFeeUsd = state.networkFeeUsd,
                )
            }
        }
    }
}

@Composable
private fun TokenSwitcher(
    selected: StablecoinToken,
    onSelected: (StablecoinToken) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackBg = MaterialTheme.colorScheme.controlFillTertiary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(trackBg)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StablecoinToken.entries.forEach { token ->
            SwitcherChip(
                label = token.label(),
                isSelected = selected == token,
                onClick = { onSelected(token) },
            )
        }
    }
}

@Composable
private fun SwitcherChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val activeBg = MaterialTheme.colorScheme.controlFillPrimary
    val activeText = MaterialTheme.colorScheme.controlTextPrimary
    val inactiveText = MaterialTheme.colorScheme.textSecondary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (isSelected) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = NunchukTheme.typography.title.copy(
                color = if (isSelected) activeText else inactiveText,
            ),
        )
    }
}

@Composable
private fun BalanceFeeRow(
    selectedToken: StablecoinToken,
    usdtBalance: Double,
    usdtBalanceUsd: Double,
    lbtcBalance: Double,
    lbtcBalanceUsd: Double,
    networkFeeLbtc: Double,
    networkFeeUsd: Double,
) {
    val (balanceAmount, balanceUsd, tokenLabel) = when (selectedToken) {
        StablecoinToken.USDT -> Triple(usdtBalance, usdtBalanceUsd, StablecoinToken.USDT.label())
        StablecoinToken.LBTC -> Triple(lbtcBalance, lbtcBalanceUsd, StablecoinToken.LBTC.label())
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InfoColumn(
            title = stringResource(R.string.nc_transaction_balance),
            valueLine = "${balanceAmount.formatDecimalWithoutZero(maxFractionDigits = MAX_DECIMAL_DIGITS)} $tokenLabel ($${balanceUsd.formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)})",
        )
        InfoColumn(
            title = stringResource(R.string.nc_network_fee),
            valueLine = "${networkFeeLbtc.formatDecimalWithoutZero(maxFractionDigits = MAX_DECIMAL_DIGITS)} ${StablecoinToken.LBTC.label()} ($${networkFeeUsd.formatDecimalWithoutZero(maxFractionDigits = USD_FRACTION_DIGITS)})",
        )
    }
}

@Composable
private fun InfoColumn(
    title: String,
    valueLine: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = NunchukTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = valueLine,
            style = NunchukTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StablecoinToken.label(): String = when (this) {
    StablecoinToken.USDT -> stringResource(R.string.nc_usdt)
    StablecoinToken.LBTC -> stringResource(R.string.nc_lbtc)
}

private fun StablecoinToken.maxDecimals(): Int = when (this) {
    StablecoinToken.USDT -> 2
    StablecoinToken.LBTC -> 8
}

private const val MAX_DECIMAL_DIGITS = 8

@PreviewLightDark
@Composable
private fun InputStablecoinAmountScreenUsdtPreview() {
    InputStablecoinAmountScreen(
        state = InputStablecoinAmountState(selectedToken = StablecoinToken.USDT),
        onClose = {},
        onScanQrClicked = {},
        onTokenSelected = {},
        onSendAllClicked = {},
        onSwitchCurrencyClicked = {},
        onContinueClicked = {},
        onInputChanged = {},
    )
}

@PreviewLightDark
@Composable
private fun InputStablecoinAmountScreenLbtcPreview() {
    InputStablecoinAmountScreen(
        state = InputStablecoinAmountState(selectedToken = StablecoinToken.LBTC),
        onClose = {},
        onScanQrClicked = {},
        onTokenSelected = {},
        onSendAllClicked = {},
        onSwitchCurrencyClicked = {},
        onContinueClicked = {},
        onInputChanged = {},
    )
}
