package com.nunchuk.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.messages.util.isContactRequestEvent
import com.nunchuk.android.messages.util.isMessageEvent
import com.nunchuk.android.messages.util.isNunchukTransactionEvent
import com.nunchuk.android.messages.util.isNunchukWalletEvent
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
    lateinit var pushNotificationManager: PushNotificationManager

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    private fun showNotificationWith(title: String, message: String) {
        val channelId = "io.nunchuk.android.channelId"
        val channelName = "Nunchuk Notification Center"
        val builder = NotificationCompat.Builder(applicationContext, channelId)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setSmallIcon(R.drawable.ic_notification)
        builder.setContentTitle(title)
        builder.setContentText(message)

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.priority = NotificationManager.IMPORTANCE_HIGH
            notificationManager.createNotificationChannel(NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH))
            builder.setChannelId(channelId)
        }

        notificationManager.notify(0, builder.build())
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (!NotificationUtils.areNotificationsEnabled(this) || remoteMessage.data.isEmpty()) {
            return
        }

        parseMessageData(remoteMessage.data)?.let {
            val title = it.first
            val message = it.second
            showNotificationWith(title, message)
        }

        mUIHandler.post {
            if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                onMessageReceivedInternal(remoteMessage.data)
            }
        }
    }

    override fun onNewToken(refreshedToken: String) {
        try {
            notificationHelper.storeFcmToken(refreshedToken)
            if (NotificationUtils.areNotificationsEnabled(context = this) && SessionHolder.hasActiveSession()) {
                pushNotificationManager.enqueueRegisterPusherWithFcmKey(refreshedToken)
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

    private fun parseMessageData(data: Map<String, String>): Pair<String, String>? {
        val roomId = data[ROOM_ID]
        val eventId = data[EVENT_ID]
        if (null == eventId || null == roomId) return emptyNotification()
        val session = getActiveSession() ?: return emptyNotification()
        val room = session.getRoom(roomId) ?: return emptyNotification()
        val timeLineEvent: TimelineEvent = room.getTimeLineEvent(eventId) ?: return emptyNotification()
        return when {
            timeLineEvent.isNunchukWalletEvent() -> getString(R.string.notification_wallet_update) to getString(R.string.notification_wallet_update_message)
            timeLineEvent.isNunchukTransactionEvent() -> getString(R.string.notification_transaction_update) to getString(R.string.notification_transaction_update_message)
            timeLineEvent.isContactRequestEvent() -> getString(R.string.notification_contact_update) to getString(R.string.notification_contact_update_message)
            timeLineEvent.isMessageEvent() -> getString(R.string.notification_text_message_update) to (timeLineEvent.getLastMessageContent()?.body ?: timeLineEvent.getTextEditableContent())
            else -> emptyNotification()
        }
    }

    private fun getActiveSession() = SessionHolder.activeSession ?: getLastSession()

    private fun getLastSession(): Session? {
        val matrix = Matrix.getInstance(applicationContext)
        return matrix.authenticationService().getLastAuthenticatedSession()
    }

    private fun emptyNotification(): Pair<String, String>? {
        return if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            getString(R.string.notification_update) to getString(R.string.notification_update_message)
        } else null
    }

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
    }

}
