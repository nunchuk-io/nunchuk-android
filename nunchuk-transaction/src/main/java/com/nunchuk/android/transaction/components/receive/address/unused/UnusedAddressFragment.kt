/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.transaction.components.receive.address.unused

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.ViewPager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.TextUtils
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.receive.address.AddressFragmentArgs
import com.nunchuk.android.transaction.databinding.FragmentUnusedAddressBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class UnusedAddressFragment : BaseFragment<FragmentUnusedAddressBinding>() {

    @Inject
    lateinit var textUtils: TextUtils

    private val controller: IntentSharingController by lazy { IntentSharingController.from(requireActivity()) }

    lateinit var adapter: UnusedAddressAdapter

    private val args: AddressFragmentArgs by lazy { AddressFragmentArgs.deserializeFrom(arguments) }

    private val viewModel: UnusedAddressViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentUnusedAddressBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observeEvent()
        viewModel.init(args.walletId)
    }

    private fun initViews() {
        val context = requireContext()
        adapter = UnusedAddressAdapter(context, ::handleItemClicked)
        binding.viewPager.adapter = adapter
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                val size = adapter.items.size
                showAddresses(position < size)
                bindCount(size)
            }
        })
        binding.btnCopy.setOnClickListener {
            handleCopyAddress()
        }

        binding.btnShare.setOnClickListener {
            handleShareAddress()
        }
    }

    private fun handleItemClicked(address: String?) {
        if (address.isNullOrBlank()) {
            viewModel.generateAddress()
        } else {
            copyAddress(address)
        }
    }

    private fun copyAddress(address: String) {
        textUtils.copyText(text = address)
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_copy_to_clipboard))
    }

    private fun handleShareAddress() {
        getCurrentAddress()?.let(controller::shareText)
    }

    private fun handleCopyAddress() {
        getCurrentAddress()?.let(::copyAddress)
    }

    private fun getCurrentAddress() = if (adapter.items.isNotEmpty()) adapter.items[binding.viewPager.currentItem] else null

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: UnusedAddressState) {
        bindAddresses(state.addresses)
    }

    private fun bindAddresses(addresses: List<String>) {
        adapter.items = addresses
        val hasUnusedAddresses = addresses.isNotEmpty()
        showAddresses(hasUnusedAddresses)
        if (hasUnusedAddresses) {
            val size = addresses.size
            bindCount(size)
        }
    }

    private fun bindCount(size: Int) {
        val current = binding.viewPager.currentItem + 1
        val count = "$current/$size address"
        binding.addressCount.text = count
    }

    private fun showAddresses(hasUnusedAddresses: Boolean) {
        binding.addressCount.isVisible = hasUnusedAddresses
        binding.btnShare.isVisible = hasUnusedAddresses
        binding.btnCopy.isVisible = hasUnusedAddresses
        binding.more.isVisible = hasUnusedAddresses
    }

    private fun handleEvent(event: UnusedAddressEvent) {

    }

    companion object {

        fun newInstance(walletId: String) = UnusedAddressFragment().apply {
            arguments = AddressFragmentArgs(walletId = walletId).buildBundle()
        }
    }

}
