package com.nunchuk.android.database.di

import android.content.Context
import com.nunchuk.android.persistence.NunchukDatabase
import dagger.Module
import dagger.Provides

@Module
internal object NunchukPersistenceModule {

    @Provides
    fun provideDatabase(context: Context) = NunchukDatabase.getInstance(context)

    @Provides
    fun provideContactDao(database: NunchukDatabase) = database.contactDao()

}