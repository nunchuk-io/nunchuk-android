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

package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isFileMessage
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

// Naming follow Matrix's convention
const val STATE_NUNCHUK_WALLET = "io.nunchuk.wallet"
const val STATE_NUNCHUK_TRANSACTION = "io.nunchuk.transaction"
const val STATE_NUNCHUK_ERROR = "io.nunchuk.error"
const val STATE_NUNCHUK_SYNC = "io.nunchuk.sync"
const val STATE_NUNCHUK_CONTACT_REQUEST = "io.nunchuk.custom.contact_request"
const val STATE_NUNCHUK_CONTACT_INVITATION_ACCEPTED = "io.nunchuk.custom.invitation_accepted"
const val STATE_NUNCHUK_CONTACT_REQUEST_ACCEPTED = "io.nunchuk.custom.contact_request_accepted"
const val STATE_NUNCHUK_CONTACT_WITHDRAW_INVITATION = "io.nunchuk.custom.withdraw_invitation"
const val TRANSACTION_CO_SIGNED = "io.nunchuk.custom.transaction_co_signed"
const val TRANSACTION_SCHEDULE_BROADCAST = "io.nunchuk.custom.transaction_schedule_broadcast"
const val SUBSCRIPTION_SUBSCRIPTION_ACTIVE = "io.nunchuk.custom.subscription_activated"
const val SUBSCRIPTION_SUBSCRIPTION_PENDING = "io.nunchuk.custom.subscription_pending"
const val TRANSACTION_CO_SIGNED_AND_BROADCAST =
    "io.nunchuk.custom.transaction_co_signed_and_broadcast"
const val TRANSACTION_SCHEDULE_MISSING_SIGNATURES = "io.nunchuk.custom.transaction_schedule_missing_signatures"
const val TRANSACTION_SCHEDULE_NETWORK_REJECTED = "io.nunchuk.custom.transaction_schedule_network_rejected"
const val TRANSACTION_UPDATED = "io.nunchuk.custom.transaction_updated"
const val TRANSACTION_RECEIVED = "io.nunchuk.custom.wallet_receive_transaction"
const val ADD_DESKTOP_KEY_COMPLETED = "io.nunchuk.custom.draft_wallet_add_key_request_completed"
const val EVENT_WALLET_CREATED = "io.nunchuk.custom.wallet_created"
const val EVENT_TRANSACTION_CANCEL = "io.nunchuk.custom.transaction_canceled"
const val STATE_ENCRYPTED_MESSAGE = "*Encrypted*"
const val GROUP_MEMBERSHIP_REQUEST_CREATED = "io.nunchuk.custom.group_membership_request_created"
const val GROUP_MEMBERSHIP_REQUEST_ACCEPTED = "io.nunchuk.custom.group_membership_request_accepted"
const val GROUP_MEMBERSHIP_REQUEST_DENIED = "io.nunchuk.custom.group_membership_request_denied"
const val DRAFT_WALLET_RESET = "io.nunchuk.custom.draft_wallet_reset"
const val GROUP_WALLET_CREATED = "io.nunchuk.custom.group_wallet_created"
const val GROUP_EMERGENCY_LOCKDOWN_STARTED = "io.nunchuk.custom.group_emergency_lockdown_started"

fun TimelineEvent.isDisplayable(isSupportRoom: Boolean) : Boolean {
    return if (isSupportRoom.not()) {
        isMessageEvent() || isEncryptedEvent() || isNotificationEvent() || isNunchukEvent()
    } else {
        isMessageEvent() || isEncryptedEvent() || isNunchukEvent()
    }
}

fun TimelineEvent.isNotificationEvent() =
    isRoomMemberEvent() || isRoomCreateEvent() || isRoomNameEvent()

fun TimelineEvent.isRoomCreateEvent() = root.getClearType() == EventType.STATE_ROOM_CREATE

fun TimelineEvent.isRoomMemberEvent() = root.getClearType() == EventType.STATE_ROOM_MEMBER

fun TimelineEvent.isEncryptedEvent() = root.getClearType() == EventType.ENCRYPTED

fun TimelineEvent.isRoomNameEvent() = root.getClearType() == EventType.STATE_ROOM_NAME

fun TimelineEvent.isMessageEvent() = root.isTextMessage() || root.isVideoMessage() || root.isImageMessage() || root.isFileMessage()

fun TimelineEvent.isNunchukEvent() =
    isNunchukWalletEvent() || isNunchukTransactionEvent() || isNunchukErrorEvent()

