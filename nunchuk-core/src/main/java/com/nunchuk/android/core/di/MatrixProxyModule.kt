package com.nunchuk.android.core.di

import android.content.Context
import com.nunchuk.android.core.matrix.RoomDisplayNameFallbackProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MatrixCommonModule {

    @Provides
    fun providesMatrixConfiguration() = MatrixConfiguration(roomDisplayNameFallbackProvider = RoomDisplayNameFallbackProviderImpl())

    @Provides
    @Singleton
    fun providesMatrix(context: Context, configuration: MatrixConfiguration) = Matrix(context, configuration)

}
