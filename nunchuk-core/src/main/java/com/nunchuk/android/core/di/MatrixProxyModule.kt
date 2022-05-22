package com.nunchuk.android.core.di

import android.content.Context
import com.nunchuk.android.core.matrix.RoomDisplayNameFallbackProviderImpl
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import javax.inject.Singleton

@Module(
    includes = [
        MatrixNetworkModule::class,
        MatrixDataModule::class,
        MatrixDomainModule::class,
        MatrixCommonModule::class
    ]
)
interface MatrixProxyModule

@Module
internal object MatrixCommonModule {

    @Provides
    fun providesMatrixConfiguration() = MatrixConfiguration(roomDisplayNameFallbackProvider = RoomDisplayNameFallbackProviderImpl())

    @Provides
    @Singleton
    fun providesMatrix(context: Context, configuration: MatrixConfiguration) = Matrix.createInstance(context, configuration)

}
