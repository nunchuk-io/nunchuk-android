package com.nunchuk.android.domain.di

import android.util.LruCache
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.type.MiniscriptTimelockBased
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheComponent {
    companion object {
        @Provides
        @Singleton
        fun provideServerTransactionCache() = LruCache<String, ServerTransaction>(100)
        
        @Provides
        @Singleton
        fun provideTimelockTransactionCache() = LruCache<String, Long>(1000)

        @Provides
        @Singleton
        fun provideWalletLockedType() = LruCache<String, MiniscriptTimelockBased>(1000)
    }
}