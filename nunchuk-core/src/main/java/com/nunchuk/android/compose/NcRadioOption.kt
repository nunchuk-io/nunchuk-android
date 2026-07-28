package com.nunchuk.android.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcRadioOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    hasError: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = when {
        hasError -> colorResource(id = R.color.nc_orange_color)
        isSelected -> MaterialTheme.colorScheme.textPrimary
        else -> MaterialTheme.colorScheme.strokePrimary
    }

    Card(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        border = BorderStroke(width = 2.dp, color = borderColor),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
            disabledContainerColor = MaterialTheme.colorScheme.background,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.4f)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                NcRadioButton(
                    modifier = Modifier.padding(),
                    selected = isSelected,
                    enabled = enabled,
                    onClick = onClick
                )
            }
            Column(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .weight(1f),
            ) {
                content()
            }
        }
    }
}

@Composable
fun NcRadioOptionWithInput(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    hasError: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    label: @Composable () -> Unit,
    inputValue: String = "",
    onInputValueChange: (String) -> Unit = {},
    inputKeyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    inputSingleLine: Boolean = true,
    inputMaxLines: Int = 1,
    inputRightContent: @Composable (() -> Unit)? = null,
    inputPlaceholder: @Composable (() -> Unit)? = null,
) {
    NcRadioOption(
        modifier = modifier,
        isSelected = isSelected,
        hasError = hasError,
        enabled = enabled,
        onClick = onClick,
    ) {
        label()
        if (isSelected) {
            Spacer(modifier = Modifier.height(12.dp))
            // Error state is conveyed by the outer card border only; keep the inner
            // field's border neutral so the two don't highlight at once.
            NcTextField(
                title = "",
                value = inputValue,
                placeholder = inputPlaceholder,
                keyboardOptions = inputKeyboardOptions,
                maxLines = inputMaxLines,
                singleLine = inputSingleLine,
                rightContent = inputRightContent,
                onValueChange = onInputValueChange,
            )
        }
    }
}

@Preview
@Composable
fun NcRadioOptionPreview() {
    NunchukTheme {
        NcRadioOption(isSelected = true) {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}

@Preview
@Composable
fun NcRadioOptionDisablePreview() {
    NunchukTheme {
        NcRadioOption(enabled = false) {
            Text(text = "A master account and secondary account(s) control three keys; Nunchuk holds the fourth key on our secure server to assist in inheritance planning and daily wallet operation.")
        }
    }
}