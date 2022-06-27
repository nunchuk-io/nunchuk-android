package com.nunchuk.android.notifications

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.isAtLeastStarted
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.trySafe
import dagger.hilt.android.AndroidEntryPoint
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import javax.inject.Inject

@AndroidEntryPoint
class PushNotificationMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: PushNotificationHelper

    @Inject
    lateinit var notificationManager: PushNotificationManager

    @Inject
    lateinit var intentProvider: PushNotificationIntentProvider

    @Inject
    lateinit var matrix: Matrix

    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (!NotificationUtils.areNotificationsEnabled(this) || remoteMessage.data.isEmpty()) {
            return
        }

        mUIHandler.post {
            parseMessageData(remoteMessage.data)?.let(::showNotification)
        }

        mUIHandler.post {
            if (!ProcessLifecycleOwner.get().isAtLeastStarted()) {
                onMessageReceivedInternal(remoteMessage.data)
            }
        }
    }

    override fun onNewToken(refreshedToken: String) {
        try {
            notificationHelper.storeFcmToken(refreshedToken)
            if (NotificationUtils.areNotificationsEnabled(context = this) && SessionHolder.hasActiveSession()) {
                notificationManager.enqueueRegisterPusherWithFcmKey(refreshedToken)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onDeletedMessages() {
    }

    private fun onMessageReceivedInternal(data: Map<String, String>) {
        try {
            val session = getActiveSession()
            if (session == null) {
                CrashlyticsReporter.recordException(Exception(getString(R.string.notification_cant_sync_push)))
            } else if (!isEventAlreadyKnown(session, data[EVENT_ID], data[ROOM_ID])) {
                session.requireBackgroundSync()
            }
        } catch (e: Exception) {
            CrashlyticsReporter.recordException(e)
        }
    }

    private fun parseMessageData(data: Map<String, String>): PushNotificationData? {
        val roomId = data[ROOM_ID]
        val eventId = data[EVENT_ID]
        if (null == eventId || null == roomId) return defaultNotificationData()
        if (roomId == SessionHolder.getActiveRoomIdSafe()) return null

        val session = getActiveSession() ?: return defaultNotificationData()
        val room = session.roomService().getRoom(roomId) ?: return defaultNotificationData()
        val timelineService = room.timelineService()
        val event = timelineService.getTimelineEvent(eventId)
        if (event == null) {
            mUIHandler.postDelayed({
                timelineService.getTimelineEvent(eventId)?.toPushNotificationData(roomId)?.let(::showNotification)
            }, RETRY_DELAY)
        }
        return event?.toPushNotificationData(roomId)
    }

    private fun TimelineEvent.toPushNotificationData(roomId: String) = when {
        isNunchukWalletEvent() -> {
            PushNotificationData(
                title = getString(R.string.notification_wallet_update),
                message = getString(R.string.notification_wallet_update_message),
                intent = intentProvider.getRoomDetailsIntent(roomId)
            )
        }
        isNunchukTransactionEvent() -> {
            PushNotificationData(
                title = getString(R.string.notification_transaction_update),
                message = getString(R.string.notification_transaction_update_message),
                intent = intentProvider.getRoomDetailsIntent(roomId)
            )
        }
        isContactUpdateEvent() -> {
            PushNotificationData(
                title = getString(R.string.notification_contact_update),
                message = lastMessageContent(),
                intent = intentProvider.getMainIntent()
            )
        }
        isMessageEvent() -> {
            PushNotificationData(
                title = lastMessageSender(),
                message = lastMessageContent(),
                intent = intentProvider.getRoomDetailsIntent(roomId)
            )
        }
        else -> defaultNotificationData()
    }

    private fun getActiveSession() = if (SessionHolder.hasActiveSession()) {
        SessionHolder.activeSession
    } else {
        getLastSession()
    }

    private fun getLastSession(): Session? = trySafe(matrix.authenticationService()::getLastAuthenticatedSession)

    private fun defaultNotificationData() = if (!ProcessLifecycleOwner.get().isAtLeastStarted()) {
        PushNotificationData(
            title = getString(R.string.notification_update),
            message = getString(R.string.notification_update_message),
            intent = intentProvider.getMainIntent()
        )
    } else null

    private fun isEventAlreadyKnown(session: Session, eventId: String?, roomId: String?): Boolean {
        if (null != eventId && null != roomId) {
            try {
                val room = session.roomService().getRoom(roomId) ?: return false
                return room.timelineService().getTimelineEvent(eventId) != null
            } catch (e: Exception) {
                CrashlyticsReporter.recordException(e)
            }
        }
        return false
    }

    companion object {
        private const val EVENT_ID = "event_id"
        private const val ROOM_ID = "room_id"
        private const val RETRY_DELAY = 500L
    }

}
