package com.nunchuk.android.messages.components.detail.holder

import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.messages.components.detail.DateModel
import com.nunchuk.android.messages.databinding.ItemDateBinding
import com.nunchuk.android.messages.util.formatMessageDate
import com.nunchuk.android.messages.util.simpleDateFormat

internal class MessageDateHolder(val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(model: DateModel) {
        binding.date.text = model.date.simpleDateFormat().formatMessageDate(true)
    }

}