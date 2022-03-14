package com.nunchuk.android.messages.components.detail

sealed class AbsChatModel {
    abstract fun getType(): Int
}

class DateModel(val date: String) : AbsChatModel() {
    override fun getType() = MessageType.TYPE_DATE.index
}

class MessageModel(val message: Message) : AbsChatModel() {
    override fun getType() = message.type
}

object BannerNewChatModel : AbsChatModel() {
    override fun getType() = MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index
}
