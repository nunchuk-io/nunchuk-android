package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.core.util.getString
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemMessageMeBinding
import com.nunchuk.android.messages.databinding.ItemMessageNotificationBinding
import com.nunchuk.android.messages.databinding.ItemMessagePartnerBinding
import org.matrix.android.sdk.api.session.room.model.Membership

internal class RoomDetailsAdapter(
    val context: Context
) : Adapter<ViewHolder>() {

    internal var messages: List<Message> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        MessageType.CHAT_MINE.index -> MineViewHolder(
            ItemMessageMeBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.CHAT_PARTNER.index -> PartnerHolder(
            ItemMessagePartnerBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.NOTIFICATION.index -> NotificationHolder(
            ItemMessageNotificationBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        else -> throw IllegalArgumentException("Invalid type")
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        return messages[position].type
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageData = messages[position]
        when (getItemViewType(position)) {
            MessageType.CHAT_MINE.index -> (holder as MineViewHolder).bind(messageData)
            MessageType.CHAT_PARTNER.index -> (holder as PartnerHolder).bind(messageData)
            MessageType.NOTIFICATION.index -> (holder as NotificationHolder).bind(messageData as NotificationMessage)
        }
    }

    internal class MineViewHolder(val binding: ItemMessageMeBinding) : ViewHolder(binding.root) {

        fun bind(messageData: Message) {
            binding.message.text = messageData.content
            val state = messageData.state
            if (state.isSent()) {
                binding.status.text = getString(R.string.nc_message_status_delivered)
            } else if (state.isSending() || state.isInProgress()) {
                binding.status.text = getString(R.string.nc_message_status_sending)
            } else if (state.hasFailed()) {
                binding.status.text = getString(R.string.nc_message_status_failed)
            } else {
                binding.status.text = getString(R.string.nc_message_status_unknown)
            }
        }

    }

    internal class PartnerHolder(val binding: ItemMessagePartnerBinding) : ViewHolder(binding.root) {

        fun bind(messageData: Message) {
            val userName = messageData.sender
            binding.avatar.text = userName.shorten()
            binding.sender.text = userName
            binding.message.text = messageData.content
        }

    }

    internal class NotificationHolder(val binding: ItemMessageNotificationBinding) : ViewHolder(binding.root) {

        fun bind(message: NotificationMessage) {
            val userName = message.sender
            when (message.membership) {
                Membership.INVITE -> binding.member.text = getString(R.string.nc_message_member_invite_group, userName, message.content)
                Membership.JOIN -> binding.member.text = getString(R.string.nc_message_member_join_group, userName)
                Membership.KNOCK -> binding.member.text = getString(R.string.nc_message_member_kick_group, userName)
                Membership.LEAVE -> binding.member.text = getString(R.string.nc_message_member_left_group, userName)
                Membership.BAN -> getString(R.string.nc_message_member_ban_group, userName)
                else -> itemView.isVisible = false
            }
        }

    }

}
