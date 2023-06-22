/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.core.di

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.AccountManagerImpl
import com.nunchuk.android.core.device.DeviceManagerImpl
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.core.loader.ImageLoaderImpl
import com.nunchuk.android.core.matrix.MatrixInterceptor
import com.nunchuk.android.core.matrix.MatrixInterceptorImpl
import com.nunchuk.android.core.network.NetworkVerifier
import com.nunchuk.android.core.network.NetworkVerifierImpl
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.push.PushEventManagerImpl
import com.nunchuk.android.utils.DeviceManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CoreModule {

    @Binds
    @Singleton
    fun bindAccountManager(manager: AccountManagerImpl): AccountManager

    @Binds
    @Singleton
    fun bindDeviceManager(manager: DeviceManagerImpl): DeviceManager

    @Binds
    @Singleton
    fun bindMatrixInterceptor(interceptor: MatrixInterceptorImpl): MatrixInterceptor

    @Binds
    @Singleton
    fun bindImageLoader(interceptor: ImageLoaderImpl): ImageLoader

    @Binds
    @Singleton
    fun bindNetworkVerifier(network: NetworkVerifierImpl): NetworkVerifier

    @Binds
    @Singleton
    fun bindPushEventManager(impl: PushEventManagerImpl): PushEventManager
}