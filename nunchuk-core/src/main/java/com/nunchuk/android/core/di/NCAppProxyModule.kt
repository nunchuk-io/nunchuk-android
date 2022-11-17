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

package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.CheckUpdateRecommendUseCase
import com.nunchuk.android.core.domain.CheckUpdateRecommendUseCaseImpl
import com.nunchuk.android.core.data.NCAppApi
import com.nunchuk.android.core.repository.NCAppRepository
import com.nunchuk.android.core.repository.NCAppRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NCAppDomainModule {
    @Binds
    fun bindCheckUpdateRecommendUseCase(userCase: CheckUpdateRecommendUseCaseImpl): CheckUpdateRecommendUseCase
}

@Module
@InstallIn(SingletonComponent::class)
internal interface NCAppDataModule {

    @Binds
    @Singleton
    fun bindNCAppRepository(userCase: NCAppRepositoryImpl): NCAppRepository

}

@Module
@InstallIn(SingletonComponent::class)
internal object NCAppNetworkModule {

    @Singleton
    @Provides
    fun provideNCAppApi(retrofit: Retrofit): NCAppApi =
        retrofit.create(NCAppApi::class.java)

}