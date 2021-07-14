package com.nunchuk.android.messages.pending.receive

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemReceivedBinding
import com.nunchuk.android.messages.model.ReceiveContact
import com.nunchuk.android.widget.util.inflate

internal class ReceivedAdapter(
    private val accept: (ReceiveContact) -> Unit,
    private val cancel: (ReceiveContact) -> Unit
) : RecyclerView.Adapter<ContactViewHolder>() {

    internal var items: List<ReceiveContact> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactViewHolder(
        parent.inflate(R.layout.item_received),
        accept, cancel
    )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class ContactViewHolder(
    itemView: View,
    val accept: (ReceiveContact) -> Unit,
    val cancel: (ReceiveContact) -> Unit
) : BaseViewHolder<ReceiveContact>(itemView) {

    private val binding = ItemReceivedBinding.bind(itemView)

    override fun bind(data: ReceiveContact) {
        val contact = data.contact
        binding.avatar.text = contact.name.first().toUpperCase().toString()
        binding.name.text = contact.name
        binding.email.text = contact.email
        binding.accept.setOnClickListener { accept(data) }
        binding.cancel.setOnClickListener { cancel(data) }
    }

}