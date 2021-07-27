package com.nunchuk.android.messages.components.create

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.messages.databinding.ItemEmailBinding
import com.nunchuk.android.model.Contact
import com.nunchuk.android.widget.util.AbsViewBinder

class ReceiptsViewBinder(
    container: ViewGroup,
    receipts: List<Contact>,
    val callback: (Contact) -> Unit,
) : AbsViewBinder<Contact, ItemEmailBinding>(container, receipts) {

    override fun initializeBinding() = ItemEmailBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: Contact) {
        val binding = ItemEmailBinding.bind(container.getChildAt(position))
        container[position].apply {
            binding.email.text = model.name
            setOnClickListener { callback(model) }
        }
    }

}