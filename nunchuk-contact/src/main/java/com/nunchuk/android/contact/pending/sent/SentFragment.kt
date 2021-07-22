package com.nunchuk.android.contact.pending.sent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.contact.databinding.FragmentSentBinding
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.model.SentContact

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
        if (event is SentEvent.LoadingEvent) {
            if (event.loading) {
                showLoading()
            } else {
                hideLoading()
            }
        }
    }

    private fun setupViews() {
        adapter = SentAdapter(::handleWithdrawEvent)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }

    private fun handleWithdrawEvent(contact: SentContact) {
        viewModel.handleWithDraw(contact)
    }

    companion object {
        fun newInstance() = SentFragment()
    }
}