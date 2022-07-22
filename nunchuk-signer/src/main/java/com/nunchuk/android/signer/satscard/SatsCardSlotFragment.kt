package com.nunchuk.android.signer.satscard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.constants.Constants
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentSatscardActiveSlotBinding
import com.nunchuk.android.signer.satscard.wallets.SelectWalletFragment
import com.nunchuk.android.type.SatsCardSlotStatus
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SatsCardSlotFragment : BaseFragment<FragmentSatscardActiveSlotBinding>(), BottomSheetOptionListener {
    @Inject
    lateinit var textUtils: TextUtils

    private val viewModel by viewModels<SatsCardSlotViewModel>()
    private val args: SatsCardArgs by lazy { SatsCardArgs.deserializeBundle(requireArguments()) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSatscardActiveSlotBinding {
        return FragmentSatscardActiveSlotBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        registerEvents()
        initViews()
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_VIEW_SATSCARD_UNSEAL) {
            val action = SatsCardSlotFragmentDirections.actionSatsCardSlotFragmentToSatsCardUnsealSlotFragment(
                slots = viewModel.getUnsealSlots().toTypedArray()
            )
            findNavController().navigate(action)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        binding.toolbar.menu.findItem(R.id.menu_more).isVisible = viewModel.getUnsealSlots().isNotEmpty()
        val activeSlot = viewModel.getActiveSlot() ?: return
        binding.tvSlot.text = "${getString(R.string.nc_slot)} ${args.status.activeSlotIndex.inc()}/${args.status.numberOfSlot}"
        binding.address.text = activeSlot.address
        binding.qrCode.setImageBitmap(activeSlot.address.orEmpty().convertToQRCode())
        binding.tvCardId.text = "${getString(R.string.nc_card_id)}: ${args.status.ident}"
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
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
            viewModel.getActiveSlot()?.let { activeSlot ->
                openSelectWallet(arrayOf(activeSlot), SelectWalletFragment.TYPE_SWEEP_UNSEAL_SLOT)
            }
        }
    }

    private fun openSelectWallet(slots: Array<SatsCardSlot>, type: Int) {
        val action = SatsCardSlotFragmentDirections.actionSatsCardSlotFragmentToSelectWalletFragment(
            slots,
            type
        )
        findNavController().navigate(action)
    }

    private fun showMore() {
        val fragment = BottomSheetOption.newInstance(
            listOf(
                SheetOption(SheetOptionType.TYPE_VIEW_SATSCARD_UNSEAL, stringId = R.string.nc_view_unsealed_slots)
            )
        )
        fragment.show(childFragmentManager, "BottomSheetOption")
    }

    private fun observer() {
        flowObserver {
            viewModel.event.collect {
                when (it) {
                    SatsCardSlotEvent.Loading -> handleLoading()
                    is SatsCardSlotEvent.GetActiveSlotBalanceSuccess -> handleShowBalanceActiveSlot(it.slot)
                    is SatsCardSlotEvent.GetOtherSlotBalanceSuccess -> handleCheckBalanceOtherSlots(it.slots)
                    is SatsCardSlotEvent.ShowError -> handleShowError(it)
                }
            }
        }
    }

    private fun handleShowError(it: SatsCardSlotEvent.ShowError) {
        val message = it.e?.message.orEmpty()
        if (message.isNotEmpty()) {
            NCToastMessage(requireActivity()).showError(it.e?.message.orEmpty())
        }
    }

    private fun handleCheckBalanceOtherSlots(slots: List<SatsCardSlot>) {
        val sum = slots.sumOf { it.balance.value }
        if (sum > 0) {
            val unsealSlowWithBalances = slots.filter { it.status == SatsCardSlotStatus.SEALED && it.balance.value > 0 }
            val labels = unsealSlowWithBalances.joinToString(separator = ", ") { "#${it.index.inc()}" }
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_text_info),
                message = getString(R.string.nc_detect_unseal_has_balance, Amount(value = sum).getBTCAmount(), labels),
                btnNo = getString(R.string.nc_not_now),
                onYesClick = {
                    openSelectWallet(unsealSlowWithBalances.toTypedArray(), SelectWalletFragment.TYPE_UNSEAL_SWEEP_ACTIVE_SLOT)
                }
            )
        }
    }

    private fun copyAddress(address: String) {
        textUtils.copyText(text = address)
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_address_copy_to_clipboard))
    }

    private fun handleShowBalanceActiveSlot(slot: SatsCardSlot) {
        binding.tvBalanceBtc.text = slot.balance.getBTCAmount()
        if (slot.isConfirmed) {
            binding.tvBalanceUsd.text = slot.balance.getUSDAmount()
        } else {
            binding.tvBalanceUsd.text = "(${getString(R.string.nc_unconfirmed)})"
        }
    }

    private fun handleLoading() {
        binding.tvBalanceBtc.text = getString(R.string.checking_balance)
        binding.tvBalanceUsd.text = getString(R.string.nc_please_wait)
    }
}