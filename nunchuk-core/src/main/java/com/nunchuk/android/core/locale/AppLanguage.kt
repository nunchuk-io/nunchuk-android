package com.nunchuk.android.core.locale

import androidx.annotation.StringRes
import com.nunchuk.android.core.R
import java.util.Locale

/**
 * Language catalog for Settings → Display settings → Language, in the order the picker shows them.
 * [nativeName] is never translated; [nameRes] is the language's name in the current app language.
 * Only [supported] entries are offered until their translations ship.
 */
enum class AppLanguage(
    val tag: String,
    val nativeName: String,
    @StringRes val nameRes: Int,
) {
    ENGLISH("en", "English", R.string.nc_language_name_english),
    ARABIC("ar", "العربية", R.string.nc_language_name_arabic),
    CHINESE_SIMPLIFIED("zh-CN", "简体中文", R.string.nc_language_name_chinese_simplified),
    CHINESE_TRADITIONAL("zh-TW", "繁體中文", R.string.nc_language_name_chinese_traditional),
    FRENCH("fr", "Français", R.string.nc_language_name_french),
    GERMAN("de", "Deutsch", R.string.nc_language_name_german),
    HINDI("hi", "हिन्दी", R.string.nc_language_name_hindi),
    INDONESIAN("id", "Bahasa Indonesia", R.string.nc_language_name_indonesian),
    JAPANESE("ja", "日本語", R.string.nc_language_name_japanese),
    KOREAN("ko", "한국어", R.string.nc_language_name_korean),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)", R.string.nc_language_name_portuguese_brazil),
    RUSSIAN("ru", "Русский", R.string.nc_language_name_russian),
    SPANISH("es", "Español", R.string.nc_language_name_spanish),
    VIETNAMESE("vi", "Tiếng Việt", R.string.nc_language_name_vietnamese);

    companion object {
        /** Shipped translations; keep in sync with locales_config.xml and resourceConfigurations. */
        val supported: List<AppLanguage> = listOf(ENGLISH)

        /** The catalog entry a locale resolves to, or null when Nunchuk doesn't offer that language. */
        fun from(locale: Locale): AppLanguage? = when (locale.language) {
            "en" -> ENGLISH
            "ar" -> ARABIC
            "zh" -> if (locale.isTraditionalChinese()) CHINESE_TRADITIONAL else CHINESE_SIMPLIFIED
            "fr" -> FRENCH
            "de" -> GERMAN
            "hi" -> HINDI
            // Android still reports Indonesian under its legacy code "in".
            "id", "in" -> INDONESIAN
            "ja" -> JAPANESE
            "ko" -> KOREAN
            "pt" -> PORTUGUESE_BRAZIL.takeIf { locale.country.isEmpty() || locale.country == "BR" }
            "ru" -> RUSSIAN
            "es" -> SPANISH
            "vi" -> VIETNAMESE
            else -> null
        }?.takeIf { it in supported }

        private fun Locale.isTraditionalChinese(): Boolean = when {
            script.isNotEmpty() -> script == "Hant"
            else -> country in setOf("TW", "HK", "MO")
        }
    }
}
