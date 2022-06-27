package com.nunchuk.android.core.di

import com.nunchuk.android.core.profile.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface UserProfileDomainModule {
    @Binds
    fun bindCompromiseUserDevicesUseCase(userCase: CompromiseUserDevicesUseCaseImpl): CompromiseUserDevicesUseCase

    @Binds
    fun bindDeleteUserDevicesUseCase(userCase: DeleteUserDevicesUseCaseImpl): DeleteUserDevicesUseCase

    @Binds
    fun bindGetUserDevicesUseCase(userCase: GetUserDevicesUseCaseImpl): GetUserDevicesUseCase

    @Binds
    fun bindGetUserProfileUseCase(userCase: GetUserProfileUseCaseImpl): GetUserProfileUseCase

    @Binds
    fun bindUpdateUserProfileUseCase(userCase: UpdateUseProfileUseCaseImpl): UpdateUseProfileUseCase

}

@Module
@InstallIn(SingletonComponent::class)
internal interface UserProfileDataModule {

    @Binds
    fun bindGetUserProfileRepository(userCase: UserProfileRepositoryImpl): UserProfileRepository

}

@Module
@InstallIn(SingletonComponent::class)
internal object UserProfileNetworkModule {

    @Singleton
    @Provides
    fun provideUserProfileApi(retrofit: Retrofit): UserProfileApi =
        retrofit.create(UserProfileApi::class.java)

}