package com.nunchuk.android.notifications.di

import com.nunchuk.android.notifications.PushNotificationManager
import com.nunchuk.android.notifications.PushNotificationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationCommonModule {

    @Binds
    fun bindPushNotificationManager(manager: PushNotificationManagerImpl): PushNotificationManager

}
