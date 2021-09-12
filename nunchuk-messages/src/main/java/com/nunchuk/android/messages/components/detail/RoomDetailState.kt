package com.nunchuk.android.messages.components.detail

import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.RoomWalletData

data class RoomDetailState(
    val roomInfo: RoomInfo,
    val roomWallet: RoomWallet?,
    val messages: List<Message>
) {

    companion object {
        fun empty() = RoomDetailState(RoomInfo.empty(), null, emptyList())
    }

}

data class RoomInfo(val roomName: String, val memberCount: Int) {
    companion object {
        fun empty() = RoomInfo("", 0)
    }
}

sealed class RoomDetailEvent {
    object RoomNotFoundEvent : RoomDetailEvent()
    object ContactNotFoundEvent : RoomDetailEvent()
    object OpenChatInfoEvent : RoomDetailEvent()
    object OpenChatGroupInfoEvent : RoomDetailEvent()
    object RoomWalletCreatedEvent : RoomDetailEvent()
    data class ViewWalletConfigEvent(
        val roomId: String,
        val roomWalletData: RoomWalletData
    ) : RoomDetailEvent()
}