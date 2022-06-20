package com.nunchuk.android.contact.components.pending

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.contact.databinding.BottomSheetPendingContactsBinding
import com.nunchuk.android.core.base.BaseBottomSheet
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PendingContactsBottomSheet : BaseBottomSheet<BottomSheetPendingContactsBinding>() {

    private lateinit var pagerAdapter: PendingContactsPagerAdapter

    var listener: () -> Unit = {}

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetPendingContactsBinding {
        return BottomSheetPendingContactsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener()
    }

    private fun setupViews() {
        val pagers = binding.pagers
        val tabs = binding.tabs
        pagerAdapter = PendingContactsPagerAdapter(requireContext(), fragmentManager = childFragmentManager)
        binding.pagers.offscreenPageLimit = PendingContactTab.values().size
        PendingContactTab.values().forEach {
            tabs.addTab(tabs.newTab().setText(it.name))
        }
        val position = pagers.currentItem
        pagers.adapter = pagerAdapter
        tabs.setupWithViewPager(pagers)
        pagers.currentItem = position

        binding.closeBtn.setOnClickListener {
            listener()
            dismiss()
        }
    }

    companion object {
        private const val TAG = "PendingContactsBottomSheet"
        fun show(fragmentManager: FragmentManager) = PendingContactsBottomSheet().apply {
            show(fragmentManager, TAG)
        }
    }

}
