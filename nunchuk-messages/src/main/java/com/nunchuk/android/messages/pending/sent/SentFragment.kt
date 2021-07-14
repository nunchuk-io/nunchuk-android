package com.nunchuk.android.messages.pending.sent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.messages.databinding.FragmentSentBinding

internal class SentFragment : BaseFragment<FragmentSentBinding>() {

    private val viewModel: SentViewModel by activityViewModels { factory }

    private lateinit var adapter: SentAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentSentBinding.inflate(inflater, container, false)

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

    private fun handleState(state: SentState) {
        adapter.items = state.contacts
    }

    private fun handleEvent(event: SentEvent) {
    }

    private fun setupViews() {
        adapter = SentAdapter(viewModel::handleWithDraw)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }

    companion object {
        fun newInstance() = SentFragment()
    }
}