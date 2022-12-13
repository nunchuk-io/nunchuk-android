package com.nunchuk.android.core.network.di

import com.nunchuk.android.api.key.MembershipApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkDatasourceModule {

    @Singleton
    @Provides
    fun provideMembershipApi(retrofit: Retrofit): MembershipApi = retrofit.create(MembershipApi::class.java)
}