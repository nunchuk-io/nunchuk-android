package com.nunchuk.android.core.nfc

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import androidx.core.view.isVisible
import com.nunchuk.android.core.databinding.NfcScanDialogBinding
import javax.inject.Inject

class NfcScanDialog @Inject constructor(activity: Activity) : Dialog(activity) {
    val binding = NfcScanDialogBinding.inflate(LayoutInflater.from(activity))

    init {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

    fun update(message: String, hint: String = "") {
        binding.hint.isVisible = hint.isNotEmpty()
        binding.hint.text = hint
        binding.message.text = message
    }
}