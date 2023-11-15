package com.nunchuk.android.domain.di

import android.util.LruCache
import com.nunchuk.android.model.transaction.ServerTransaction
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
    }
}