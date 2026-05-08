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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.util.CurrencyFormatter
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatCurrencyDecimal
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getTextBtcUnit
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.transaction.R
import java.text.DecimalFormatSymbols
import java.util.Locale

private val SlimeDark = Color(0xFF1C652D)

// Match the legacy XML's android:maxLength="20" and stay safely under
// Long.MAX_VALUE (19 digits) so NumberCommaTransformation can format the integer part.
private const val MAX_TOTAL_LENGTH = 20
private const val MAX_INTEGER_DIGITS = 16

// NumberCommaTransformation reads from DecimalFormatSymbols.getInstance() — mirror that
// so we never disagree with the visual transformation about which char is the decimal point.
private val decimalSeparator: Char get() = DecimalFormatSymbols.getInstance().decimalSeparator

/**
 * Renders the raw numeric input as `"12,345 BTC"`, keeping the cursor between the number and
 * the suffix (matches the legacy XML where the EditText sat to the left of the BTC label).
 * When the input is empty the suffix is still shown, with the cursor positioned at offset 0 so
 * it doesn't land in the middle of the label.
 */
private class AmountWithSuffixTransformation(private val suffix: String) : VisualTransformation {
    private val gap = if (suffix.isEmpty()) "" else " "

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val numberPart = if (raw.isEmpty()) "" else applyThousandsGrouping(raw)
        // Always include the gap so the cursor doesn't sit flush against the suffix when empty.
        val display = numberPart + gap + suffix
        // Cursor sits right after the number portion (or at 0 when there is none).
        val cursorAt = numberPart.length

        return TransformedText(
            text = AnnotatedString(display),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = cursorAt
                override fun transformedToOriginal(offset: Int): Int = raw.length
            }
        )
    }
}

