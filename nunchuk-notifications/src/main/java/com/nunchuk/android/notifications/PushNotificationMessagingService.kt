package com.nunchuk.android.notifications

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.NotificationUtils
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.getTextEditableContent
import javax.inject.Inject

class PushNotificationMessagingService : FirebaseMessagingService(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var notificationHelper: PushNotificationHelper

    @Inject
    lateinit var notificationManager: PushNotificationManager

    @Inject
    lateinit var intentProvider: PushNotificationIntentProvider

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
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
        val room = session.getRoom(roomId) ?: return defaultNotificationData()
        val event = room.getTimeLineEvent(eventId)
        if (event == null) {
            mUIHandler.postDelayed({
                room.getTimeLineEvent(eventId)?.toPushNotificationData(roomId)?.let(::showNotification)
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
        isContactRequestEvent() -> {
            PushNotificationData(
                title = getString(R.string.notification_contact_update),
                message = (getLastMessageContent()?.body ?: getTextEditableContent()),
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

    private fun getLastSession(): Session? {
        val matrix = Matrix.getInstance(applicationContext)
        return matrix.authenticationService().getLastAuthenticatedSession()
    }

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
                val room = session.getRoom(roomId) ?: return false
                return room.getTimeLineEvent(eventId) != null
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

fun LifecycleOwner.isAtLeastStarted() = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
