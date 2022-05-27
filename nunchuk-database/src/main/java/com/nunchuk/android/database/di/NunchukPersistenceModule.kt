package com.nunchuk.android.database.di

import android.content.Context
import com.nunchuk.android.persistence.NunchukDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
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


}