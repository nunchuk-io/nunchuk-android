package com.nunchuk.android.messages.components.room.create

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.get
import com.nunchuk.android.messages.R
import com.nunchuk.android.model.Contact

class ReceiptsViewBinder(
    private val container: ViewGroup,
    private val receipts: List<Contact>,
    val callback: (Contact) -> Unit,
) {

    private val context: Context = container.context
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val layoutId: Int = R.layout.item_email

    fun bindItems() {
        val size = receipts.size
        val childCount = container.childCount
        if (childCount == 0 || childCount < size) {
            addChildViews(container, size - childCount)
        } else if (childCount > size) {
            container.removeViews(0, childCount - size)
        }
        for (i in 0 until size) {
            bindItem(i, receipts[i])
        }
    }

    private fun bindItem(position: Int, receipt: Contact) {
        container[position].apply {
            val textView = findViewById<TextView>(R.id.email)
            textView.text = receipt.name
            setOnClickListener { callback(receipt) }
        }
    }

    private fun addChildViews(container: ViewGroup, size: Int) {
        for (i in 0 until size) {
            container.addView(inflater.inflate(layoutId, container, false))
        }
    }

}