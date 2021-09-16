package com.nunchuk.android.messages.components.group

import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.Wallet
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.session.room.model.RoomSummary

data class ChatGroupInfoState(
    val summary: RoomSummary? = null,
    val roomMembers: List<RoomMemberSummary> = emptyList(),
    val roomWallet: RoomWallet? = null,
    val wallet: Wallet? = null
)

sealed class ChatGroupInfoEvent {
    object RoomNotFoundEvent : ChatGroupInfoEvent()
    object CreateSharedWalletEvent : ChatGroupInfoEvent()
    object LeaveRoomSuccess : ChatGroupInfoEvent()
    data class UpdateRoomNameError(val message: String) : ChatGroupInfoEvent()
    data class UpdateRoomNameSuccess(val name: String) : ChatGroupInfoEvent()
    data class LeaveRoomError(val message: String) : ChatGroupInfoEvent()
    data class CreateTransactionEvent(
        val roomId: String,
        val walletId: String,
        val availableAmount: Double
    ) : ChatGroupInfoEvent()
}