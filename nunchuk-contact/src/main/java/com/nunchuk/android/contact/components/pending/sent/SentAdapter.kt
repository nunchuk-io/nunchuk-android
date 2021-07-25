package com.nunchuk.android.contact.components.pending.sent

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.databinding.ItemSentBinding
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.model.SentContact
import com.nunchuk.android.widget.util.inflate

internal class SentAdapter(
    private val listener: (SentContact) -> Unit
) : RecyclerView.Adapter<ContactViewHolder>() {

    internal var items: List<SentContact> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactViewHolder(
        parent.inflate(R.layout.item_sent),
        listener
    )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class ContactViewHolder(
    itemView: View,
    val listener: (SentContact) -> Unit
) : BaseViewHolder<SentContact>(itemView) {

    private val binding = ItemSentBinding.bind(itemView)

    override fun bind(data: SentContact) {
        val contact = data.contact
        binding.avatar.text = contact.name.shorten()
        binding.name.text = contact.name
        binding.email.text = contact.email
        binding.withdraw.setOnClickListener { listener(data) }
    }

}