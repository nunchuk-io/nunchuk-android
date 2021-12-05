package com.nunchuk.android.messages.util

import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

// Naming follow Matrix's convention
const val STATE_NUNCHUK_WALLET = "io.nunchuk.wallet"
const val STATE_NUNCHUK_TRANSACTION = "io.nunchuk.transaction"
const val STATE_NUNCHUK_SYNC = "io.nunchuk.sync"
const val STATE_NUNCHUK_CONTACT_REQUEST = "io.nunchuk.custom.contact_request"
const val STATE_ROOM_SERVER_NOTICE = "m.server_notice"

fun TimelineEvent.isDisplayable() = isMessageEvent() || isNotificationEvent() || isNunchukEvent()

fun TimelineEvent.isNotificationEvent() = isRoomMemberEvent() || isRoomCreateEvent() || isRoomNameEvent()

fun TimelineEvent.isRoomCreateEvent() = root.getClearType() == EventType.STATE_ROOM_CREATE

fun TimelineEvent.isRoomMemberEvent() = root.getClearType() == EventType.STATE_ROOM_MEMBER

fun TimelineEvent.isRoomNameEvent() = root.getClearType() == EventType.STATE_ROOM_NAME

fun TimelineEvent.isMessageEvent() = root.isTextMessage()

fun TimelineEvent.isNunchukEvent() = isNunchukWalletEvent() || isNunchukTransactionEvent()

fun TimelineEvent.isNunchukWalletEvent() = root.type == STATE_NUNCHUK_WALLET

fun TimelineEvent.isNunchukTransactionEvent() = root.type == STATE_NUNCHUK_TRANSACTION

fun TimelineEvent.isNunchukConsumeSyncEvent() = root.type == STATE_NUNCHUK_SYNC

fun TimelineEvent.isContactRequestEvent() = root.content?.get("msgtype") == STATE_NUNCHUK_CONTACT_REQUEST
