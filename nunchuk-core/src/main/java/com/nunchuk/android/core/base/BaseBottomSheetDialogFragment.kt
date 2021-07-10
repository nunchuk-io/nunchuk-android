package com.nunchuk.android.core.base

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.widget.util.expandDialog

abstract class BaseBottomSheetDialogFragment<out Binding : ViewBinding> : BottomSheetDialogFragment() {

    private var _binding: Binding? = null

    protected val binding get() = _binding!!

    abstract fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): Binding

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = initializeBinding(inflater, container)
        dialog?.setOnShowListener(DialogInterface::expandDialog)
        return binding.root
    }

}