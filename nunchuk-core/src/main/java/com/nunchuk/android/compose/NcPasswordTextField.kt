/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NcPasswordTextField(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    error: String? = null,
    onClick: () -> Unit = {},
    placeholder: @Composable (() -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions(),
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    onFocusEvent: (FocusState) -> Unit = {},
    onValueChange: (value: String) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
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
                    .clickable(onClick = onClick)
                    .fillMaxWidth(),
                value = value,
                textStyle = NunchukTheme.typography.body,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                keyboardActions = keyboardActions,
                maxLines = maxLines,
                enabled = enabled,
                onValueChange = onValueChange,
                visualTransformation = if (passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                decorationBox = @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.OutlinedTextFieldDecorationBox(
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
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onFocusEvent: (FocusState) -> Unit = {},
    onValueChange: (value: TextFieldValue) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
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
                visualTransformation = visualTransformation,
                decorationBox = @Composable { innerTextField ->
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.OutlinedTextFieldDecorationBox(
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