package com.nunchuk.android.core.sheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.databinding.FragmentSheetOptionBinding

class BottomSheetOption : BaseBottomSheet<FragmentSheetOptionBinding>() {
    private lateinit var listener: BottomSheetOptionListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is BottomSheetOptionListener) {
            context
        } else if (parentFragment is BottomSheetOptionListener) {
            parentFragment as BottomSheetOptionListener
        } else {
            throw IllegalArgumentException("Activity or parent fragment should implement BottomSheetOptionListener")
        }
    }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSheetOptionBinding {
        return FragmentSheetOptionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val options = arguments?.getParcelableArrayList<SheetOption>(EXTRA_OPTIONS).orEmpty()
        binding.recyclerView.adapter = SheetOptionAdapter(options) {
            listener.onOptionClicked(it)
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val EXTRA_OPTIONS = "extra_options"

        fun newInstance(options: List<SheetOption>): BottomSheetOption {
            return BottomSheetOption().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(EXTRA_OPTIONS, ArrayList(options))
                }
            }
        }
    }
}

interface BottomSheetOptionListener {
    fun onOptionClicked(option: SheetOption)
}