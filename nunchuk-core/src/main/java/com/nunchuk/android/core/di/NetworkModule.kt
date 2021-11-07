package com.nunchuk.android.core.di

import com.nunchuk.android.core.api.PriceConverterAPI
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
internal object NetworkModule {

    @Singleton
    @Provides
    fun providePriceConverterAPI(retrofit: Retrofit): PriceConverterAPI = retrofit.create(PriceConverterAPI::class.java)

}