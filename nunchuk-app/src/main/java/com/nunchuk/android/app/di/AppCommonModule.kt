package com.nunchuk.android.app.di

import android.app.Application
import android.content.Context
import com.nunchuk.android.app.network.HeaderProviderImpl
import com.nunchuk.android.app.provider.AppInfoProviderImpl
import com.nunchuk.android.app.provider.PushNotificationIntentProviderImpl
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.notifications.PushNotificationIntentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AppCommonModule {
    @Binds
    fun bindApplicationContext(application: Application): Context

    @Binds
    fun bindAppInfoProvider(nav: AppInfoProviderImpl): AppInfoProvider

    @Binds
    fun bindHeaderProvider(provider: HeaderProviderImpl): HeaderProvider

    @Binds
    fun bindPushNotificationIntentProvider(provider: PushNotificationIntentProviderImpl): PushNotificationIntentProvider
}