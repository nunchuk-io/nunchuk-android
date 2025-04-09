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

package com.nunchuk.android.signer.satscard.unseal

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentUnsealSlotBinding
import com.nunchuk.android.signer.satscard.SatsCardSlotViewModel
import com.nunchuk.android.signer.satscard.unSealBalanceSlots
import com.nunchuk.android.signer.util.openSweepRecipeScreen
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SatsCardUnsealSlotFragment : BaseFragment<FragmentUnsealSlotBinding>(), BottomSheetOptionListener {
    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        SatsCardUnsealSlotAdapter {
            val action = SatsCardUnsealSlotFragmentDirections.actionSatsCardUnsealSlotFragmentToSatsCardSlotQrFragment(it)
            findNavController().navigate(action)
        }
    }
    private val args by navArgs<SatsCardUnsealSlotFragmentArgs>()
    private val viewModel by activityViewModels<SatsCardSlotViewModel>()

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            openSelectWallet()
        }
    }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUnsealSlotBinding {
        return FragmentUnsealSlotBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
        observer()
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_SWEEP_TO_WALLET) {
            if (args.hasWallet) {
                openSelectWallet()
            } else {
                navigator.openQuickWalletScreen(
                    activityContext = requireActivity(),
                    quickWalletParam = QuickWalletParam(
                        slots = viewModel.getUnsealSlots().unSealBalanceSlots()
                    )
                )
            }
        } else if (option.type == SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS) {
            val slots = viewModel.getUnsealSlots().unSealBalanceSlots()
            openSweepRecipeScreen(navigator, slots, false)
        }
    }

    private fun observer() {
        flowObserver(viewModel.state) {
            val unsealSlots = viewModel.getUnsealSlots()
            adapter.submitList(unsealSlots)
            binding.btnSweep.isVisible = viewModel.isHasUnsealSlotBalance()
            showOrHideLoading(it.isLoading)
        }
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.recyclerView.adapter = adapter
        binding.btnSweep.setOnDebounceClickListener(coroutineScope = viewLifecycleOwner.lifecycleScope) {
            showSweepOptions()
        }
    }

    private fun openSelectWallet() {
        navigator.openSelectWalletScreen(
            activityContext = requireActivity(),
            slots = viewModel.getUnsealSlots().unSealBalanceSlots(),
            type = SelectWalletType.TYPE_SWEEP_UNSEAL_SLOT,
        )
    }

    private fun showSweepOptions() {
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(SheetOptionType.TYPE_SWEEP_TO_WALLET, R.drawable.ic_wallet_info, R.string.nc_sweep_to_a_wallet),
                SheetOption(SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS, R.drawable.ic_sending_bitcoin, R.string.nc_withdraw_to_an_address),
            )
        )
        dialog.show(childFragmentManager, "BottomSheetOption")
    }
}