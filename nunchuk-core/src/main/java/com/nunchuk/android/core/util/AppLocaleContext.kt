package com.nunchuk.android.core.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat

/**
 * For APIs that require an immediate String (SDK callbacks, notifications, persisted names).
 * UI state and events should carry [UiText] instead. Never cache this context: the app language
 * may change while the application or a retained ViewModel stays alive.
 */
fun Context.withAppLocale(): Context {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return this
    val configuration = Configuration(resources.configuration)
    // Match AppCompat's language fallback: requested languages first, then system languages.
    val systemLocales = ConfigurationCompat.getLocales(Resources.getSystem().configuration)
    val tags = (locales.toLanguageTags() + "," + systemLocales.toLanguageTags())
        .split(',').filter { it.isNotEmpty() }.distinct().joinToString(",")
    ConfigurationCompat.setLocales(configuration, LocaleListCompat.forLanguageTags(tags))
    return createConfigurationContext(configuration)
}
