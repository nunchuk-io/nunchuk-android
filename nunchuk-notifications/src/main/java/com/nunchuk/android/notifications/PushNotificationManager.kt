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

package com.nunchuk.android.notifications

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.core.provider.LocaleProvider
import com.nunchuk.android.core.provider.StringProvider
import org.matrix.android.sdk.api.session.pushers.HttpPusher
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

interface PushNotificationManager {

    suspend fun testPush(pushKey: String)

    fun enqueueRegisterPusherWithFcmKey(pushKey: String): UUID?
}

internal class PushNotificationManagerImpl @Inject constructor(
    private val localeProvider: LocaleProvider,
    private val stringProvider: StringProvider,
    private val appInfoProvider: AppInfoProvider,
    private val sessionHolder: SessionHolder
) : PushNotificationManager {

    override suspend fun testPush(pushKey: String) {
        getPushersService()?.testPush(
            stringProvider.getString(R.string.push_http_url),
            stringProvider.getString(R.string.push_app_id),
            pushKey,
            TEST_EVENT_ID
        )
    }

    override fun enqueueRegisterPusherWithFcmKey(pushKey: String): UUID? {
        return getPushersService()?.let {
            Timber.d("enqueueRegisterPusherWithFcmKey")
            it.enqueueAddHttpPusher(createHttpPusher(pushKey))
        }
    }

    private fun createHttpPusher(pushKey: String) = HttpPusher(
        pushKey,
        stringProvider.getString(R.string.push_app_id),
        profileTag = "mobile" + "_" + abs(sessionHolder.getSafeActiveSession()?.myUserId.orEmpty().hashCode()),
        localeProvider.current().language,
        appInfoProvider.getAppName(),
        sessionHolder.getSafeActiveSession()?.sessionParams?.deviceId ?: "MOBILE",
        stringProvider.getString(R.string.push_http_url),
        append = false,
        withEventIdOnly = true,
        enabled = true,
        deviceId = sessionHolder.getSafeActiveSession()?.sessionParams?.deviceId.orEmpty(),
    )

    suspend fun unregisterPusher(pushKey: String) {
        getPushersService()?.removeHttpPusher(pushKey, stringProvider.getString(R.string.push_app_id))
    }

    private fun getPushersService() = sessionHolder.getSafeActiveSession()?.pushersService()

    companion object {
        private const val TEST_EVENT_ID = "TEST_EVENT_ID"
    }
}
