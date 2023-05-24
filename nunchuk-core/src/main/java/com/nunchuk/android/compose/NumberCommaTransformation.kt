package com.nunchuk.android.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.nunchuk.android.core.util.formatDecimalWithoutZero

class NumberCommaTransformation(private val suffix: String = "") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatValue = text.text.toDoubleOrNull()?.formatDecimalWithoutZero() ?: ""
        val value = when {
            formatValue == "0" -> "$text $suffix"
            formatValue.isEmpty() -> ""
            text.text.endsWith(".") -> "${formatValue}. $suffix"
            else -> "$formatValue $suffix"
        }
        return TransformedText(
            text = AnnotatedString(value),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return value.length
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return text.length
                }
            }
        )
    }
}