package com.nunchuk.android.transaction.receive.address.unused

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.viewpager.widget.ViewPager
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.FragmentUnusedAddressBinding
import com.nunchuk.android.transaction.receive.address.AddressFragmentArgs
import com.nunchuk.android.utils.TextUtils
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

internal class UnusedAddressFragment : BaseFragment<FragmentUnusedAddressBinding>() {

    @Inject
    lateinit var textUtils: TextUtils

    @Inject
    lateinit var controller: IntentSharingController

    lateinit var adapter: UnusedAddressAdapter

    private val args: AddressFragmentArgs by lazy { AddressFragmentArgs.deserializeFrom(arguments) }

    private val viewModel: UnusedAddressViewModel by activityViewModels { factory }

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
        binding.viewPager.pageMargin = dipToPixels(context, context.resources.getDimension(R.dimen.nc_padding_4)).toInt()
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                showAddresses(position < adapter.items.size)
                binding.addressCount.text = "${binding.viewPager.currentItem + 1}/${adapter.items.size} address"
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
            binding.addressCount.text = "${binding.viewPager.currentItem + 1}/${addresses.size} address"
        }
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

fun dipToPixels(context: Context, dipValue: Float): Float {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
}