fun TimelineEvent.isNunchukWalletEvent() = root.getClearType() == STATE_NUNCHUK_WALLET

fun TimelineEvent.isNunchukTransactionEvent() = root.getClearType() == STATE_NUNCHUK_TRANSACTION

fun TimelineEvent.isNunchukConsumeSyncEvent() = root.getClearType() == STATE_NUNCHUK_SYNC

fun TimelineEvent.isNunchukErrorEvent() = root.getClearType() == STATE_NUNCHUK_ERROR

fun TimelineEvent.isContactUpdateEvent() =
    isContactRequestEvent() || isContactWithdrawInvitationEvent() || isContactRequestAcceptedEvent() || isContactInvitationAcceptedEvent()

fun TimelineEvent.isContactRequestEvent() = getMsgType() == STATE_NUNCHUK_CONTACT_REQUEST

fun TimelineEvent.isContactWithdrawInvitationEvent() =
    getMsgType() == STATE_NUNCHUK_CONTACT_WITHDRAW_INVITATION

fun TimelineEvent.isContactRequestAcceptedEvent() =
    getMsgType() == STATE_NUNCHUK_CONTACT_REQUEST_ACCEPTED

fun TimelineEvent.isContactInvitationAcceptedEvent() =
    getMsgType() == STATE_NUNCHUK_CONTACT_INVITATION_ACCEPTED

fun TimelineEvent.isServerTransactionEvent() = isCosignedEvent() || isBroadcastEvent() || isCosignedAndBroadcastEvent() || isTransactionUpdateEvent()

fun TimelineEvent.isCosignedEvent() = getMsgType() == TRANSACTION_CO_SIGNED

fun TimelineEvent.isBroadcastEvent() = getMsgType() == TRANSACTION_SCHEDULE_BROADCAST

fun TimelineEvent.isCosignedAndBroadcastEvent() =
    getMsgType() == TRANSACTION_CO_SIGNED_AND_BROADCAST

fun TimelineEvent.isTransactionUpdateEvent() =
    getMsgType() == TRANSACTION_UPDATED

fun TimelineEvent.isTransactionScheduleMissingSignaturesEvent() = getMsgType() == TRANSACTION_SCHEDULE_MISSING_SIGNATURES
fun TimelineEvent.isTransactionScheduleNetworkRejectedEvent() = getMsgType() == TRANSACTION_SCHEDULE_NETWORK_REJECTED

fun TimelineEvent.isTransactionReceived() = getMsgType() == TRANSACTION_RECEIVED
fun TimelineEvent.isAddKeyCompleted() = getMsgType() == ADD_DESKTOP_KEY_COMPLETED
fun TimelineEvent.isWalletCreated() = getMsgType() == EVENT_WALLET_CREATED
fun TimelineEvent.isTransactionCancelled() = getMsgType() == EVENT_TRANSACTION_CANCEL

fun TimelineEvent.isTransactionHandleErrorMessageEvent() = isTransactionScheduleMissingSignaturesEvent() || isTransactionScheduleNetworkRejectedEvent()

fun TimelineEvent.getMsgType() = root.getClearContent()?.get("msgtype")

fun TimelineEvent.getMsgBody() = root.getClearContent()?.get("body")?.toString().orEmpty()

fun TimelineEvent.getTransactionId() = root.getClearContent()?.get("transaction_id")?.toString()

fun TimelineEvent.getWalletId() = root.getClearContent()?.get("wallet_local_id")?.toString()
fun TimelineEvent.getGroupId() = root.getClearContent()?.get("group_id")?.toString()

fun TimelineEvent.isGroupMembershipRequestEvent() =
    getMsgType() == GROUP_MEMBERSHIP_REQUEST_ACCEPTED || getMsgType() == GROUP_MEMBERSHIP_REQUEST_DENIED

fun TimelineEvent.isDraftWalletResetEvent() =
    getMsgType() == DRAFT_WALLET_RESET

fun TimelineEvent.isGroupMembershipRequestCreatedEvent() =
    getMsgType() == GROUP_MEMBERSHIP_REQUEST_CREATED

fun TimelineEvent.isGroupWalletCreatedEvent() =
    getMsgType() == GROUP_WALLET_CREATED

fun TimelineEvent.isGroupEmergencyLockdownStarted() =
    getMsgType() == GROUP_EMERGENCY_LOCKDOWN_STARTED