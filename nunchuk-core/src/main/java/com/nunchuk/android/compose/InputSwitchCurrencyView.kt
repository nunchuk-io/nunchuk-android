package com.nunchuk.android.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.getTextBtcUnit

@Composable
fun InputSwitchCurrencyView(
    title: String,
    isBtc: Boolean,
    currencyValue: String,
    onSwitchBtcAndCurrency: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    NcTextField(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        title = title,
        placeholder = {
            val unit =
                if (isBtc) LocalContext.current.getTextBtcUnit()
                else LOCAL_CURRENCY
            Text(
                text = "0.00 $unit",
                style = NunchukTheme.typography.body.copy(
                    color = colorResource(
                        id = R.color.nc_boulder_color
                    )
                )
            )
        },
        value = currencyValue,
        onValueChange = onValueChange,
        visualTransformation = NumberCommaTransformation(
            if (isBtc) LocalContext.current.getTextBtcUnit() else LOCAL_CURRENCY
        ),
        rightContent = {
            SwitchAmount(isBtc, onSwitchBtcAndCurrency = onSwitchBtcAndCurrency)
        })
}

@Composable
private fun SwitchAmount(isBtc: Boolean, onSwitchBtcAndCurrency: (Boolean) -> Unit) {
    val unit = if (isBtc) LOCAL_CURRENCY else LocalContext.current.getTextBtcUnit()
    Row(
        modifier = Modifier.clickable { onSwitchBtcAndCurrency(isBtc.not()) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_switch),
            contentDescription = "Switch",
            tint = MaterialTheme.colors.primary
        )
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(R.string.nc_transaction_switch_to_currency_data, unit),
            textDecoration = TextDecoration.Underline,
            style = NunchukTheme.typography.titleSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InputSwitchCurrencyViewPreview() {
    NunchukTheme {
        InputSwitchCurrencyView(
            title = "title",
            isBtc = true,
            currencyValue = "2",
            onSwitchBtcAndCurrency = {},
            onValueChange = {})
    }
}
