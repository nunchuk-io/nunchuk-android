package com.nunchuk.android.transaction.components.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.checkCameraPermission
import com.nunchuk.android.transaction.components.details.TransactionOption.EXPORT
import com.nunchuk.android.transaction.components.details.TransactionOption.IMPORT
import com.nunchuk.android.transaction.databinding.DialogTransactionSignBottomSheetBinding

class TransactionSignBottomSheet : BaseBottomSheet<DialogTransactionSignBottomSheetBinding>() {

    private lateinit var listener: (TransactionOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogTransactionSignBottomSheetBinding {
        return DialogTransactionSignBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.btnExport.setOnClickListener {
            listener(EXPORT)
            dismiss()
        }

        binding.btnImport.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(IMPORT)
                dismiss()
            }
        }
    }

    fun setListener(listener: (TransactionOption) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "TransactionSignBottomSheet"

        fun show(fragmentManager: FragmentManager) = TransactionSignBottomSheet().apply { show(fragmentManager, TAG) }
    }

}