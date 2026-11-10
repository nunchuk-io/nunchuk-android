package com.nunchuk.android.core.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun onlyShippedTranslationsResolveToALanguage() {
        assertEquals(listOf(AppLanguage.ENGLISH), AppLanguage.supported)
        assertEquals(AppLanguage.ENGLISH, AppLanguage.from(Locale.UK))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.from(Locale.US))
        assertNull(AppLanguage.from(Locale.GERMANY))
        assertNull(AppLanguage.from(Locale.forLanguageTag("vi-VN")))
        assertNull(AppLanguage.from(Locale.forLanguageTag("zh-Hant-TW")))
    }
}