@Composable
fun InputAmountScreen(
    state: InputAmountState,
    isClaimInheritance: Boolean,
    isFromSelectedCoin: Boolean,
    availableAmount: Double,
    onClose: () -> Unit,
    onScanQrClicked: () -> Unit,
    onBatchTransactionClicked: () -> Unit,
    onSendAllClicked: () -> Unit,
    onSwitchCurrencyClicked: () -> Unit,
    onContinueClicked: () -> Unit,
    onInputChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val unitLabel = if (state.useBtc) context.getTextBtcUnit() else LOCAL_CURRENCY
    val maxDecimalDigits = when {
        !state.useBtc -> 8
        CURRENT_DISPLAY_UNIT_TYPE == SAT -> 0
        else -> 8
    }
    val secondaryCurrency = if (state.useBtc) {
        if (LOCAL_CURRENCY == USD_CURRENCY) {
            state.amountUSD.formatCurrencyDecimal(maxFractionDigits = USD_FRACTION_DIGITS)
        } else {
            "${state.amountUSD.formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)} $LOCAL_CURRENCY"
        }
    } else {
        state.amountBTC.toAmount().getBTCAmount()
    }
    val switchTargetUnit = if (state.useBtc) LOCAL_CURRENCY else context.getTextBtcUnit()

    NunchukTheme {
        NcScaffold(
            topBar = {
                NcTopAppBar(
                    title = if (isClaimInheritance) {
                        stringResource(R.string.nc_withdraw_a_custom_amount)
                    } else {
                        stringResource(R.string.nc_transaction_new)
                    },
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = false,
                    onBackPress = onClose,
                    actions = {
                        IconButton(onClick = onBatchTransactionClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_batch_transaction),
                                contentDescription = stringResource(R.string.nc_batch_transaction),
                                tint = Color.Unspecified,
                            )
                        }
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
                CurrencyInputBlock(
                    inputText = state.inputText,
                    unitLabel = unitLabel,
                    maxDecimalDigits = maxDecimalDigits,
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
                        text = if (isFromSelectedCoin) {
                            stringResource(R.string.nc_send_all_selected)
                        } else {
                            stringResource(R.string.nc_transaction_send_all)
                        },
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

                BalanceBlock(
                    isFromSelectedCoin = isFromSelectedCoin,
                    availableAmount = availableAmount,
                )
            }
        }
    }
}

@Composable
private fun CurrencyInputBlock(
    inputText: String,
    unitLabel: String,
    maxDecimalDigits: Int,
    secondaryCurrency: String,
    onInputChanged: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val textFieldValue = remember(inputText) {
        TextFieldValue(text = inputText, selection = TextRange(inputText.length))
    }
    val textColor = MaterialTheme.colorScheme.textPrimary
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val rawMaxWidthPx = with(density) { maxWidth.toPx() }
            // Compose previews can hand us infinite max width; fall back to a sane phone width
            // so auto-size still has something to shrink against.
            val maxRowWidthPx = if (rawMaxWidthPx.isFinite() && rawMaxWidthPx > 0f) {
                rawMaxWidthPx
            } else {
                with(density) { 320.dp.toPx() }
            }
            val baseStyle = NunchukTheme.typography.heading.copy(
                fontSize = autoSizeFontSize(
                    measurer = measurer,
                    rawInput = inputText,
                    unitLabel = unitLabel,
                    maxWidthPx = maxRowWidthPx,
                ),
                color = textColor,
            )

            BasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = textFieldValue,
                onValueChange = { newValue ->
                    val filtered = filterCurrencyInput(
                        newValue.text,
                        allowDecimal = maxDecimalDigits > 0,
                    )
                    val limited = CurrencyFormatter.format(filtered, maxDecimalDigits)
                    val intDigits = limited.substringBefore(decimalSeparator).length
                    val withinLimits = intDigits <= MAX_INTEGER_DIGITS &&
                        limited.length <= MAX_TOTAL_LENGTH
                    if (withinLimits && limited != inputText) {
                        onInputChanged(limited)
                    }
                },
                singleLine = true,
                textStyle = baseStyle.copy(textAlign = TextAlign.Center),
                cursorBrush = SolidColor(textColor),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (maxDecimalDigits > 0) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                visualTransformation = AmountWithSuffixTransformation(unitLabel),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = secondaryCurrency,
            style = NunchukTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun autoSizeFontSize(
    measurer: TextMeasurer,
    rawInput: String,
    unitLabel: String,
    maxWidthPx: Float,
    baseSp: Float = 44f,
    minSp: Float = 16f,
    stepSp: Float = 2f,
): TextUnit {
    if (maxWidthPx <= 0f) return baseSp.sp
    val displayed = applyThousandsGrouping(rawInput)
    // Reserve room for the 8.dp spacer between input and label, plus the label itself.
    val combined = if (displayed.isEmpty()) unitLabel else "$displayed  $unitLabel"
    val style = NunchukTheme.typography.heading
    // 0.92 leaves a small margin for font metric differences between measurer and renderer.
    val target = maxWidthPx * 0.92f
    var size = baseSp
    while (size > minSp) {
        val measured = measurer.measure(
            text = AnnotatedString(combined),
            style = style.copy(fontSize = size.sp),
        )
        if (measured.size.width <= target) break
        size -= stepSp
    }
    return size.sp
}

private fun applyThousandsGrouping(raw: String): String {
    if (raw.isEmpty()) return raw
    val sep = decimalSeparator
    val dot = raw.indexOf(sep)
    val intPart = if (dot >= 0) raw.substring(0, dot) else raw
    val fracPart = if (dot >= 0) raw.substring(dot) else ""
    val intLong = intPart.toLongOrNull() ?: return raw
    val grouped = String.format(Locale.getDefault(), "%,d", intLong)
    return grouped + fracPart
}

@Composable
private fun BalanceBlock(
    isFromSelectedCoin: Boolean,
    availableAmount: Double,
) {
    val balanceLabel = if (isFromSelectedCoin) {
        stringResource(R.string.nc_total_amount_selected)
    } else {
        stringResource(R.string.nc_transaction_balance)
    }
    val amountColor = if (isFromSelectedCoin) SlimeDark else MaterialTheme.colorScheme.textPrimary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = balanceLabel,
            style = NunchukTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = availableAmount.getBTCAmount(),
            style = NunchukTheme.typography.bodySmall.copy(color = amountColor),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "(${availableAmount.getCurrencyAmount()})",
            style = NunchukTheme.typography.bodySmall.copy(color = amountColor),
        )
    }
}

private fun filterCurrencyInput(
    input: String,
    allowDecimal: Boolean,
): String {
    if (input.isEmpty()) return input
    val sep = decimalSeparator
    val builder = StringBuilder()
    var hasDecimal = false
    for (ch in input) {
        when {
            ch.isDigit() -> builder.append(ch)
            allowDecimal && ch == sep && !hasDecimal -> {
                hasDecimal = true
                builder.append(ch)
            }
        }
    }
    return builder.toString()
}

@PreviewLightDark
@Composable
private fun InputAmountScreenPreview() {
    InputAmountScreen(
        state = InputAmountState(
            amountBTC = 0.0012345,
            amountUSD = 50.0,
            useBtc = true,
            inputText = "0.0012345",
        ),
        isClaimInheritance = false,
        isFromSelectedCoin = false,
        availableAmount = 0.5,
        onClose = {},
        onScanQrClicked = {},
        onBatchTransactionClicked = {},
        onSendAllClicked = {},
        onSwitchCurrencyClicked = {},
        onContinueClicked = {},
        onInputChanged = {},
    )
}
