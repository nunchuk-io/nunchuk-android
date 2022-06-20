package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.CheckUpdateRecommendUseCase
import com.nunchuk.android.core.domain.CheckUpdateRecommendUseCaseImpl
import com.nunchuk.android.core.data.NCAppApi
import com.nunchuk.android.core.repository.NCAppRepository
import com.nunchuk.android.core.repository.NCAppRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NCAppDomainModule {
    @Binds
    fun bindCheckUpdateRecommendUseCase(userCase: CheckUpdateRecommendUseCaseImpl): CheckUpdateRecommendUseCase
}

@Module
@InstallIn(SingletonComponent::class)
internal interface NCAppDataModule {

    @Binds
    fun bindNCAppRepository(userCase: NCAppRepositoryImpl): NCAppRepository

}

@Module
@InstallIn(SingletonComponent::class)
internal object NCAppNetworkModule {

    @Singleton
    @Provides
    fun provideNCAppApi(retrofit: Retrofit): NCAppApi =
        retrofit.create(NCAppApi::class.java)

}