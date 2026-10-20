package com.nunchuk.android.transaction.components.send.amount

import com.nunchuk.android.core.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AmountInputLocaleTest {
    @Test
    fun btcInputKeepsDotDecimalWhenAppLanguageChanges() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1234.56", filterCurrencyInput("1234.56", true))
            assertEquals("1,234.56", applyThousandsGrouping("1234.56"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun fiatInputUsesTheSameLocaleForFilteringPrecisionAndGrouping() {
        val locale = Locale.GERMANY
        val filtered = filterCurrencyInput("1234,567", true, locale)
        assertEquals("1234,567", filtered)
        val limited = CurrencyFormatter.format(filtered, 2, locale)
        assertEquals("1234,56", limited)
        assertEquals("1.234,56", applyThousandsGrouping(limited, locale))
        assertEquals("1234", filterCurrencyInput("12,34", false, locale))
    }
}
