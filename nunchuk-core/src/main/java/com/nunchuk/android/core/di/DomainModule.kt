package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.*
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
    fun bindGetBlockchainExplorerUrlUseCase(useCase: GetBlockchainExplorerUrlUseCaseImpl): GetBlockchainExplorerUrlUseCase

    @Binds
    fun bindAddBlockChainConnectionListenerUseCase(useCase: AddBlockChainConnectionListenerUseCaseImpl): AddBlockChainConnectionListenerUseCase

    @Binds
    fun bindHideBannerNewChatUseCase(useCase: HideBannerNewChatUseCaseImpl): HideBannerNewChatUseCase

    @Binds
    fun bindCheckShowBannerNewChatUseCase(useCase: CheckShowBannerNewChatUseCaseImpl): CheckShowBannerNewChatUseCase

    @Binds
    fun bindLoginWithMatrixUseCase(useCase: LoginWithMatrixUseCaseImpl): LoginWithMatrixUseCase
}