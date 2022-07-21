package com.nunchuk.android.signer.satscard.unseal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.databinding.FragmentUnsealSlotBinding

class SatsCardUnsealSlotFragment : BaseFragment<FragmentUnsealSlotBinding>() {
    private val args : SatsCardUnsealSlotFragmentArgs by navArgs()
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { SatsCardUnsealSlotAdapter(args.slots.toList()) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUnsealSlotBinding {
        return FragmentUnsealSlotBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.recyclerView.adapter = adapter
    }
}