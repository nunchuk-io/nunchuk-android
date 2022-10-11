package com.nunchuk.android.database.di

import android.content.Context
import com.nunchuk.android.persistence.NunchukDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NunchukPersistenceModule {

    @Singleton
    @Provides
    fun provideDatabase(context: Context) = NunchukDatabase.getInstance(context)

    @Singleton
    @Provides
    fun provideContactDao(database: NunchukDatabase) = database.contactDao()

    @Singleton
    @Provides
    fun provideSyncFileDao(database: NunchukDatabase) = database.syncFileDao()


    @Singleton
    @Provides
    fun provideSyncEventDao(database: NunchukDatabase) = database.syncEventDao()
}