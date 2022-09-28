package com.nunchuk.android.messages.components.detail

import androidx.recyclerview.widget.DiffUtil

internal object ChatMessageDiffCallback : DiffUtil.ItemCallback<AbsChatModel>() {
    override fun areItemsTheSame(p0: AbsChatModel, p1: AbsChatModel): Boolean {
        return p0.getType() == p1.getType()
    }

    override fun areContentsTheSame(p0: AbsChatModel, p1: AbsChatModel): Boolean {
        if (p0 is DateModel && p1 is DateModel) {
            return p0.getType() == p1.getType() && p0.date == p1.date
        } else if (p0 is MessageModel && p1 is MessageModel) {
            return areMessagesTheSame(p0.message, p1.message)
        }
        return p0.getType() == p1.getType()
    }

    private fun areMessagesTheSame(p0: Message, p1: Message): Boolean = p0 == p1
}