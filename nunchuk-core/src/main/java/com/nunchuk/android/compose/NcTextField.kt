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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcTextField(
    modifier: Modifier = Modifier,
    title: String,
    titleHint: String = "",
    titleStyle: TextStyle = NunchukTheme.typography.titleSmall,
    value: String,
    rightContent: @Composable (() -> Unit)? = null,
    error: String? = null,
    hint: String? = null,
    hasError: Boolean = !error.isNullOrEmpty(),
    showErrorMessageOnly: Boolean = false,
    onClick: () -> Unit = {},
    placeholder: @Composable (() -> Unit)? = null,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions = KeyboardActions(),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    disableBackgroundColor: Color = MaterialTheme.colorScheme.background,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    textFieldColor: Color = MaterialTheme.colorScheme.background,
    maxLength: Int = Int.MAX_VALUE,
    enableMaxLength: Boolean = false,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    inputBoxHeight: Dp = Dp.Unspecified,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onFocusEvent: (Boolean) -> Unit = {},
    borderColor : Color = Color(0xFFDEDEDE),
    textStyle: TextStyle = NunchukTheme.typography.body,
    onValueChange: (value: String) -> Unit,
) {
    var backgroundErrorColor = textFieldColor
    var borderErrorColor = borderColor
    if (hasError && showErrorMessageOnly.not()) {
        backgroundErrorColor = colorResource(id = R.color.nc_red_tint_color)
        borderErrorColor = colorResource(id = R.color.nc_orange_color)
    }
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusEvent(isFocused)
    }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            if (title.isNotEmpty()) {
                if (titleHint.isNotEmpty()) {
                    Row {
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp),
                            text = title,
                            style = titleStyle
                        )
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                            text = titleHint,
                            style = NunchukTheme.typography.bodySmall.copy(color = colorResource(id = R.color.nc_grey_dark_color))
                        )
                    }
                } else {
                    Text(
                        modifier = Modifier.padding(bottom = 4.dp),
                        text = title,
                        style = titleStyle
                    )
                }
            }
            if (enableMaxLength) {
                Text(
                    modifier = Modifier.padding(bottom = 4.dp),
                    text = "${value.length}/$maxLength",
                    style = NunchukTheme.typography.bodySmall
                )
            }
        }
        BasicTextField(
            modifier = Modifier
                .background(
                    color = if (enabled.not()) disableBackgroundColor else backgroundErrorColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                )
                .clickable(onClick = onClick)
                .height(inputBoxHeight)
                .fillMaxWidth(),
            value = value,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            maxLines = maxLines,
            enabled = enabled,
            readOnly = readOnly,
            minLines = minLines,
            onValueChange = onValueChange,
            interactionSource = interactionSource,
            visualTransformation = visualTransformation,
            decorationBox = @Composable { innerTextField ->
                // places leading icon, text field with label and placeholder, trailing icon
                TextFieldDefaults.DecorationBox(
                    value = value,
                    visualTransformation = VisualTransformation.None,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = null,
                    leadingIcon = null,
                    trailingIcon = rightContent,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                    container = {
                        Box(
                            Modifier.border(
                                width = 1.dp,
                                color = borderErrorColor,
                                shape = RoundedCornerShape(8.dp),
                            )
                        )
                    }
                )
            },
        )
        if (!error.isNullOrEmpty() || !hint.isNullOrEmpty()) {
            val color = if (hasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.greyDark
            CompositionLocalProvider(LocalContentColor provides color) {
                BottomText(error ?: hint)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcTextField(
    modifier: Modifier = Modifier,
    title: String,
    value: TextFieldValue,
    rightContent: @Composable BoxScope.() -> Unit = {},
    error: String? = null,
    minLines: Int = 1,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    inputBoxHeight: Dp = Dp.Unspecified,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onFocusEvent: (Boolean) -> Unit = {},
    onValueChange: (value: TextFieldValue) -> Unit,
) {
    val hasError = !error.isNullOrEmpty()
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusEvent(isFocused)
    }
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
                        color = if (hasError) colorResource(id = R.color.nc_red_tint_color) else MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                    )
                    .height(inputBoxHeight)
                    .fillMaxWidth(),
                value = value,
                textStyle = NunchukTheme.typography.body,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                maxLines = maxLines,
                enabled = enabled,
                readOnly = readOnly,
                minLines = minLines,
                onValueChange = onValueChange,
                visualTransformation = visualTransformation,
                decorationBox = @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
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
                        container = {
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
            rightContent()
        }
        if (hasError) {
            BottomText(error)
        }
    }
}

@Composable
private fun BottomText(error: String?) {
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
        )
        Text(
            text = error.orEmpty(), style = NunchukTheme.typography.bodySmall.copy(
                color = LocalContentColor.current
            )
        )
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
                rightContent = {
                    Icon(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = ""
                    )
                }
            ) {

            }
        }
    }
}

@Preview
@Composable
fun NcTextFieldMaxLengthPreview() {
    NunchukTheme {
        Box(Modifier.background(color = Color.White)) {
            NcTextField(
                title = "Title here",
                value = "Value here",
                maxLength = 100,
                enableMaxLength = true
            ) {

            }
        }
    }
}