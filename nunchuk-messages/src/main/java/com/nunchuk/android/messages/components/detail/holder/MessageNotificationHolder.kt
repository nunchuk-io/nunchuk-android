package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.util.getHtmlString
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.NotificationMessage
import com.nunchuk.android.messages.databinding.ItemMessageNotificationBinding
import com.nunchuk.android.messages.util.*
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.Membership.*
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

internal class MessageNotificationHolder(val binding: ItemMessageNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: NotificationMessage) {
        val sender = message.sender.displayNameOrId()
        val event = message.timelineEvent
        when {
            event.isRoomMemberEvent() -> bindMembershipEvent(event, sender)
            event.isRoomNameEvent() -> bindRoomNameEvent(event, sender)
            event.isRoomCreateEvent() -> bindRoomCreateEvent(event, sender)
            else -> binding.notification.text = "${message.timelineEvent}"
        }
    }

    private fun bindRoomNameEvent(event: TimelineEvent, sender: String) {
        binding.notification.text = getHtmlString(R.string.nc_message_name_changed, sender, event.nameChange().orEmpty())
    }

    private fun bindRoomCreateEvent(event: TimelineEvent, sender: String) {
        val content: RoomCreateContent? = event.root.content.toModel()
        binding.notification.text = getHtmlString(R.string.nc_message_create_room, content?.creator ?: sender)
    }

    private fun bindMembershipEvent(event: TimelineEvent, sender: String) {
        val content: RoomMemberContent? = event.root.content.toModel()
        val user = content?.displayName ?: sender
        when (event.membership()) {
            INVITE -> binding.notification.text = getHtmlString(R.string.nc_message_member_invite_group, sender, user)
            JOIN -> binding.notification.text = getHtmlString(R.string.nc_message_member_join_group, user)
            KNOCK -> binding.notification.text = getHtmlString(R.string.nc_message_member_kick_group, user)
            LEAVE -> binding.notification.text = getHtmlString(R.string.nc_message_member_left_group, user)
            BAN -> getHtmlString(R.string.nc_message_member_ban_group, user)
            NONE -> {
            }
        }
    }

}