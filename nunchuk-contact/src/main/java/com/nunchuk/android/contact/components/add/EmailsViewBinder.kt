package com.nunchuk.android.contact.components.add

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.databinding.ItemEmailBinding
import com.nunchuk.android.widget.util.AbsViewBinder

class EmailsViewBinder(
    container: ViewGroup,
    emails: List<EmailWithState>,
    val callback: (EmailWithState) -> Unit,
) : AbsViewBinder<EmailWithState, ItemEmailBinding>(container, emails) {

    override fun initializeBinding() = ItemEmailBinding.inflate(inflater, container, false)

    override fun bindItem(position: Int, model: EmailWithState) {
        val binding = ItemEmailBinding.bind(container.getChildAt(position))
        binding.email.text = model.email
        if (model.valid) {
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_green_background)
            binding.email.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle_outline_24, 0, R.drawable.ic_close, 0)
        } else {
            binding.root.background = ContextCompat.getDrawable(context, R.drawable.nc_rounded_red_background)
            binding.email.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_error_outline_24, 0, R.drawable.ic_close, 0)
        }
        binding.root.setOnClickListener { callback(model) }
    }

}