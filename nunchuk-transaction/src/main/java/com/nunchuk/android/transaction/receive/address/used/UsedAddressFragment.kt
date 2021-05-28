package com.nunchuk.android.transaction.receive.address.used

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.transaction.databinding.FragmentUsedAddressBinding
import com.nunchuk.android.transaction.receive.address.AddressFragmentArgs
import com.nunchuk.android.transaction.receive.address.used.UsedAddressEvent.GetUsedAddressErrorEvent
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

internal class UsedAddressFragment : BaseFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: AddressFragmentArgs by lazy { AddressFragmentArgs.deserializeFrom(arguments) }

    private val viewModel: UsedAddressViewModel by lazy {
        ViewModelProviders.of(requireActivity(), factory).get(UsedAddressViewModel::class.java)
    }

    private lateinit var adapter: UsedAddressAdapter

    private var _binding: FragmentUsedAddressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUsedAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

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
                balance = it.balance.getBTCAmount()
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