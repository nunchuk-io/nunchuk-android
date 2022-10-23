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
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.share.model.TransactionOption.*
import com.nunchuk.android.transaction.databinding.DialogTransactionSignBottomSheetBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener

class TransactionOptionsBottomSheet : BaseBottomSheet<DialogTransactionSignBottomSheetBinding>() {

    private lateinit var listener: (TransactionOption) -> Unit

    private val args: TransactionOptionsArgs by lazy {
        TransactionOptionsArgs.deserializeFrom(
            arguments
        )
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogTransactionSignBottomSheetBinding {
        return DialogTransactionSignBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.btnCancel.isVisible = args.isPending
        binding.btnCancel.setOnClickListener {
            listener(CANCEL)
            dismiss()
        }
        binding.btnExportPSBT.isVisible = args.isPending
        binding.btnExportPSBT.setOnClickListener {
            listener(EXPORT_PSBT)
            dismiss()
        }

        binding.btnExport.isVisible = args.isPending
        binding.btnExport.setOnClickListener {
            listener(EXPORT)
            dismiss()
        }

        binding.btnExportPassport.isVisible = args.isPending
        binding.btnExportPassport.setOnClickListener {
            listener(EXPORT_PASSPORT)
            dismiss()
        }

        binding.btnImport.isVisible = args.isPending
        binding.btnImport.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(IMPORT_KEYSTONE)
                dismiss()
            }
        }

        binding.btnImportPassport.isVisible = args.isPending
        binding.btnImportPassport.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(IMPORT_PASSPORT)
                dismiss()
            }
        }

        binding.btnReplaceFee.isVisible = args.isPendingConfirm
        binding.btnReplaceFee.setOnClickListener {
            if (requireActivity().checkCameraPermission()) {
                listener(REPLACE_BY_FEE)
                dismiss()
            }
        }

        binding.btnCopyTxId.setOnDebounceClickListener {
            listener(COPY_TRANSACTION_ID)
            dismiss()
        }

        binding.btnRemoveTransaction.isVisible = args.isRejected
        binding.btnRemoveTransaction.setOnDebounceClickListener {
            listener(REMOVE_TRANSACTION)
            dismiss()
        }
    }

    fun setListener(listener: (TransactionOption) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "TransactionOptionsBottomSheet"

        private fun newInstance(
            isPending: Boolean,
            isPendingConfirm: Boolean,
            isRejected: Boolean
        ) = TransactionOptionsBottomSheet().apply {
            arguments =
                TransactionOptionsArgs(isPending, isPendingConfirm, isRejected).buildBundle()
        }


        fun show(
            fragmentManager: FragmentManager,
            isPending: Boolean,
            isPendingConfirm: Boolean,
            isRejected: Boolean
        ): TransactionOptionsBottomSheet {
            return newInstance(
                isPending,
                isPendingConfirm,
                isRejected
            ).apply { show(fragmentManager, TAG) }
        }
    }

}

data class TransactionOptionsArgs(
    val isPending: Boolean,
    val isPendingConfirm: Boolean,
    val isRejected: Boolean
) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putBoolean(EXTRA_IS_PENDING, isPending)
        putBoolean(EXTRA_IS_PENDING_CONFIRM, isPendingConfirm)
        putBoolean(EXTRA_IS_REJECTED, isRejected)
    }

    companion object {
        private const val EXTRA_IS_PENDING = "EXTRA_IS_PENDING"
        private const val EXTRA_IS_PENDING_CONFIRM = "EXTRA_IS_PENDING_CONFIRM"
        private const val EXTRA_IS_REJECTED = "EXTRA_IS_REJECTED"

        fun deserializeFrom(data: Bundle?) = TransactionOptionsArgs(
            data?.getBooleanValue(EXTRA_IS_PENDING).orFalse(),
            data?.getBooleanValue(EXTRA_IS_PENDING_CONFIRM).orFalse(),
            data?.getBooleanValue(EXTRA_IS_REJECTED).orFalse(),
        )
    }
}
