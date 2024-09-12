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

import com.nunchuk.android.core.domain.CleanUpCryptoAssetsUseCase
import com.nunchuk.android.core.domain.CleanUpCryptoAssetsUseCaseImpl
import com.nunchuk.android.core.domain.CreateOrUpdateSyncFileUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateSyncFileUseCaseImpl
import com.nunchuk.android.core.domain.DeleteSyncFileUseCase
import com.nunchuk.android.core.domain.DeleteSyncFileUseCaseImpl
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCase
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCaseImpl
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCaseImpl
import com.nunchuk.android.core.domain.GetSyncFileUseCase
import com.nunchuk.android.core.domain.GetSyncFileUseCaseImpl
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCase
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCaseImpl
import com.nunchuk.android.core.domain.HideBannerNewChatUseCase
import com.nunchuk.android.core.domain.HideBannerNewChatUseCaseImpl
import com.nunchuk.android.core.domain.LoginWithMatrixUseCase
import com.nunchuk.android.core.domain.LoginWithMatrixUseCaseImpl
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCaseImpl
import com.nunchuk.android.core.domain.UpdateDeveloperSettingUseCase
import com.nunchuk.android.core.domain.UpdateDeveloperSettingUseCaseImpl
import com.nunchuk.android.core.domain.UpdateDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.UpdateDisplayUnitSettingUseCaseImpl
import com.nunchuk.android.share.GetCurrentUserAsContactUseCase
import com.nunchuk.android.share.GetCurrentUserAsContactUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface DomainModule {

    @Binds
    fun bindScheduleGetPriceConvertBTCUseCase(repository: ScheduleGetPriceConvertBTCUseCaseImpl): ScheduleGetPriceConvertBTCUseCase

    @Binds
    fun bindHideBannerNewChatUseCase(useCase: HideBannerNewChatUseCaseImpl): HideBannerNewChatUseCase

    @Binds
    fun bindLoginWithMatrixUseCase(useCase: LoginWithMatrixUseCaseImpl): LoginWithMatrixUseCase

    @Binds
    fun bindGetDisplayUnitSettingUseCase(useCase: GetDisplayUnitSettingUseCaseImpl): GetDisplayUnitSettingUseCase

    @Binds
    fun bindUpdateDisplayUnitSettingUseCase(useCase: UpdateDisplayUnitSettingUseCaseImpl): UpdateDisplayUnitSettingUseCase

    @Binds
    fun bindHealthCheckMasterSignerUseCase(useCase: HealthCheckMasterSignerUseCaseImpl): HealthCheckMasterSignerUseCase

    @Binds
    fun bindGetCurrentAccountAsContact(useCase: GetCurrentUserAsContactUseCaseImpl): GetCurrentUserAsContactUseCase

    @Binds
    fun bindCleanUpCryptoAssetsUseCase(useCase: CleanUpCryptoAssetsUseCaseImpl): CleanUpCryptoAssetsUseCase

    @Binds
    fun bindGetDeveloperSettingUseCase(useCase: GetDeveloperSettingUseCaseImpl): GetDeveloperSettingUseCase

    @Binds
    fun bindUpdateDeveloperSettingUseCase(useCase: UpdateDeveloperSettingUseCaseImpl): UpdateDeveloperSettingUseCase

    @Binds
    fun bindGetSyncFileUseCase(useCase: GetSyncFileUseCaseImpl): GetSyncFileUseCase

    @Binds
    fun bindCreateOrUpdateSyncFileUseCase(useCase: CreateOrUpdateSyncFileUseCaseImpl): CreateOrUpdateSyncFileUseCase

    @Binds
    fun bindDeleteSyncFileUseCase(useCase: DeleteSyncFileUseCaseImpl): DeleteSyncFileUseCase
}