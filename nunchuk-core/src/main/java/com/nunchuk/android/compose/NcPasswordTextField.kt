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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcPasswordTextField(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    error: String? = null,
    hasError: Boolean = !error.isNullOrEmpty(),
    onClick: () -> Unit = {},
    placeholder: @Composable (() -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions(),
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    onFocusEvent: (FocusState) -> Unit = {},
    onValueChange: (value: String) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
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
                        color = if (hasError) colorResource(id = R.color.nc_red_tint_color) else MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onFocusEvent(onFocusEvent)
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                    )
                    .clickable(onClick = onClick)
                    .fillMaxWidth(),
                value = value,
                textStyle = NunchukTheme.typography.body,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                maxLines = maxLines,
                enabled = enabled,
                onValueChange = onValueChange,
                visualTransformation = if (!passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                decorationBox = @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
                        value = value,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = null,
                        leadingIcon = null,
                        trailingIcon = {
                            val image = if (passwordVisible)
                                R.drawable.ic_hide_pass
                            else R.drawable.ic_show_pass

                            // Please provide localized description for accessibility services
                            val description =
                                if (passwordVisible) "Hide password" else "Show password"

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(painter = painterResource(id = image), description)
                            }
                        },
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
                                    color = if (hasError) colorResource(id = R.color.nc_orange_color) else MaterialTheme.colorScheme.border,
                                    shape = RoundedCornerShape(8.dp),
                                )
                            )
                        }
                    )
                },
            )
        }
        if (!error.isNullOrEmpty()) {
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
                    text = error, style = NunchukTheme.typography.bodySmall.copy(
                        color = colorResource(
                            id = R.color.nc_orange_color
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun NcPasswordTextField(
    modifier: Modifier = Modifier,
    title: String,
    value: TextFieldValue,
    error: String? = null,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onFocusEvent: (FocusState) -> Unit = {},
    onValueChange: (value: TextFieldValue) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val hasError = !error.isNullOrEmpty()
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
                        color = if (hasError) colorResource(id = R.color.nc_red_tint_color) else MaterialTheme.colorScheme.background,
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
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                decorationBox = @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
                        value = value.text,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = null,
                        leadingIcon = null,
                        trailingIcon = {
                            val image = if (passwordVisible)
                                R.drawable.ic_hide_pass
                            else R.drawable.ic_show_pass

                            // Please provide localized description for accessibility services
                            val description =
                                if (passwordVisible) "Hide password" else "Show password"

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(painter = painterResource(id = image), description)
                            }
                        },
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
                                    color = if (hasError) colorResource(id = R.color.nc_orange_color) else MaterialTheme.colorScheme.border,
                                    shape = RoundedCornerShape(8.dp),
                                )
                            )
                        }
                    )
                },
            )
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
internal fun NcPasswordTextFieldPreview() {
    NunchukTheme {
        Box(Modifier.background(color = Color.White)) {
            NcPasswordTextField(
                title = "Title here",
                value = "Value here",
            ) {

            }
        }
    }
}

@Preview
@Composable
internal fun NcPasswordTextFieldErrorPreview() {
    NunchukTheme {
        Box(Modifier.background(color = Color.White)) {
            NcPasswordTextField(
                title = "Title here",
                value = "Value here",
                error = "Decryption key is invalid",
            ) {

            }
        }
    }
}