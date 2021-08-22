package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.messages.components.detail.holder.MessageDateHolder
import com.nunchuk.android.messages.components.detail.holder.MessageMineViewHolder
import com.nunchuk.android.messages.components.detail.holder.MessageNotificationHolder
import com.nunchuk.android.messages.components.detail.holder.MessagePartnerHolder
import com.nunchuk.android.messages.databinding.ItemDateBinding
import com.nunchuk.android.messages.databinding.ItemMessageMeBinding
import com.nunchuk.android.messages.databinding.ItemMessageNotificationBinding
import com.nunchuk.android.messages.databinding.ItemMessagePartnerBinding

internal class MessagesAdapter(
    val context: Context
) : Adapter<ViewHolder>() {

    internal var chatModels: List<AbsChatModel> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        MessageType.TYPE_CHAT_MINE.index -> MessageMineViewHolder(
            ItemMessageMeBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_CHAT_PARTNER.index -> MessagePartnerHolder(
            ItemMessagePartnerBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_NOTIFICATION.index -> MessageNotificationHolder(
            ItemMessageNotificationBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_DATE.index -> MessageDateHolder(
            ItemDateBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        else -> throw IllegalArgumentException("Invalid type")
    }

    override fun getItemCount(): Int = chatModels.size

    override fun getItemViewType(position: Int): Int = chatModels[position].getType()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageData = chatModels[position]
        when (getItemViewType(position)) {
            MessageType.TYPE_CHAT_MINE.index -> (holder as MessageMineViewHolder).bind((messageData as MessageModel).message)
            MessageType.TYPE_CHAT_PARTNER.index -> (holder as MessagePartnerHolder).bind((messageData as MessageModel).message)
            MessageType.TYPE_NOTIFICATION.index -> (holder as MessageNotificationHolder).bind((messageData as MessageModel).message as NotificationMessage)
            MessageType.TYPE_DATE.index -> (holder as MessageDateHolder).bind(messageData as DateModel)
        }
    }

}
