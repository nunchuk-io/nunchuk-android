package com.nunchuk.android.core.persistence

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NCSharePreferences @Inject constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(APP_SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE)

    var appSettings: String
        get() = prefs.getString(SP_KEY_APP_SETTINGS, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_APP_SETTINGS, value).apply()

    var deviceId: String
        get() = prefs.getString(SP_KEY_DEVICE_ID, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_DEVICE_ID, value).apply()

    var dontShowBannerNewChat: Boolean
        get() = prefs.getBoolean(SP_KEY_DEVICE_DONT_SHOW_BANNER_NEW_CHAT, false)
        set(value) = prefs.edit().putBoolean(SP_KEY_DEVICE_DONT_SHOW_BANNER_NEW_CHAT, value).apply()

    companion object {
        private const val APP_SHARE_PREFERENCE_NAME = "com.nunchuk.android.pref"
        private const val SP_KEY_APP_SETTINGS = "com.nunchuk.android.key.app.settings"
        private const val SP_KEY_DEVICE_ID = "com.nunchuk.android.key.device.id"
        private const val SP_KEY_DEVICE_DONT_SHOW_BANNER_NEW_CHAT = "com.nunchuk.android.key.dont.show.banner.new.chat"
    }
}