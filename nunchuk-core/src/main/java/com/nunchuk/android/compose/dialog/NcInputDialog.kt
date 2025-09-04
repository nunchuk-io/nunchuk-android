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

package com.nunchuk.android.compose.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nunchuk.android.compose.NcPasswordTextField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.R
import timber.log.Timber

enum class NcInputType {
    TEXT, NUMBER, PASSWORD, MULTILINE
}

@Composable
fun NcInputDialog(
    title: String,
    confirmText: String = stringResource(id = R.string.nc_text_confirm),
    inputBoxTitle: String = "",
    onConfirmed: (String) -> Unit = {},
    onCanceled: () -> Unit = {},
    onDismiss: () -> Unit = {},
    isMaskedInput: Boolean = true,
    errorMessage: String? = null,
    descMessage: String? = null,
    inputType: NcInputType = NcInputType.TEXT,
    clickablePhrases: List<Pair<String, () -> Unit>> = emptyList(),
    initialValue: String = "",
    placeholder: String = "",
    maxLines: Int = if (inputType == NcInputType.MULTILINE) Int.MAX_VALUE else 1,
    singleLine: Boolean = inputType != NcInputType.MULTILINE
) {
    var inputValue by rememberSaveable { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Debug: Log that the dialog is being composed
    Timber.tag("NcInputDialog").d("NcInputDialog composing with title: '$title', inputType: $inputType, isMaskedInput: $isMaskedInput")

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(24.dp)
                )
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            // Title
            Text(
                modifier = Modifier.align(CenterHorizontally),
                text = title,
                style = NunchukTheme.typography.title
            )

            // Description message with clickable phrases
            if (!descMessage.isNullOrEmpty()) {
                if (clickablePhrases.isNotEmpty()) {
                    ClickableDescriptionText(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                        text = descMessage,
                        clickablePhrases = clickablePhrases
                    )
                } else {
                    Text(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                        text = descMessage,
                        style = NunchukTheme.typography.body,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Input box title
            if (inputBoxTitle.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    text = inputBoxTitle,
                    style = NunchukTheme.typography.titleSmall
                )
            }

            // Input field
            when {
                inputType == NcInputType.PASSWORD || (isMaskedInput && inputType == NcInputType.TEXT) -> {
                    NcPasswordTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .padding(top = if (inputBoxTitle.isEmpty()) 16.dp else 0.dp),
                        title = "",
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        error = errorMessage,
                        placeholder = if (placeholder.isNotEmpty()) {
                            { Text(text = placeholder) }
                        } else null,
                        keyboardOptions = getKeyboardOptions(inputType),
                        keyboardActions = KeyboardActions(
                            onDone = { 
                                keyboardController?.hide()
                                onConfirmed(inputValue)
                            }
                        ),
                        maxLines = maxLines,
                        singleLine = singleLine
                    )
                }
                else -> {
                    NcTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .padding(top = if (inputBoxTitle.isEmpty()) 16.dp else 0.dp),
                        title = "",
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        error = errorMessage,
                        placeholder = if (placeholder.isNotEmpty()) {
                            { Text(text = placeholder) }
                        } else null,
                        keyboardOptions = getKeyboardOptions(inputType),
                        keyboardActions = KeyboardActions(
                            onDone = { 
                                keyboardController?.hide()
                                onConfirmed(inputValue)
                            }
                        ),
                        maxLines = maxLines,
                        singleLine = singleLine
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCanceled
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_text_cancel),
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                }
                NcPrimaryDarkButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onConfirmed(inputValue) }
                ) {
                    Text(text = confirmText)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun ClickableDescriptionText(
    modifier: Modifier = Modifier,
    text: String,
    clickablePhrases: List<Pair<String, () -> Unit>>,
    textStyle: TextStyle = NunchukTheme.typography.body
) {
    val annotatedString = remember(text, clickablePhrases) {
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.Unspecified)) {
                append(text)
            }
            
            clickablePhrases.forEach { (phrase, _) ->
                var startIndex = text.indexOf(phrase)
                while (startIndex >= 0) {
                    val endIndex = startIndex + phrase.length
                    addStyle(
                        style = SpanStyle(
                            color = Color.Unspecified,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = startIndex,
                        end = endIndex
                    )
                    addStringAnnotation(
                        tag = "clickable",
                        annotation = phrase,
                        start = startIndex,
                        end = endIndex
                    )
                    startIndex = text.indexOf(phrase, endIndex)
                }
            }
        }
    }

    ClickableText(
        modifier = modifier,
        text = annotatedString,
        style = textStyle.copy(textAlign = TextAlign.Center),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "clickable",
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                clickablePhrases.find { it.first == annotation.item }?.second?.invoke()
            }
        }
    )
}

private fun getKeyboardOptions(inputType: NcInputType): KeyboardOptions {
    return when (inputType) {
        NcInputType.TEXT -> KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        )
        NcInputType.NUMBER -> KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        )
        NcInputType.PASSWORD -> KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        )
        NcInputType.MULTILINE -> KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NcInputDialogPreview() {
    NunchukTheme {
        NcInputDialog(
            title = "Enter your passphrase",
            descMessage = "Please enter your passphrase to continue. Make sure to check the documentation for more details.",
            inputType = NcInputType.PASSWORD,
            clickablePhrases = listOf(
                "documentation" to { /* navigate to docs */ }
            ),
            onConfirmed = {},
            onCanceled = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NcInputDialogTextPreview() {
    NunchukTheme {
        NcInputDialog(
            title = "Enter wallet name",
            inputBoxTitle = "Wallet Name",
            placeholder = "My Wallet",
            inputType = NcInputType.TEXT,
            isMaskedInput = false,
            onConfirmed = {},
            onCanceled = {}
        )
    }
}
