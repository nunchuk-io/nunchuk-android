package com.nunchuk.android.messages.room.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.messages.R

class RoomDetailsAdapter(
    val context: Context
) : RecyclerView.Adapter<RoomDetailsAdapter.ViewHolder>() {

    internal var messages: List<Message> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = when (viewType) {
            MessageType.CHAT_MINE.index -> LayoutInflater.from(context).inflate(R.layout.item_message_me, parent, false)
            MessageType.CHAT_PARTNER.index -> LayoutInflater.from(context).inflate(R.layout.item_message_partner, parent, false)
            else -> throw IllegalArgumentException("Invalid type")
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        return messages[position].type
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageData = messages[position]
        val userName = messageData.sender
        val content = messageData.content
        when (messageData.type) {
            MessageType.CHAT_MINE.index -> holder.message.text = content
            MessageType.CHAT_PARTNER.index -> {
                holder.sender?.text = userName
                holder.message.text = content
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sender: TextView? = itemView.findViewById(R.id.sender)
        val message: TextView = itemView.findViewById(R.id.message)
    }

}