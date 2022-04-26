package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.*
import com.nunchuk.android.share.GetCurrentUserAsContactUseCase
import com.nunchuk.android.share.GetCurrentUserAsContactUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
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
}