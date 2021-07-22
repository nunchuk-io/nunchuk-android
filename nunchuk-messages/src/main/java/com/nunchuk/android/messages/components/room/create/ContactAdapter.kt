package com.nunchuk.android.messages.components.room.create

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseViewHolder
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.databinding.ItemContactBinding
import com.nunchuk.android.model.Contact
import com.nunchuk.android.widget.util.inflate

internal class ContactsAdapter(
    private val listener: (Contact) -> Unit
) : RecyclerView.Adapter<ContactViewHolder>() {

    internal var items: List<Contact> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ContactViewHolder(
        parent.inflate(R.layout.item_contact),
        listener
    )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}

internal class ContactViewHolder(
    itemView: View,
    val listener: (Contact) -> Unit
) : BaseViewHolder<Contact>(itemView) {

    private val binding = ItemContactBinding.bind(itemView)

    override fun bind(data: Contact) {
        binding.avatar.text = data.name.first().toUpperCase().toString()
        binding.name.text = data.name
        binding.email.text = data.email
        binding.root.setOnClickListener { listener(data) }
    }

}