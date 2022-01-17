package com.nunchuk.android.app.di

import com.nunchuk.android.app.network.HeaderProviderImpl
import com.nunchuk.android.app.provider.AppInfoProviderImpl
import com.nunchuk.android.app.provider.PushNotificationIntentProviderImpl
import com.nunchuk.android.core.network.HeaderProvider
import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.notifications.PushNotificationIntentProvider
import dagger.Binds
import dagger.Module

@Module
internal interface AppCommonModule {

    @Binds
    fun bindAppInfoProvider(nav: AppInfoProviderImpl): AppInfoProvider

    @Binds
    fun bindHeaderProvider(provider: HeaderProviderImpl): HeaderProvider

    @Binds
    fun bindPushNotificationIntentProvider(provider: PushNotificationIntentProviderImpl): PushNotificationIntentProvider

}