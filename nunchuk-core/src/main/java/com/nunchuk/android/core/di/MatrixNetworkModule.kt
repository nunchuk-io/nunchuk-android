package com.nunchuk.android.core.di

import com.nunchuk.android.core.api.MatrixAPI
import com.nunchuk.android.network.util.MATRIX_RETROFIT
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object MatrixNetworkModule {

    @Singleton
    @Provides
    fun provideMatrixApi(@Named(MATRIX_RETROFIT) retrofit: Retrofit): MatrixAPI = retrofit.create(MatrixAPI::class.java)

}