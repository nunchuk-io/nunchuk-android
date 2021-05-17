package com.nunchuk.android.transaction.receive.address.unused

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.qr.convertToQRCode
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.databinding.FragmentUnusedAddressBinding
import com.nunchuk.android.transaction.receive.address.AddressFragmentArgs
import dagger.android.support.DaggerFragment
import javax.inject.Inject

internal class UnusedAddressFragment : DaggerFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    lateinit var adapter: AddressPagerAdapter

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
        adapter = AddressPagerAdapter(context)
        binding.viewPager.adapter = adapter
        binding.viewPager.pageMargin = dipToPixels(context, context.resources.getDimension(R.dimen.nc_padding_4)).toInt()
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                binding.addressCount.text = "${binding.viewPager.currentItem + 1}/${adapter.addresses.size} address"
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
        adapter.addresses = addresses
        if (addresses.isNotEmpty()) {
            binding.addressCount.isVisible = true
            binding.addressCount.text = "${binding.viewPager.currentItem + 1}/${addresses.size} address"
        } else {
            binding.addressCount.isVisible = false
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

class AddressPagerAdapter(private val context: Context) : PagerAdapter() {

    internal var addresses = emptyList<String>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getCount(): Int {
        return addresses.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = layoutInflater.inflate(R.layout.item_address_carousel, null)

        val imageView: ImageView = view.findViewById(R.id.qrCode)
        val textView: TextView = view.findViewById(R.id.address)
        val address = addresses[position]
        imageView.setImageBitmap(address.convertToQRCode())
        textView.text = address

        val vp = container as ViewPager
        vp.addView(view, 0)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val vp = container as ViewPager
        val view = `object` as View
        vp.removeView(view)
    }

}