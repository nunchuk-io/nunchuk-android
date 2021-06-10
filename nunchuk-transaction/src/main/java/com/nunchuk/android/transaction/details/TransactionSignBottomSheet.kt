package com.nunchuk.android.transaction.details

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.transaction.databinding.DialogTransactionSignBottomSheetBinding
import com.nunchuk.android.transaction.details.TransactionOption.EXPORT
import com.nunchuk.android.transaction.details.TransactionOption.IMPORT
import com.nunchuk.android.widget.util.expandDialog

class TransactionSignBottomSheet : BottomSheetDialogFragment() {

    private lateinit var listener: (TransactionOption) -> Unit

    private var _binding: DialogTransactionSignBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogTransactionSignBottomSheetBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener(DialogInterface::expandDialog)
        return binding.root
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
        private const val TAG = "UpdateSignerBottomSheet"

        fun show(fragmentManager: FragmentManager) = TransactionSignBottomSheet().apply { show(fragmentManager, TAG) }
    }
}