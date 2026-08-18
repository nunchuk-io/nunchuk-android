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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A [TextFieldValue] kept in sync with a hoisted [String], together with the callback that feeds
 * edits back to the text field.
 */
@Stable
class TextFieldValueHolder(
    val value: TextFieldValue,
    val onValueChange: (TextFieldValue) -> Unit,
)

/**
 * Bridges the hoisted [String] APIs of our text fields to the [TextFieldValue] based
 * `BasicTextField`.
 *
 * The String based `BasicTextField` reuses the previous IME composition whenever the hoisted text
 * is replaced from the outside - which is what every caller that filters input, truncates to a max
 * length or lets a ViewModel rewrite the text does. The kept composition can then point past the
 * end of the shorter text, and the next time the IME asks for cursor anchor info Compose fills
 * character bounding boxes for that range and throws
 * `IllegalArgumentException: offset(N) is out of bounds`.
 *
 * Rebuilding the value without a composition (and with the selection clamped to the new text)
 * whenever the text changes outside of the field keeps the IME state and the laid out text in step.
 */
@Composable
fun rememberTextFieldValueHolder(
    text: String,
    onTextChange: (String) -> Unit,
): TextFieldValueHolder {
    val state = remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val current = state.value
    val value = if (current.text == text) {
        current
    } else {
        TextFieldValue(
            text = text,
            selection = TextRange(current.selection.end.coerceIn(0, text.length)),
        )
    }
    SideEffect { state.value = value }
    return TextFieldValueHolder(value) { newValue ->
        state.value = newValue
        if (newValue.text != value.text) onTextChange(newValue.text)
    }
}
