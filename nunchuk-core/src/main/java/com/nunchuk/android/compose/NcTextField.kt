package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NcTextField(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    rightText: String? = null,
    error: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    onFocusEvent: (FocusState) -> Unit = {},
    onValueChange: (value: String) -> Unit,
) {
    val hasError = error != null && error.isNotEmpty()
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                text = title,
                style = NunchukTheme.typography.titleSmall
            )
        }
        Box {
            BasicTextField(
                modifier = Modifier
                    .background(
                        color = if (hasError) colorResource(id = R.color.nc_red_tint_color) else MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusEvent(onFocusEvent)
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                    )
                    .fillMaxWidth(),
                value = value,
                textStyle = NunchukTheme.typography.body,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                maxLines = maxLines,
                enabled = enabled,
                onValueChange = onValueChange,
                decorationBox = @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.OutlinedTextFieldDecorationBox(
                        value = value,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = null,
                        leadingIcon = null,
                        trailingIcon = null,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        border = {
                            Box(
                                Modifier.border(
                                    width = 1.dp,
                                    color = if (hasError) colorResource(id = R.color.nc_orange_color) else Color(
                                        0xFFDEDEDE
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                )
                            )
                        }
                    )
                },
            )
            if (rightText.isNullOrEmpty().not()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    text = rightText.orEmpty(),
                    style = NunchukTheme.typography.body
                )
            }
        }
        if (hasError) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp),
                    painter = painterResource(id = R.drawable.ic_error_outline),
                    contentDescription = "Error icon",
                    tint = colorResource(id = R.color.nc_orange_color)
                )
                Text(
                    text = error.orEmpty(), style = NunchukTheme.typography.bodySmall.copy(
                        color = colorResource(
                            id = R.color.nc_orange_color
                        )
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NcTextField(
    modifier: Modifier = Modifier,
    title: String,
    value: TextFieldValue,
    rightText: String? = null,
    error: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    onFocusEvent: (FocusState) -> Unit = {},
    onValueChange: (value: TextFieldValue) -> Unit,
) {
    val hasError = error != null && error.isNotEmpty()
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                text = title,
                style = NunchukTheme.typography.titleSmall
            )
        }
        Box {
            BasicTextField(
                modifier = Modifier
                    .background(
                        color = if (hasError) colorResource(id = R.color.nc_red_tint_color) else MaterialTheme.colors.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusEvent(onFocusEvent)
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                    )
                    .fillMaxWidth(),
                value = value,
                textStyle = NunchukTheme.typography.body,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                maxLines = maxLines,
                enabled = enabled,
                onValueChange = onValueChange,
                decorationBox = @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.OutlinedTextFieldDecorationBox(
                        value = value.text,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = null,
                        leadingIcon = null,
                        trailingIcon = null,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        border = {
                            Box(
                                Modifier.border(
                                    width = 1.dp,
                                    color = if (hasError) colorResource(id = R.color.nc_orange_color) else Color(
                                        0xFFDEDEDE
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                )
                            )
                        }
                    )
                },

            )
            if (rightText.isNullOrEmpty().not()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    text = rightText.orEmpty(),
                    style = NunchukTheme.typography.body
                )
            }
        }
        if (hasError) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(2.dp),
                    painter = painterResource(id = R.drawable.ic_error_outline),
                    contentDescription = "Error icon",
                    tint = colorResource(id = R.color.nc_orange_color)
                )
                Text(
                    text = error.orEmpty(), style = NunchukTheme.typography.bodySmall.copy(
                        color = colorResource(
                            id = R.color.nc_orange_color
                        )
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun NcTextFieldPreview() {
    NunchukTheme {
        Box(Modifier.background(color = Color.White)) {
            NcTextField(
                title = "Title here",
                value = "Value here",
            ) {

            }
        }
    }
}

@Preview
@Composable
fun NcTextFieldErrorPreview() {
    NunchukTheme {
        Box(Modifier.background(color = Color.White)) {
            NcTextField(
                title = "Title here",
                value = "Value here",
                error = "Decryption key is invalid",
                rightText = "Hours"
            ) {

            }
        }
    }
}