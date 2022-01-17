package com.nunchuk.android.app.provider

import android.content.Context
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.messages.components.detail.RoomDetailArgs
import com.nunchuk.android.notifications.PushNotificationIntentProvider
import javax.inject.Inject

class PushNotificationIntentProviderImpl @Inject constructor(
    private val context: Context
) : PushNotificationIntentProvider {

    override fun getRoomDetailsIntent(roomId: String) = RoomDetailArgs(roomId).buildIntent(context)

    override fun getMainIntent() = MainActivity.createIntent(context)
}