package com.nunchuk.android.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AmountLocaleTest {
    @Test
    fun roundingPreservesAmountsRegardlessOfAppLanguage() {
        val original = Locale.getDefault()
        try {
            for (locale in listOf(Locale.US, Locale.GERMANY, Locale.FRANCE, Locale.forLanguageTag("ar"))) {
                Locale.setDefault(locale)
                assertEquals("12.5", 12.5.formatRoundDecimal())
                assertEquals(12.5, 12.5.roundDecimal(), 0.0)
                assertEquals(1234.57, 1234.567.roundDecimal(), 0.0)
                assertEquals(-12.5, (-12.5).roundDecimal(), 0.0)
                assertEquals(0.0, 0.0.roundDecimal(), 0.0)
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
