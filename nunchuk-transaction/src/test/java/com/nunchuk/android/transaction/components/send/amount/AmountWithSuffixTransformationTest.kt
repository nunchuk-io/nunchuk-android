package com.nunchuk.android.transaction.components.send.amount

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountWithSuffixTransformationTest {
    private val transformation = AmountWithSuffixTransformation("BTC")

    @Test
    fun selectAllCoversTheWholeNumber() {
        val transformed = transformation.filter(AnnotatedString("12345.67"))
        assertEquals("12,345.67 BTC", transformed.text.text)
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(9, transformed.offsetMapping.originalToTransformed(8))
    }

    @Test
    fun offsetsSkipGroupingSeparators() {
        val mapping = transformation.filter(AnnotatedString("12345")).offsetMapping
        assertEquals(1, mapping.originalToTransformed(1))
        assertEquals(3, mapping.originalToTransformed(2))
        // Both sides of the comma map back to the same raw offset.
        assertEquals(2, mapping.transformedToOriginal(2))
        assertEquals(2, mapping.transformedToOriginal(3))
    }

    @Test
    fun suffixOffsetsMapToEndOfNumber() {
        val mapping = transformation.filter(AnnotatedString("0.0001")).offsetMapping
        assertEquals(6, mapping.transformedToOriginal(10))
    }

    @Test
    fun leadingZerosDroppedFromDisplayStillMapBothWays() {
        val transformed = transformation.filter(AnnotatedString("00123"))
        assertEquals("123 BTC", transformed.text.text)
        val mapping = transformed.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(0, mapping.originalToTransformed(2))
        assertEquals(1, mapping.originalToTransformed(3))
        assertEquals(3, mapping.originalToTransformed(5))
        // Select all over the display covers every raw char, zeros included.
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(5, mapping.transformedToOriginal(3))
        assertEquals(4, mapping.transformedToOriginal(2))
    }

    @Test
    fun allZerosCollapseToSingleDisplayedZero() {
        val mapping = transformation.filter(AnnotatedString("000.5")).offsetMapping
        // Display is "0.5 BTC": raw "0" at index 2 is the one kept.
        assertEquals(0, mapping.originalToTransformed(2))
        assertEquals(1, mapping.originalToTransformed(3))
        assertEquals(3, mapping.originalToTransformed(5))
        assertEquals(5, mapping.transformedToOriginal(3))
        assertEquals(3, mapping.transformedToOriginal(1))
    }

    @Test
    fun emptyInputMapsToZero() {
        val mapping = transformation.filter(AnnotatedString("")).offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(0, mapping.transformedToOriginal(3))
    }
}
