package com.nunchuk.android.notifications.di

import com.nunchuk.android.notifications.PushNotificationManager
import com.nunchuk.android.notifications.PushNotificationManagerImpl
import com.nunchuk.android.notifications.PushNotificationMessagingService
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(
    includes = [
        NotificationCommonModule::class,
        NotificationServiceModule::class
    ]
)
interface NotificationProxyModule

@Module
internal interface NotificationCommonModule {

    @Binds
    fun bindPushNotificationManager(manager: PushNotificationManagerImpl): PushNotificationManager

}

@Module
internal interface NotificationServiceModule {

    @ContributesAndroidInjector
    fun pushNotificationMessagingService(): PushNotificationMessagingService

}
