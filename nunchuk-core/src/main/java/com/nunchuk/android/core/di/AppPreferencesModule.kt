package com.nunchuk.android.core.di

import android.content.Context
import com.nunchuk.android.core.persistence.NCSharePreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
internal class AppPreferencesModule {
    @Provides
    @Singleton
    fun bindAppSharePreferences(context: Context): NCSharePreferences = NCSharePreferences(context)

}