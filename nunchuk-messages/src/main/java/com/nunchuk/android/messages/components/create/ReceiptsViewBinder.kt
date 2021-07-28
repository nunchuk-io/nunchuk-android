package com.nunchuk.android.messages.components.create

import android.view.ViewGroup
import androidx.core.view.get
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.databinding.ItemReceiptBinding
import com.nunchuk.android.model.Contact
import com.nunchuk.android.widget.util.AbsViewBinder

class ReceiptsViewBinder(
    container: ViewGroup,
    receipts: List<Contact>,
    val callback: (Contact) -> Unit,
) : AbsViewBinder<Contact, ItemReceiptBinding>(container, receipts) {

    override fun initializeBinding() = ItemReceiptBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: Contact) {
        val binding = ItemReceiptBinding.bind(container.getChildAt(position))
        container[position].apply {
            binding.name.text = model.name
            binding.avatar.text = model.name.shorten()
            setOnClickListener { callback(model) }
        }
    }

}