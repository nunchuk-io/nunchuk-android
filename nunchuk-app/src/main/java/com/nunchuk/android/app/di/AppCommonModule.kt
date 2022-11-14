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

package com.nunchuk.android.app.di

import android.app.Application
import android.content.Context
import com.nunchuk.android.app.network.HeaderProviderImpl
import com.nunchuk.android.app.provider.AppInfoProviderImpl
import com.nunchuk.android.app.provider.PushNotificationIntentProviderImpl
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.notifications.PushNotificationIntentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AppCommonModule {
    @Binds
    fun bindApplicationContext(application: Application): Context

    @Binds
    fun bindAppInfoProvider(nav: AppInfoProviderImpl): AppInfoProvider

    @Binds
    fun bindHeaderProvider(provider: HeaderProviderImpl): HeaderProvider

    @Binds
    fun bindPushNotificationIntentProvider(provider: PushNotificationIntentProviderImpl): PushNotificationIntentProvider
}