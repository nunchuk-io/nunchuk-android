package com.nunchuk.android.transaction.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheetDialogFragment
import com.nunchuk.android.transaction.databinding.DialogTransactionSignBottomSheetBinding
import com.nunchuk.android.transaction.details.TransactionOption.EXPORT
import com.nunchuk.android.transaction.details.TransactionOption.IMPORT

class TransactionSignBottomSheet : BaseBottomSheetDialogFragment<DialogTransactionSignBottomSheetBinding>() {

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
            listener(IMPORT)
            dismiss()
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