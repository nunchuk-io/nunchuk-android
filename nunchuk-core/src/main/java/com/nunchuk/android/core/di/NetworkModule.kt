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

import com.nunchuk.android.core.data.api.BannerApi
import com.nunchuk.android.core.data.api.GroupWalletApi
import com.nunchuk.android.core.data.api.PriceConverterAPI
import com.nunchuk.android.core.data.api.TransactionApi
import com.nunchuk.android.core.data.api.UserWalletsApi
import com.nunchuk.android.network.util.TEST_NET_RETROFIT
import com.nunchuk.android.network.util.TEST_NET_USER_WALLET_API
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Singleton
    @Provides
    fun providePriceConverterAPI(retrofit: Retrofit): PriceConverterAPI = retrofit.create(
        PriceConverterAPI::class.java
    )

    @Singleton
    @Provides
    fun provideTransactionAPI(retrofit: Retrofit): TransactionApi = retrofit.create(
        TransactionApi::class.java
    )

    @Singleton
    @Provides
    fun provideUserWalletsApi(retrofit: Retrofit): UserWalletsApi =
        retrofit.create(UserWalletsApi::class.java)

    @Singleton
    @Provides
    fun provideGroupWalletApi(retrofit: Retrofit): GroupWalletApi =
        retrofit.create(GroupWalletApi::class.java)

    @Singleton
    @Provides
    @Named(TEST_NET_USER_WALLET_API)
    fun provideTestNetUserWalletsApi(@Named(TEST_NET_RETROFIT) retrofit: Retrofit): UserWalletsApi =
        retrofit.create(UserWalletsApi::class.java)

    @Singleton
    @Provides
    @Named(TEST_NET_USER_WALLET_API)
    fun provideTestNetGroupWalletApi(@Named(TEST_NET_RETROFIT) retrofit: Retrofit): GroupWalletApi =
        retrofit.create(GroupWalletApi::class.java)

    @Singleton
    @Provides
    fun provideBannerApi(retrofit: Retrofit): BannerApi = retrofit.create(BannerApi::class.java)
}