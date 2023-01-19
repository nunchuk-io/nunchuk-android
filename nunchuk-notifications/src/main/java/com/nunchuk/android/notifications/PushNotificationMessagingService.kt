/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.notifications

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.isAtLeastStarted
import com.nunchuk.android.messages.util.*
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.trySafe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PushNotificationMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: PushNotificationHelper

    @Inject
    lateinit var notificationManager: PushNotificationManager

    @Inject
    lateinit var pushEventManager: PushEventManager

    @Inject
    lateinit var intentProvider: PushNotificationIntentProvider

    @Inject
    lateinit var matrix: Matrix

    @Inject
    lateinit var sessionHolder: SessionHolder

    @Inject
    lateinit var applicationScope: CoroutineScope

    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (!NotificationUtils.areNotificationsEnabled(this) || remoteMessage.data.isEmpty()) {
            return
        }
        val event = getEvent(remoteMessage.data)?.also {
            if (it.isServerTransactionEvent()) {
                applicationScope.launch {
                    pushEventManager.push(PushEvent.ServerTransactionEvent(it.getWalletId(), it.getTransactionId()))
                }
            }
        }

        mUIHandler.post {
            parseMessageData(event)?.let(::showNotification)
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
            if (NotificationUtils.areNotificationsEnabled(context = this) && sessionHolder.hasActiveSession()) {
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
                session.syncService().requireBackgroundSync()
            }
        } catch (e: Exception) {
            CrashlyticsReporter.recordException(e)
        }
    }

    private fun getEvent(data: Map<String, String>): TimelineEvent? {
        Timber.d(data.toString())
        val roomId = data[ROOM_ID]
        val eventId = data[EVENT_ID]
        if (null == eventId || null == roomId) return null
        if (roomId == sessionHolder.getActiveRoomIdSafe()) return null

        val session = getActiveSession() ?: return null
        val room = session.roomService().getRoom(roomId) ?: return null
        val timelineService = room.timelineService()
        val event = timelineService.getTimelineEvent(eventId)
        if (event == null) {
            mUIHandler.postDelayed({
                timelineService.getTimelineEvent(eventId)?.toPushNotificationData(roomId)?.let(::showNotification)
            }, RETRY_DELAY)
        }
        Timber.d(event.toString())
        return event
    }

    private fun parseMessageData(event: TimelineEvent?): PushNotificationData? {
        return event?.toPushNotificationData(event.roomId)
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
                message = lastMessageContent(this@PushNotificationMessagingService),
                intent = intentProvider.getMainIntent()
            )
        }
        isMessageEvent() -> {
            PushNotificationData(
                title = lastMessageSender(),
                message = lastMessageContent(this@PushNotificationMessagingService),
                intent = intentProvider.getRoomDetailsIntent(roomId)
            )
        }
        isCosignedEvent() -> {
            PushNotificationData(
                title = getString(R.string.notification_transaction_update),
                message = getString(R.string.nc_notification_transaction_co_signed),
                intent = intentProvider.getTransactionDetailIntent(getWalletId(), getTransactionId())
            )
        }
        isCosignedAndBroadcastEvent() -> {
            PushNotificationData(
                title = getString(R.string.notification_transaction_update),
                message = getString(R.string.nc_notification_cosign_and_broadcast),
                intent = intentProvider.getTransactionDetailIntent(getWalletId(), getTransactionId())
            )
        }
        else -> defaultNotificationData()
    }

    private fun getActiveSession() = if (sessionHolder.hasActiveSession()) {
        sessionHolder.getSafeActiveSession()
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
