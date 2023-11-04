package com.nunchuk.android.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun NcNumberInputField(
    title: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowDecimal: Boolean = true,
    afterDecimal: Int = Int.MAX_VALUE,
) {
    val decimalCharacter = remember {
        DecimalFormatSymbols(Locale.US).decimalSeparator
    }
    NcTextField(
        modifier = modifier,
        title = title,
        value = value,
        onValueChange = { s ->
            if (s.all { it.isDigit() || (allowDecimal && it == decimalCharacter) }) {
                onValueChange(s)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        maxLines = 1,
        visualTransformation = NumberCommaTransformation(afterDecimal = afterDecimal),
    )
}

@Preview(showBackground = true)
@Composable
fun NcNumberInputFieldPreview() {
    NunchukTheme {
        NcNumberInputField(
            title = "Amount",
            value = "1234567890.9999",
            onValueChange = {},
            afterDecimal = 2
        )
    }
}