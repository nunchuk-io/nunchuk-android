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
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.TextUtils
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.transaction.components.receive.TabCountChangeListener
import com.nunchuk.android.transaction.components.receive.address.AddressFragmentArgs
import com.nunchuk.android.transaction.components.receive.address.AddressTab
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class UsedAddressFragment : BaseFragment<ViewBinding>() {

    @Inject
    lateinit var textUtils: TextUtils

    private val args: AddressFragmentArgs by lazy { AddressFragmentArgs.deserializeFrom(arguments) }

    private val viewModel: UsedAddressViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): ViewBinding = ViewBinding {
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()

                NunchukTheme {
                    UsedAddressContent(
                        addresses = state.addresses,
                        onAddressClick = { model ->
                            navigator.openAddressDetailsScreen(
                                activityContext = requireActivity(),
                                address = model.address,
                                balance = model.balance.getBTCAmount(),
                                walletId = args.walletId,
                            )
                        },
                        onCopyAddress = { address ->
                            textUtils.copyText(text = address)
                            NCToastMessage(requireActivity()).showMessage(
                                getString(com.nunchuk.android.core.R.string.nc_address_copy_to_clipboard)
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvent()
        viewModel.init(args.walletId)
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is UsedAddressEvent.GetUsedAddressErrorEvent -> {
                    activity?.let { NCToastMessage(it).showError(event.message) }
                }
            }
        }

        flowObserver(viewModel.state) { state ->
            (requireActivity() as TabCountChangeListener).onChange(
                AddressTab.USED,
                state.addresses.size,
            )
        }
    }

    companion object {
        fun newInstance(walletId: String) = UsedAddressFragment().apply {
            arguments = AddressFragmentArgs(walletId = walletId).buildBundle()
        }
    }
}
