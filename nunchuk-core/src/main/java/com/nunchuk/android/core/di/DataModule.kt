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

import com.nunchuk.android.core.repository.BannerRepositoryImpl
import com.nunchuk.android.core.repository.BtcRepository
import com.nunchuk.android.core.repository.BtcRepositoryImpl
import com.nunchuk.android.core.repository.CampaignsRepositoryImpl
import com.nunchuk.android.core.repository.DummyTransactionRepositoryImpl
import com.nunchuk.android.core.repository.GroupWalletRepositoryImpl
import com.nunchuk.android.core.repository.HandledEventRepositoryImpl
import com.nunchuk.android.core.repository.InheritanceRepositoryImpl
import com.nunchuk.android.core.repository.KeyRepositoryImpl
import com.nunchuk.android.core.repository.NotificationRepositoryImpl
import com.nunchuk.android.core.repository.PremiumWalletRepositoryImpl
import com.nunchuk.android.core.repository.RecurringPaymentRepositoryImpl
import com.nunchuk.android.core.repository.SettingRepositoryImpl
import com.nunchuk.android.core.repository.TransactionRepositoryImpl
import com.nunchuk.android.repository.BannerRepository
import com.nunchuk.android.repository.CampaignsRepository
import com.nunchuk.android.repository.DummyTransactionRepository
import com.nunchuk.android.repository.GroupWalletRepository
import com.nunchuk.android.repository.HandledEventRepository
import com.nunchuk.android.repository.InheritanceRepository
import com.nunchuk.android.repository.KeyRepository
import com.nunchuk.android.repository.NotificationRepository
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.repository.RecurringPaymentRepository
import com.nunchuk.android.repository.SettingRepository
import com.nunchuk.android.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    @Singleton
    fun bindPriceConverterAPIRepository(repository: BtcRepositoryImpl): BtcRepository

    @Binds
    @Singleton
    fun bindTransactionRepository(repository: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    fun bindSettingRepository(repository: SettingRepositoryImpl): SettingRepository

    @Binds
    @Singleton
    fun bindUserWalletsRepository(repository: PremiumWalletRepositoryImpl): PremiumWalletRepository

    @Binds
    @Singleton
    fun bindGroupWalletRepository(repository: GroupWalletRepositoryImpl): GroupWalletRepository

    @Binds
    @Singleton
    fun bindBannerRepository(repository: BannerRepositoryImpl): BannerRepository

    @Binds
    @Singleton
    fun bindNotificationRepository(repository: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    fun provideKeyRepository(implementation: KeyRepositoryImpl) : KeyRepository

    @Binds
    @Singleton
    fun bindHandledEventRepository(repository: HandledEventRepositoryImpl): HandledEventRepository

    @Binds
    @Singleton
    fun bindDummyTransactionRepository(repository: DummyTransactionRepositoryImpl): DummyTransactionRepository

    @Binds
    @Singleton
    fun bindRecurringPaymentRepository(repository: RecurringPaymentRepositoryImpl): RecurringPaymentRepository

    @Binds
    @Singleton
    fun bindCampaignsRepository(repository: CampaignsRepositoryImpl): CampaignsRepository

    @Binds
    @Singleton
    fun bindInheritanceRepository(repository: InheritanceRepositoryImpl): InheritanceRepository
}