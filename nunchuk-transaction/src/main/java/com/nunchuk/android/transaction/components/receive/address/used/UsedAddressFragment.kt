/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.transaction.components.receive.address.used

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.transaction.components.receive.TabCountChangeListener
import com.nunchuk.android.transaction.components.receive.address.AddressFragmentArgs
import com.nunchuk.android.transaction.components.receive.address.AddressTab
import com.nunchuk.android.transaction.components.receive.address.used.UsedAddressEvent.GetUsedAddressErrorEvent
import com.nunchuk.android.transaction.databinding.FragmentUsedAddressBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class UsedAddressFragment : BaseFragment<FragmentUsedAddressBinding>() {

    private val args: AddressFragmentArgs by lazy { AddressFragmentArgs.deserializeFrom(arguments) }

    private val viewModel: UsedAddressViewModel by viewModels()

    private lateinit var adapter: UsedAddressAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentUsedAddressBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observeEvent()
        viewModel.init(args.walletId)
    }

    private fun initViews() {
        adapter = UsedAddressAdapter {
            navigator.openAddressDetailsScreen(
                activityContext = requireActivity(),
                address = it.address,
                balance = it.balance.getBTCAmount(),
                walletId = args.walletId
            )
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: UsedAddressState) {
        adapter.items = state.addresses
        (requireActivity() as TabCountChangeListener).onChange(AddressTab.USED, state.addresses.size)
    }

    private fun handleEvent(event: UsedAddressEvent) {
        when (event) {
            is GetUsedAddressErrorEvent -> activity?.let { NCToastMessage(it).showError(event.message) }
        }
    }

    companion object {

        fun newInstance(walletId: String) = UsedAddressFragment().apply {
            arguments = AddressFragmentArgs(walletId = walletId).buildBundle()
        }

    }

}