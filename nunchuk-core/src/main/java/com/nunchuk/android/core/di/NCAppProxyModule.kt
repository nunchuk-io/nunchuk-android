package com.nunchuk.android.core.di

import com.nunchuk.android.core.domain.CheckUpdateRecommendUseCase
import com.nunchuk.android.core.domain.CheckUpdateRecommendUseCaseImpl
import com.nunchuk.android.core.data.NCAppApi
import com.nunchuk.android.core.repository.NCAppRepository
import com.nunchuk.android.core.repository.NCAppRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module(
    includes = [
        NCAppDomainModule::class,
        NCAppDataModule::class,
        NCAppNetworkModule::class
    ]
)
interface NCAppProxyModule

@Module
internal interface NCAppDomainModule {
    @Binds
    fun bindCheckUpdateRecommendUseCase(userCase: CheckUpdateRecommendUseCaseImpl): CheckUpdateRecommendUseCase
}

@Module
internal interface NCAppDataModule {

    @Binds
    fun bindNCAppRepository(userCase: NCAppRepositoryImpl): NCAppRepository

}

@Module
internal object NCAppNetworkModule {

    @Singleton
    @Provides
    fun provideNCAppApi(retrofit: Retrofit): NCAppApi =
        retrofit.create(NCAppApi::class.java)

}