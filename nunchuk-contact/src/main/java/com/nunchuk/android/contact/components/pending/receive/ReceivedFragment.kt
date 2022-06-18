package com.nunchuk.android.contact.components.pending.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.contact.databinding.FragmentReceivedBinding
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ReceivedFragment : BaseFragment<FragmentReceivedBinding>() {

    private val viewModel: ReceivedViewModel by viewModels()

    private lateinit var adapter: ReceivedAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentReceivedBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        observeEvent()
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveData()
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleState(state: ReceivedState) {
        adapter.items = state.contacts
        binding.skeletonContainer.root.isVisible = state.contacts.isEmpty()
    }

    private fun handleEvent(event: ReceivedEvent) {
        if (event is ReceivedEvent.LoadingEvent) {
            if (event.loading) {
                showLoading()
            } else {
                hideLoading()
            }
        }
    }

    private fun setupViews() {
        adapter = ReceivedAdapter(viewModel::handleAcceptRequest, viewModel::handleCancelRequest)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance() = ReceivedFragment()
    }
}