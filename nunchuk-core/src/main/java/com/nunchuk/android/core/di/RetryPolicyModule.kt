package com.nunchuk.android.core.di

import com.nunchuk.android.core.retry.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RetryPolicyModule {

    @Provides
    @Singleton
    @Named(DEFAULT_RETRY_POLICY)
    fun bindDefaultRetryPolicy(): RetryPolicy = DefaultRetryPolicy()

    @Provides
    @Singleton
    @Named(SYNC_RETRY_POLICY)
    fun bindSyncRetryPolicy(): RetryPolicy = SyncRetryPolicy()

}