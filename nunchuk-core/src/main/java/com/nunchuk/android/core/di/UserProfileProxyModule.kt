package com.nunchuk.android.core.di

import com.nunchuk.android.core.profile.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton


@Module(
    includes = [
        UserProfileDomainModule::class,
        UserProfileDataModule::class,
        UserProfileNetworkModule::class
    ]
)
interface UserProfileProxyModule

@Module
internal interface UserProfileDomainModule {

    @Binds
    fun bindGetUserProfileUseCase(userCase: GetUserProfileUseCaseImpl): GetUserProfileUseCase

    @Binds
    fun bindUpdateUserProfileUseCase(userCase: UpdateUseProfileUseCaseImpl): UpdateUseProfileUseCase

}

@Module
internal interface UserProfileDataModule {

    @Binds
    fun bindGetUserProfileRepository(userCase: UserProfileRepositoryImpl): UserProfileRepository

}

@Module
internal object UserProfileNetworkModule {

    @Singleton
    @Provides
    fun provideUserProfileApi(retrofit: Retrofit): UserProfileApi =
        retrofit.create(UserProfileApi::class.java)

}