package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.CleanUpCryptoAssetsUseCase
import com.nunchuk.android.core.domain.CleanUpCryptoAssetsUseCaseImpl
import com.nunchuk.android.core.domain.CreateOrUpdateSyncFileUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateSyncFileUseCaseImpl
import com.nunchuk.android.core.domain.DeleteSyncFileUseCase
import com.nunchuk.android.core.domain.DeleteSyncFileUseCaseImpl
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetAppSettingUseCaseUseCaseImpl
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCase
import com.nunchuk.android.core.domain.GetDeveloperSettingUseCaseImpl
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCaseImpl
import com.nunchuk.android.core.domain.GetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.GetPriceConvertBTCUseCaseImpl
import com.nunchuk.android.core.domain.GetSyncFileUseCase
import com.nunchuk.android.core.domain.GetSyncFileUseCaseImpl
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCase
import com.nunchuk.android.core.domain.HealthCheckMasterSignerUseCaseImpl
import com.nunchuk.android.core.domain.HideBannerNewChatUseCase
import com.nunchuk.android.core.domain.HideBannerNewChatUseCaseImpl
import com.nunchuk.android.core.domain.InitAppSettingsUseCase
import com.nunchuk.android.core.domain.InitAppSettingsUseCaseImpl
import com.nunchuk.android.core.domain.LoginWithMatrixUseCase
import com.nunchuk.android.core.domain.LoginWithMatrixUseCaseImpl
import com.nunchuk.android.core.domain.SaveCacheFileUseCase
import com.nunchuk.android.core.domain.SaveCacheFileUseCaseImpl
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCaseImpl
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCaseImpl
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
    fun bindGetPriceConvertBTCUseCase(repository: GetPriceConvertBTCUseCaseImpl): GetPriceConvertBTCUseCase

    @Binds
    fun bindScheduleGetPriceConvertBTCUseCase(repository: ScheduleGetPriceConvertBTCUseCaseImpl): ScheduleGetPriceConvertBTCUseCase

    @Binds
    fun bindUpdateAppSettingUseCase(useCase: UpdateAppSettingUseCaseImpl): UpdateAppSettingUseCase

    @Binds
    fun bindGetLocalAppSettingUseCase(useCase: GetAppSettingUseCaseUseCaseImpl): GetAppSettingUseCase

    @Binds
    fun bindInitAppSettingsUseCase(useCase: InitAppSettingsUseCaseImpl): InitAppSettingsUseCase

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

    @Binds
    fun bindSaveCacheFileUseCase(useCase: SaveCacheFileUseCaseImpl): SaveCacheFileUseCase

}