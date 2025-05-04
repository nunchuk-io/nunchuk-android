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

package com.nunchuk.android.signer.satscard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.constants.Constants
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.TextUtils
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showWarning
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentSatscardActiveSlotBinding
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.util.openSweepRecipeScreen
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SatsCardSlotFragment : BaseFragment<FragmentSatscardActiveSlotBinding>(),
    BottomSheetOptionListener {
    @Inject
    lateinit var textUtils: TextUtils

    private val viewModel by activityViewModels<SatsCardSlotViewModel>()
    private val args: SatsCardArgs by lazy { SatsCardArgs.deserializeBundle(requireArguments()) }

    private var isSweepActiveSlot: Boolean = true

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSatscardActiveSlotBinding {
        return FragmentSatscardActiveSlotBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isSweepActiveSlot = savedInstanceState?.getBoolean(EXTRA_IS_SWEEP_ACTIVE_SLOT, true) != false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        registerEvents()
        initViews()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(EXTRA_IS_SWEEP_ACTIVE_SLOT, isSweepActiveSlot)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_SATSCARD_SKIP_SLOT -> {
                NfcSetupActivity.navigate(
                    activity = requireActivity(),
                    setUpAction = NfcSetupActivity.SETUP_SATSCARD,
                    hasWallet = args.hasWallet,
                    slot = viewModel.getActiveSlot()
                )
            }

            SheetOptionType.TYPE_VIEW_SATSCARD_UNSEAL -> {
                val action =
                    SatsCardSlotFragmentDirections.actionSatsCardSlotFragmentToSatsCardUnsealSlotFragment(
                        args.hasWallet
                    )
                findNavController().navigate(action)
            }

            SheetOptionType.TYPE_SWEEP_TO_WALLET -> {
                if (args.hasWallet) {
                    openSelectWallet(getInteractSlots().toTypedArray())
                } else {
                    navigator.openQuickWalletScreen(
                        activityContext = requireActivity(),
                        quickWalletParam = QuickWalletParam(
                            slots = getInteractSlots(),
                            type = getSelectWalletType()
                        )
                    )
                }
            }

            SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS -> {
                openSweepRecipeScreen(navigator, getInteractSlots(), isSweepActiveSlot)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        val activeSlot = viewModel.getActiveSlot() ?: return
        binding.tvSlot.text =
            "${getString(R.string.nc_slot)} ${args.status.activeSlotIndex.inc()}/${args.status.numberOfSlot}"
        binding.address.text = activeSlot.address
        binding.qrCode.setImageBitmap(activeSlot.address.orEmpty().convertToQRCode())
        binding.tvCardId.text = "${getString(R.string.nc_card_id)}: ${args.status.ident}"
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_more -> {
                    showMore()
                    true
                }

                else -> false
            }
        }
        binding.btnCopy.setOnClickListener {
            viewModel.getActiveSlot()?.let { activeSlot ->
                copyAddress(activeSlot.address.orEmpty())
            }
        }
        binding.btnViewOnExplore.setOnClickListener {
            viewModel.getActiveSlot()?.let { activeSlot ->
                requireActivity().openExternalLink(Constants.BLOCKSTREAM_MAINNET_ADDRESS_TEMPLATE + activeSlot.address)
            }
        }
        binding.btnUnsealAndSweep.setOnClickListener {
            isSweepActiveSlot = true
            val activeSlot = viewModel.getActiveSlot() ?: return@setOnClickListener
            if (activeSlot.isConfirmed.not()) {
                showWarning(getString(R.string.nc_please_wait_balance_confirmation))
            } else {
                showSweepOptions()
            }
        }
    }

    private fun showSweepOptions() {
        (childFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_WALLET,
                    R.drawable.ic_wallet_info,
                    R.string.nc_sweep_to_a_wallet
                ),
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS,
                    R.drawable.ic_sending_bitcoin,
                    R.string.nc_withdraw_to_an_address
                ),
            )
        )
        dialog.show(childFragmentManager, "BottomSheetOption")
    }

    private fun openSelectWallet(slots: Array<SatsCardSlot>) {
        navigator.openSelectWalletScreen(
            activityContext = requireActivity(),
            slots = slots.toList(),
            type = getSelectWalletType(),
        )
    }

    private fun getSelectWalletType(): Int {
        val type =
            if (isSweepActiveSlot) SelectWalletType.TYPE_UNSEAL_SWEEP_ACTIVE_SLOT else SelectWalletType.TYPE_SWEEP_UNSEAL_SLOT
        return type
    }

    private fun showMore() {
        val options = mutableListOf<SheetOption>()
        if (viewModel.getUnsealSlots().isNotEmpty()) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_VIEW_SATSCARD_UNSEAL,
                    stringId = R.string.nc_view_unsealed_slots
                )
            )
        }
        if (viewModel.state.value.status.activeSlotIndex == 0) {
            options.add(
                SheetOption(
                    SheetOptionType.TYPE_SATSCARD_SKIP_SLOT,
                    stringId = R.string.nc_skip_first_slot
                )
            )
        }
        val fragment = BottomSheetOption.newInstance(
            options = options
        )
        fragment.show(childFragmentManager, "BottomSheetOption")
    }

    private fun observer() {
        flowObserver(viewModel.event, ::handleEvent)
        flowObserver(viewModel.state) {
            binding.toolbar.menu.findItem(R.id.menu_more).isVisible =
                viewModel.getUnsealSlots().isNotEmpty() || it.status.activeSlotIndex == 0
            if (it.isLoading) {
                handleLoading()
            } else if (it.isNetworkError) {
                handleNetworkError()
            } else if (it.isSuccess) {
                handleShowBalanceActiveSlot(viewModel.getActiveSlot() ?: return@flowObserver)
            }
        }
    }

    private fun handleEvent(it: SatsCardSlotEvent) {
        when (it) {
            is SatsCardSlotEvent.GetOtherSlotBalanceSuccess -> handleCheckBalanceOtherSlots(it.slots)
            is SatsCardSlotEvent.ShowError -> handleShowError(it)
        }
    }

    private fun handleShowError(it: SatsCardSlotEvent.ShowError) {
        val message = it.e?.message.orEmpty()
        if (message.isNotEmpty()) {
            NCToastMessage(requireActivity()).showError(it.e?.message.orEmpty())
        }
    }

    private fun handleNetworkError() {
        binding.tvBalanceBtc.text = getString(R.string.checking_balance)
        binding.tvBalanceUsd.apply {
            text = getString(R.string.nc_no_internet_connection)
            setTextColor(ContextCompat.getColor(requireActivity(), R.color.nc_orange_color))
        }
    }

    private fun handleCheckBalanceOtherSlots(slots: List<SatsCardSlot>) {
        val balanceSlots = slots.unSealBalanceSlots()
        val sum = balanceSlots.sumOf { it.balance.value }
        if (sum > 0) {
            val labels = balanceSlots.joinToString(separator = ", ") { "#${it.index.inc()}" }
            val message = if (balanceSlots.size > 1) {
                getString(
                    R.string.nc_detect_unseal_slots_has_balance,
                    Amount(value = sum).getBTCAmount(),
                    labels
                )
            } else {
                getString(
                    R.string.nc_detect_unseal_slot_has_balance,
                    Amount(value = sum).getBTCAmount(),
                    labels
                )
            }
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_text_info),
                message = message,
                btnNo = getString(R.string.nc_not_now),
                onYesClick = {
                    isSweepActiveSlot = false
                    showSweepOptions()
                }
            )
        }
    }

    private fun getInteractSlots(): List<SatsCardSlot> {
        return if (isSweepActiveSlot) {
            listOf(viewModel.getActiveSlot() ?: SatsCardSlot())
        } else {
            viewModel.getUnsealSlots().unSealBalanceSlots()
        }
    }

    private fun copyAddress(address: String) {
        textUtils.copyText(text = address)
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_copy_to_clipboard))
    }

    private fun handleShowBalanceActiveSlot(slot: SatsCardSlot) {
        binding.btnUnsealAndSweep.isEnabled = slot.balance.value > 0L
        binding.tvBalanceBtc.text = slot.balance.getBTCAmount()
        if (slot.isConfirmed) {
            binding.tvBalanceUsd.text = slot.balance.getCurrencyAmount()
        } else {
            binding.tvBalanceUsd.text = "(${getString(R.string.nc_unconfirmed)})"
        }
    }

    private fun handleLoading() {
        binding.tvBalanceBtc.text = getString(R.string.checking_balance)
        binding.tvBalanceUsd.text = getString(R.string.nc_please_wait)
    }

    companion object {
        private const val EXTRA_IS_SWEEP_ACTIVE_SLOT = "_a"
    }
}