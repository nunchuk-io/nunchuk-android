package com.nunchuk.android.transaction.components.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.checkCameraPermission
import com.nunchuk.android.core.util.checkReadExternalPermission
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.transaction.components.details.TransactionOption.*
import com.nunchuk.android.transaction.databinding.DialogTransactionSignBottomSheetBinding

class TransactionOptionsBottomSheet : BaseBottomSheet<DialogTransactionSignBottomSheetBinding>() {

    private lateinit var listener: (TransactionOption) -> Unit

    private val args: TransactionOptionsArgs by lazy { TransactionOptionsArgs.deserializeFrom(arguments) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogTransactionSignBottomSheetBinding {
        return DialogTransactionSignBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.btnCancel.isVisible = args.shouldShowCancel

        binding.btnCancel.setOnClickListener {
            if (requireActivity().checkReadExternalPermission()) {
                listener(CANCEL)
                dismiss()
            }
        }
        binding.btnExport.setOnClickListener {
            listener(EXPORT)
            dismiss()
        }

        binding.btnExportPassport.setOnClickListener {
            listener(EXPORT_PASSPORT)
            dismiss()
        }

        binding.btnImport.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(IMPORT)
                dismiss()
            }
        }

        binding.btnImportPassport.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(IMPORT_PASSPORT)
                dismiss()
            }
        }
    }

    fun setListener(listener: (TransactionOption) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "TransactionOptionsBottomSheet"

        private fun newInstance(shouldShowCancel: Boolean) = TransactionOptionsBottomSheet().apply {
            arguments = TransactionOptionsArgs(shouldShowCancel).buildBundle()
        }


        fun show(fragmentManager: FragmentManager, shouldShowCancel: Boolean): TransactionOptionsBottomSheet {
            return newInstance(shouldShowCancel).apply { show(fragmentManager, TAG) }
        }
    }

}

data class TransactionOptionsArgs(val shouldShowCancel: Boolean) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putBoolean(EXTRA_SHOW_CANCEL, shouldShowCancel)
    }

    companion object {
        private const val EXTRA_SHOW_CANCEL = "EXTRA_SHOW_CANCEL"

        fun deserializeFrom(data: Bundle?) = TransactionOptionsArgs(
            data?.getBooleanValue(EXTRA_SHOW_CANCEL).orFalse()
        )
    }
}
