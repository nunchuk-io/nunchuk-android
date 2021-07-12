package com.nunchuk.android.main.components.tabs.chat.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.main.databinding.FragmentMessagesBinding
import org.matrix.android.sdk.api.session.room.model.RoomSummary

internal class MessagesFragment : BaseFragment<FragmentMessagesBinding>() {

    private val viewModel: MessagesViewModel by activityViewModels { factory }

    private lateinit var adapter: MessagesAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMessagesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        observeEvent()
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveMessages()
    }

    private fun setupViews() {
        adapter = MessagesAdapter(::openRoomDetailScreen)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), VERTICAL, false)
        binding.recyclerView.adapter = adapter
        binding.fab.setOnClickListener {
            navigator.openCreateRoomScreen(requireActivity().supportFragmentManager)
        }
    }

    private fun openRoomDetailScreen(summary: RoomSummary) {
        navigator.openRoomDetailActivity(requireContext(), summary.roomId)
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleState(state: MessagesState) {
        adapter.items = state.rooms
    }

    private fun handleEvent(event: MessagesEvent) {

    }

    companion object {
        fun newInstance() = MessagesFragment()
    }

}