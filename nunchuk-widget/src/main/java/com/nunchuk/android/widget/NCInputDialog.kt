package com.nunchuk.android.widget

import android.app.Dialog
import android.app.ProgressDialog.show
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import com.nunchuk.android.widget.databinding.NcConfirmDialogBinding
import javax.inject.Inject

class NCInputDialog @Inject constructor(private val context: Context) {

    fun showDialog(
        title: String,
        onConfirmed: (String) -> Unit = {},
        onCanceled: () -> Unit = {},
        isMaskedInput: Boolean = false
    ) = Dialog(context).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        val binding = NcConfirmDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        binding.title.text = title
        binding.btnYes.setOnClickListener {
            onConfirmed(binding.message.text.toString())
            dismiss()
        }

        binding.btnNo.setOnClickListener {
            onCanceled()
            dismiss()
        }
        if (isMaskedInput) {
            binding.message.makeMaskedInput()
        }
        show()
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }
}