package com.nunchuk.android.app.di

import com.nunchuk.android.main.repository.MembershipRepositoryImpl
import com.nunchuk.android.repository.MembershipRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindMembershipRepository(repository: MembershipRepositoryImpl) : MembershipRepository
}