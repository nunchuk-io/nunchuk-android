/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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

    var developerSetting: String
        get() = prefs.getString(SP_KEY_DEVELOPER_SETTING, "").orEmpty()
        set(value) = prefs.edit().putString(SP_KEY_DEVELOPER_SETTING, value).apply()

    companion object {
        private const val PACKAGE_PREFIX = "com.nunchuk.android"
        private const val APP_SHARE_PREFERENCE_NAME = "${PACKAGE_PREFIX}.pref"
        private const val SP_KEY_DISPLAY_UNIT_SETTINGS = "${PACKAGE_PREFIX}.key.display.unit.settings"
        private const val SP_KEY_APP_SETTINGS = "${PACKAGE_PREFIX}.key.app.settings"
        private const val SP_KEY_DEVICE_ID = "${PACKAGE_PREFIX}.key.device.id"
        private const val SP_KEY_DEVICE_SHOW_BANNER_NEW_CHAT = "${PACKAGE_PREFIX}.key.show.banner.new.chat.v2"
        private const val SP_KEY_FCM_TOKEN = "${PACKAGE_PREFIX}.key.fcm.token"
        private const val SP_KEY_DEVELOPER_SETTING = "${PACKAGE_PREFIX}.key.developer.setting"
    }
}