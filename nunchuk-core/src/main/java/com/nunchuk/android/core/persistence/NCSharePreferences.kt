package com.nunchuk.android.core.persistence

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NCSharePreferences @Inject constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(APP_SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE)

    var displayUnitSetting: String
        get() = prefs.getString(SP_KEY_DISPLAY_UNIT_SETTINGS, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_DISPLAY_UNIT_SETTINGS, value).apply()

    var appSettings: String
        get() = prefs.getString(SP_KEY_APP_SETTINGS, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_APP_SETTINGS, value).apply()

    var deviceId: String
        get() = prefs.getString(SP_KEY_DEVICE_ID, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_DEVICE_ID, value).apply()

    var showBannerNewChat: Boolean
        get() = prefs.getBoolean(SP_KEY_DEVICE_SHOW_BANNER_NEW_CHAT, true)
        set(value) = prefs.edit().putBoolean(SP_KEY_DEVICE_SHOW_BANNER_NEW_CHAT, value).apply()

    var fcmToken: String?
        get() = prefs.getString(SP_KEY_FCM_TOKEN, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_DEVICE_SHOW_BANNER_NEW_CHAT, value).apply()

    var newDevice: Boolean
        get() = prefs.getBoolean(SP_KEY_NEW_DEVICE, true)
        set(value) = prefs.edit().putBoolean(SP_KEY_NEW_DEVICE, value).apply()

    var developerSetting: String
        get() = prefs.getString(SP_KEY_DEVELOPER_SETTING, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_DEVELOPER_SETTING, value).apply()

    var syncSetting: String
        get() = prefs.getString(SP_SYNC_SETTING, "").orEmpty()
        set(value) = prefs.edit().putString(SP_SYNC_SETTING, value).apply()

    companion object {
        private const val PACKAGE_PREFIX = "com.nunchuk.android"
        private const val APP_SHARE_PREFERENCE_NAME = "${PACKAGE_PREFIX}.pref"
        private const val SP_KEY_DISPLAY_UNIT_SETTINGS = "${PACKAGE_PREFIX}.key.display.unit.settings"
        private const val SP_KEY_APP_SETTINGS = "${PACKAGE_PREFIX}.key.app.settings"
        private const val SP_KEY_DEVICE_ID = "${PACKAGE_PREFIX}.key.device.id"
        private const val SP_KEY_DEVICE_SHOW_BANNER_NEW_CHAT = "${PACKAGE_PREFIX}.key.show.banner.new.chat"
        private const val SP_KEY_FCM_TOKEN = "${PACKAGE_PREFIX}.key.fcm.token"
        private const val SP_KEY_NEW_DEVICE = "${PACKAGE_PREFIX}.key.new.device"
        private const val SP_KEY_DEVELOPER_SETTING = "${PACKAGE_PREFIX}.key.developer.setting"
        private const val SP_SYNC_SETTING = "${PACKAGE_PREFIX}.key.sync.setting"
    }
}