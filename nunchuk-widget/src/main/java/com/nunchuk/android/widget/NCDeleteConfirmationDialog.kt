package com.nunchuk.android.widget

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import com.nunchuk.android.widget.databinding.NcDeleteConfirmDialogBinding
import javax.inject.Inject

class NCDeleteConfirmationDialog @Inject constructor(private val context: Context) {

    fun showDialog(
        title: String = context.getString(R.string.nc_confirmation),
        message: String = context.getString(R.string.nc_delete_account),
        onConfirmed: (String) -> Unit = {},
        onCanceled: () -> Unit = {}
    ) {
        Dialog(context).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            val binding = NcDeleteConfirmDialogBinding.inflate(LayoutInflater.from(context))
            setContentView(binding.root)
            binding.title.text = title
            binding.message.text = message
            binding.btnYes.setOnClickListener {
                onConfirmed(binding.input.text.toString())
                dismiss()
            }

            binding.btnNo.setOnClickListener {
                onCanceled()
                dismiss()
            }
            show()
            window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        }
    }
}