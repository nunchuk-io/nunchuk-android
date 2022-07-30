package com.nunchuk.android.widget

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.widget.databinding.NcInfoLoadingDialogBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class NCLoadingDialogCreator(val activity: AppCompatActivity) {
    val dialog = Dialog(activity)
    val binding = NcInfoLoadingDialogBinding.inflate(LayoutInflater.from(activity))
    var showJob: Job? = null

    init {
        dialog.apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.attributes?.windowAnimations = R.style.NCAnimatedDialog
            window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        }
    }

    fun showDialog(cancelable: Boolean = true, title: String?, message: String?) {
        showJob?.cancel()
        showJob = activity.lifecycleScope.launch {
            delay(150)
            ensureActive()
            binding.title.text = title
            binding.message.isGone = message.isNullOrEmpty()
            binding.message.text = message
            dialog.setCancelable(cancelable)
            dialog.show()
        }
    }

    fun cancel() {
        showJob?.cancel()
        dialog.cancel()
    }
}
