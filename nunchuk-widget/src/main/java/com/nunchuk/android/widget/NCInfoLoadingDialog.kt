package com.nunchuk.android.widget

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import com.nunchuk.android.widget.databinding.NcInfoDialogBinding
import javax.inject.Inject

class NCInfoLoadingDialog @Inject constructor(
    private val activity: Activity
) {
    fun init(
        title: String = activity.getString(R.string.nc_please_wait),
        message: String,
        btnText: String = activity.getString(R.string.nc_text_cancel),
        onBtnClick: () -> Unit = {},
        cancelable: Boolean = false
    ) = Dialog(activity).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(cancelable)
        setCanceledOnTouchOutside(cancelable)
        val binding = NcInfoDialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.title.text = title
        binding.btnYes.text = btnText
        binding.message.text = message
        binding.btnYes.setOnClickListener {
            onBtnClick()
            dismiss()
        }
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }
}