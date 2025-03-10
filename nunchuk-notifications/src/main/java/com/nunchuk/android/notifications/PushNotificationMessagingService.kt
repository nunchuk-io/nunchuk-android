/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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
import com.nunchuk.android.core.domain.message.HandlePushMessageUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.isAtLeastStarted
import com.nunchuk.android.messages.util.getContent
import com.nunchuk.android.messages.util.getGroupId
import com.nunchuk.android.messages.util.getLastMessageContentSafe
import com.nunchuk.android.messages.util.getMsgBody
import com.nunchuk.android.messages.util.getTitle
import com.nunchuk.android.messages.util.getTransactionId
import com.nunchuk.android.messages.util.getWalletId
import com.nunchuk.android.messages.util.isContactUpdateEvent
import com.nunchuk.android.messages.util.isCosignedAndBroadcastEvent
import com.nunchuk.android.messages.util.isCosignedEvent
import com.nunchuk.android.messages.util.isHealthCheckReminderEvent
import com.nunchuk.android.messages.util.isKeyRecoveryApproved
import com.nunchuk.android.messages.util.isKeyRecoveryRequest
import com.nunchuk.android.messages.util.isMessageEvent
import com.nunchuk.android.messages.util.isNunchukTransactionEvent
import com.nunchuk.android.messages.util.isNunchukWalletEvent
import com.nunchuk.android.messages.util.isRemoveAlias
import com.nunchuk.android.messages.util.isSetAlias
import com.nunchuk.android.messages.util.isTransactionReceived
import com.nunchuk.android.messages.util.isTransactionScheduleMissingSignaturesEvent
import com.nunchuk.android.messages.util.isTransactionScheduleNetworkRejectedEvent
import com.nunchuk.android.messages.util.isTransactionSignatureRequest
import com.nunchuk.android.messages.util.isWalletInheritancePlanningRequestDenied
import com.nunchuk.android.messages.util.lastMessageContent
import com.nunchuk.android.messages.util.lastMessageSender
import com.nunchuk.android.usecase.SaveHandledEventUseCase
import com.nunchuk.android.usecase.free.groupwallet.NotificationDeviceRegisterUseCase
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

    @Inject
    lateinit var saveHandledEventUseCase: SaveHandledEventUseCase

    @Inject
    lateinit var handlePushMessageUseCase: HandlePushMessageUseCase

    @Inject
    lateinit var notificationDeviceRegisterUseCase: NotificationDeviceRegisterUseCase

    @Inject
    lateinit var groupWalletPushNotificationManager: GroupWalletPushNotificationManager

    private val mUIHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (!NotificationUtils.areNotificationsEnabled(this) || remoteMessage.data.isEmpty()) {
            return
        }

        val data = remoteMessage.data

        applicationScope.launch {
            val handledByGroupWallet = groupWalletPushNotificationManager.parseNotification(data)?.let { notification ->
                showNotification(notification, intentProvider.getMainIntent())
                true
            } ?: false

            if (!handledByGroupWallet) {
                val event = getEvent(data)?.also { event ->
                    runCatching {
                        handlePushMessageUseCase(event)
                    }
                }

                mUIHandler.post {
                    parseMessageData(event)?.let(::showNotification)
                    if (ProcessLifecycleOwner.get().isAtLeastStarted().not() && event != null) {
                        applicationScope.launch {
                            saveHandledEventUseCase.invoke(event.eventId)
                        }
                    }
                }

                if (!ProcessLifecycleOwner.get().isAtLeastStarted()) {
                    onMessageReceivedInternal(remoteMessage.data)
                }
            }
        }
    }

    override fun onNewToken(refreshedToken: String) {
        try {
            notificationManager.enqueueRegisterPusherWithFcmKey(refreshedToken)
            applicationScope.launch {
                notificationDeviceRegisterUseCase(
                    NotificationDeviceRegisterUseCase.Param(
                        refreshedToken
                    )
                )
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
            // TODO create worker for retry
            mUIHandler.postDelayed({
                timelineService.getTimelineEvent(eventId)?.toPushNotificationData(roomId)
                    ?.let(::showNotification)
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
                id = localId,
                title = getString(R.string.notification_wallet_update),
                message = getString(R.string.notification_wallet_update_message),
                intent = intentProvider.getRoomDetailsIntent(roomId)
            )
        }

        isNunchukTransactionEvent() -> {
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_transaction_update),
                message = getString(R.string.notification_transaction_update_message),
                intent = intentProvider.getRoomDetailsIntent(roomId)
            )
        }

        isContactUpdateEvent() -> {
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_contact_update),
                message = lastMessageContent(this@PushNotificationMessagingService),
                intent = intentProvider.getMainIntent()
            )
        }

        isMessageEvent() -> {
            PushNotificationData(
                id = localId,
                title = lastMessageSender(),
                message = lastMessageContent(this@PushNotificationMessagingService),
                intent = intentProvider.getRoomDetailsIntent(roomId)
            )
        }

        isTransactionReceived() -> {
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_transaction_update),
                message = getMsgBody(),
                intent = intentProvider.getTransactionDetailIntent(
                    walletId = getWalletId().orEmpty(),
                    txId = getTransactionId().orEmpty(),
                )
            )
        }

        isCosignedEvent() -> {
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_transaction_update),
                message = getString(R.string.nc_notification_transaction_co_signed),
                intent = intentProvider.getTransactionDetailIntent(
                    walletId = getWalletId().orEmpty(),
                    txId = getTransactionId().orEmpty(),
                )
            )
        }

        isCosignedAndBroadcastEvent() -> {
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_transaction_update),
                message = getString(R.string.nc_notification_cosign_and_broadcast),
                intent = intentProvider.getTransactionDetailIntent(
                    walletId = getWalletId().orEmpty(),
                    txId = getTransactionId().orEmpty(),
                )
            )
        }

        isTransactionScheduleMissingSignaturesEvent() -> {
            val message = this.getLastMessageContentSafe().orEmpty()
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_transaction_update),
                message = message,
                intent = intentProvider.getTransactionDetailIntent(
                    walletId = getWalletId().orEmpty(),
                    txId = getTransactionId().orEmpty(),
                    isCancelBroadcast = true,
                    errorMessage = message
                )
            )
        }

        isTransactionScheduleNetworkRejectedEvent() -> {
            val message = this.getLastMessageContentSafe().orEmpty()
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_transaction_update),
                message = message,
                intent = intentProvider.getTransactionDetailIntent(
                    walletId = getWalletId().orEmpty(),
                    txId = getTransactionId().orEmpty(),
                    isCancelBroadcast = true,
                    errorMessage = message
                )
            )
        }

        isWalletInheritancePlanningRequestDenied() -> {
            val message = this.getLastMessageContentSafe().orEmpty()
            PushNotificationData(
                id = localId,
                title = message,
                message = "",
                intent = intentProvider.getGeneralIntent(getWalletId(), getGroupId(), null)
            )
        }

        isKeyRecoveryRequest() -> {
            val message = this.getLastMessageContentSafe().orEmpty()
            PushNotificationData(
                id = localId,
                title = getString(R.string.nc_notification_key_recovery_requested),
                message = message,
                intent = intentProvider.getGeneralIntent(getWalletId(), getGroupId(), null)
            )
        }

        isKeyRecoveryApproved() -> {
            val message = this.getLastMessageContentSafe().orEmpty()
            PushNotificationData(
                id = localId,
                title = getString(R.string.nc_notification_key_recovery_approved),
                message = message,
                intent = intentProvider.getGeneralIntent(getWalletId(), getGroupId(), null)
            )
        }

        isTransactionSignatureRequest() -> {
            PushNotificationData(
                id = localId,
                title = getTitle().orEmpty(),
                message = getContent().orEmpty(),
                intent = intentProvider.getGeneralIntent(getWalletId(), getGroupId(), null)
            )
        }

        isRemoveAlias() || isSetAlias() -> {
            PushNotificationData(
                id = localId,
                title = getTitle().orEmpty(),
                message = getContent().orEmpty(),
                intent = intentProvider.getAliasIntent(getWalletId().orEmpty())
            )
        }

        isHealthCheckReminderEvent() -> {
            PushNotificationData(
                id = localId,
                title = getTitle().orEmpty(),
                message = getMsgBody(),
                intent = intentProvider.getGeneralIntent(getWalletId(), getGroupId(), null)
            )
        }

        else -> defaultNotificationData(
            localId,
            getWalletId(),
            getGroupId(),
            getTransactionId(),
            getLastMessageContentSafe().orEmpty()
        )
    }

    private fun getActiveSession() = if (sessionHolder.hasActiveSession()) {
        sessionHolder.getSafeActiveSession()
    } else {
        getLastSession()
    }

    private fun getLastSession(): Session? =
        trySafe(matrix.authenticationService()::getLastAuthenticatedSession)

    private fun defaultNotificationData(
        localId: Long,
        walletId: String?,
        groupId: String?,
        transactionId: String?,
        message: String
    ) =
        if (!ProcessLifecycleOwner.get().isAtLeastStarted()) {
            PushNotificationData(
                id = localId,
                title = getString(R.string.notification_update),
                message = message.ifEmpty { getString(R.string.notification_update_message) },
                intent = intentProvider.getGeneralIntent(walletId, groupId, transactionId)
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
