package com.nunchuk.android.transaction.details

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.transaction.databinding.DialogTransactionBottomSheetBinding
import com.nunchuk.android.widget.util.expandDialog

class TransactionDetailsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var listener: () -> Unit

    private var _binding: DialogTransactionBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogTransactionBottomSheetBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener(DialogInterface::expandDialog)
        return binding.root
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
        private const val TAG = "UpdateSignerBottomSheet"

        fun show(fragmentManager: FragmentManager) = TransactionDetailsBottomSheet().apply { show(fragmentManager, TAG) }
    }
}