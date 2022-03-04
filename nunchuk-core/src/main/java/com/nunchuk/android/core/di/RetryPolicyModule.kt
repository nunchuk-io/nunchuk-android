package com.nunchuk.android.core.di

import com.nunchuk.android.core.retry.*
import com.nunchuk.android.network.util.MATRIX_RETROFIT
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
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