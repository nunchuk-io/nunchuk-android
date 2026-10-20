package com.nunchuk.android.core.locale

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.LocaleList
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import timber.log.Timber
import java.util.Locale

/**
 * The app language choice, stored by AppCompat's per-app language API: the system keeps it on
 * Android 13+ (and shows it in App info → Language), AppCompat's autoStoreLocales service keeps it
 * below that. Either way it lives outside account data, so it survives sign-out and account changes.
 * An empty app locale list means "follow the device language".
 */
object AppLanguageManager {
    private const val PREFS_NAME = "nc_app_language"
    private const val KEY_EXISTING_INSTALL_CHECKED = "existing_install_checked"

    /** The explicitly chosen language, or null while following the device language. */
    fun selectedLanguage(): AppLanguage? =
        AppCompatDelegate.getApplicationLocales()[0]?.let(AppLanguage::from)

    /** The device's preferred locale, ignoring the per-app override. */
    fun deviceLocale(context: Context): Locale = deviceLocales(context)[0] ?: Locale.ENGLISH

    /** The first language in the device's preference order that Nunchuk offers. */
    fun deviceLanguage(context: Context): AppLanguage? {
        val locales = deviceLocales(context)
        return (0 until locales.size()).firstNotNullOfOrNull { index ->
            locales[index]?.let(AppLanguage::from)
        }
    }

    /** The language the UI is in: the explicit choice, else the device language, else English. */
    fun languageInUse(context: Context): AppLanguage =
        selectedLanguage() ?: deviceLanguage(context) ?: AppLanguage.ENGLISH

    /** Applies [language] immediately (activities are recreated); null follows the device. */
    fun setLanguage(language: AppLanguage?) {
        require(language == null || language in AppLanguage.supported) { "Translation is not available" }
        AppCompatDelegate.setApplicationLocales(
            language?.let { LocaleListCompat.forLanguageTags(it.tag) }
                ?: LocaleListCompat.getEmptyLocaleList()
        )
    }

    /**
     * New installs follow the device language; installs that predate the language setting keep
     * English, which was the only language they had. Runs once per installation.
     */
    fun keepEnglishForExistingInstall(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_EXISTING_INSTALL_CHECKED, false)) return
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val isUpdate = info.firstInstallTime != info.lastUpdateTime
            if (isUpdate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Application startup has no Activity delegate yet. Use the framework service
                    // directly, since AppCompat cannot resolve LocaleManager at this point.
                    val manager = context.getSystemService(LocaleManager::class.java)
                    if (manager.applicationLocales.isEmpty) {
                        manager.applicationLocales = LocaleList.forLanguageTags(AppLanguage.ENGLISH.tag)
                    }
                } else if (AppCompatDelegate.getApplicationLocales().isEmpty) {
                    setLanguage(AppLanguage.ENGLISH)
                }
            }
            prefs.edit { putBoolean(KEY_EXISTING_INSTALL_CHECKED, true) }
        }.onFailure { Timber.e(it) }
    }

    private fun deviceLocales(context: Context): LocaleListCompat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleListCompat.wrap(context.getSystemService(LocaleManager::class.java).systemLocales)
        } else {
            ConfigurationCompat.getLocales(Resources.getSystem().configuration)
        }
}
