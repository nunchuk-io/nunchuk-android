package com.nunchuk.android.transaction.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheetDialogFragment
import com.nunchuk.android.transaction.databinding.DialogTransactionBottomSheetBinding

class TransactionDetailsBottomSheet : BaseBottomSheetDialogFragment<DialogTransactionBottomSheetBinding>() {

    private lateinit var listener: () -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogTransactionBottomSheetBinding {
        return DialogTransactionBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.btnCancel.setOnClickListener {
            listener()
            dismiss()
        }
    }

    fun setListener(listener: () -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "TransactionDetailsBottomSheet"

        fun show(fragmentManager: FragmentManager) = TransactionDetailsBottomSheet().apply { show(fragmentManager, TAG) }
    }
}