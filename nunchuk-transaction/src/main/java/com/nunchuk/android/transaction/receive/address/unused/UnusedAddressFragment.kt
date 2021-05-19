package com.nunchuk.android.transaction.receive.address.unused

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.FragmentUnusedAddressBinding
import com.nunchuk.android.transaction.receive.address.AddressFragmentArgs
import dagger.android.support.DaggerFragment
import javax.inject.Inject

internal class UnusedAddressFragment : DaggerFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    lateinit var adapter: UnusedAddressAdapter

    private val args: AddressFragmentArgs by lazy { AddressFragmentArgs.deserializeFrom(arguments) }

    private val viewModel: UnusedAddressViewModel by lazy {
        ViewModelProviders.of(requireActivity(), factory).get(UnusedAddressViewModel::class.java)
    }

    private var _binding: FragmentUnusedAddressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUnusedAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observeEvent()
        viewModel.init(args.walletId)
    }

    private fun initViews() {
        val context = requireContext()
        adapter = UnusedAddressAdapter(context)
        binding.viewPager.adapter = adapter
        binding.viewPager.pageMargin = dipToPixels(context, context.resources.getDimension(R.dimen.nc_padding_4)).toInt()
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                binding.addressCount.text = "${binding.viewPager.currentItem + 1}/${adapter.items.size} address"
            }
        })
    }

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
        binding.addressCount.isVisible = hasUnusedAddresses
        binding.btnShare.isVisible = hasUnusedAddresses
        binding.btnCopy.isVisible = hasUnusedAddresses
        if (hasUnusedAddresses) {
            binding.addressCount.text = "${binding.viewPager.currentItem + 1}/${addresses.size} address"
        }
    }

    private fun handleEvent(event: UnusedAddressEvent) {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

