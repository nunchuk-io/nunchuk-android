package com.nunchuk.android.messages.contact

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.nunchuk.android.messages.R
import com.nunchuk.android.utils.EmailValidator

class EmailsViewBinder(
    private val container: ViewGroup,
    private val emails: List<String>,
    val onItemSelectedListener: (String) -> Unit,
) {

    private val context: Context = container.context
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val layoutId: Int = R.layout.item_email

    fun bindItems() {
        val size = emails.size
        val childCount = container.childCount
        if (childCount == 0 || childCount < size) {
            addChildViews(container, size - childCount)
        } else if (childCount > size) {
            container.removeViews(0, childCount - size)
        }
        for (i in 0 until size) {
            bindItem(i, emails[i])
        }
    }

    private fun bindItem(position: Int, model: String) {
        container[position].apply {
            val textView = findViewById<TextView>(R.id.email)
            textView.text = model
            if (EmailValidator.valid(model)) {
                background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_green_background)
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_outline_24, 0, R.drawable.ic_close, 0)
            } else {
                background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_red_background)
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_outline_24, 0, R.drawable.ic_close, 0)
            }
            setOnClickListener { onItemSelectedListener(model) }
        }
    }

    private fun addChildViews(container: ViewGroup, size: Int) {
        for (i in 0 until size) {
            container.addView(inflater.inflate(layoutId, container, false))
        }
    }

}