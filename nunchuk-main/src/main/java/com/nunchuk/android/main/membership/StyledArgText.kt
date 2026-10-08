package com.nunchuk.android.main.membership

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Builds a sentence from the format string [formatRes] whose first argument is the text of
 * [argRes], and applies [style] to that argument only.
 *
 * Keeping the whole sentence in one resource (with the styled part as a placeholder) lets
 * translators reorder the styled part instead of concatenating translated fragments.
 */
@Composable
internal fun annotatedStringWithStyledArg(
    @StringRes formatRes: Int,
    @StringRes argRes: Int,
    style: SpanStyle,
): AnnotatedString {
    val arg = stringResource(argRes)
    val text = stringResource(formatRes, arg)
    return buildAnnotatedString {
        val index = text.indexOf(arg)
        if (index < 0) {
            append(text)
        } else {
            append(text.substring(0, index))
            withStyle(style) { append(arg) }
            append(text.substring(index + arg.length))
        }
    }
}
