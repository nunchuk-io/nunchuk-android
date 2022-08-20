package com.nunchuk.android.widget

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import androidx.core.view.isVisible
import com.nunchuk.android.widget.databinding.NcWarningDialogVeritcalBinding
import javax.inject.Inject

class NCWarningVerticalDialog @Inject constructor(
    private val activity: Activity
) {

    fun showDialog(
        title: String = activity.getString(R.string.nc_text_warning),
        message: String,
        btnYes: String = activity.getString(R.string.nc_text_yes),
        btnNo: String = activity.getString(R.string.nc_text_no),
        btnNeutral: String = "",
        onYesClick: () -> Unit = {},
        onNoClick: () -> Unit = {},
        onNeutralClick: () -> Unit = {}
    ) = Dialog(activity).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        val binding = NcWarningDialogVeritcalBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.title.text = title
        binding.btnYes.text = btnYes
        binding.btnNo.text = btnNo
        binding.message.text = message
        binding.btnNeutral.text = btnNeutral
        binding.btnNeutral.isVisible = btnNeutral.isNotEmpty()
        binding.btnYes.setOnClickListener {
            onYesClick()
            dismiss()
        }

        binding.btnNo.setOnClickListener {
            onNoClick()
            dismiss()
        }
        binding.btnNeutral.setOnClickListener {
            onNeutralClick()
            dismiss()
        }
        show()
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

}