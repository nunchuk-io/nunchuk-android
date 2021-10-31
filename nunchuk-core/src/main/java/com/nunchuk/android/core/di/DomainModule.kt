package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.GetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.GetPriceConvertBTCUseCaseImpl
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCaseImpl
import dagger.Binds
import dagger.Module

@Module
internal interface DomainModule {
    @Binds
    fun bindGetPriceConvertBTCUseCase(repository: GetPriceConvertBTCUseCaseImpl): GetPriceConvertBTCUseCase

    @Binds
    fun bindScheduleGetPriceConvertBTCUseCase(repository: ScheduleGetPriceConvertBTCUseCaseImpl): ScheduleGetPriceConvertBTCUseCase
}