package com.nunchuk.android.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MaxLengthTransformation(private val maxLength: Int, private val prefix: String = "") :
    VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val prefixOffset = prefix.length

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + prefixOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset < prefixOffset) return 0
                return offset - prefixOffset
            }
        }
        val truncatedText = if (text.text.length > maxLength) {
            text.text.substring(0, maxLength)
        } else {
            text.text
        }
        val out = prefix + truncatedText
        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}

