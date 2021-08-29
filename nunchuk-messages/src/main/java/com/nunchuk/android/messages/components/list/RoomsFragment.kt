package com.nunchuk.android.messages.components.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.messages.components.list.RoomsEvent.LoadingEvent
import com.nunchuk.android.messages.databinding.FragmentMessagesBinding
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import javax.inject.Inject

class RoomsFragment : BaseFragment<FragmentMessagesBinding>() {

    private val viewModel: RoomsViewModel by activityViewModels { factory }

    @Inject
    lateinit var accountManager: AccountManager

    private lateinit var adapter: RoomAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMessagesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        observeEvent()
        viewModel.retrieveMessages()
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveMessages()
    }

    private fun setupViews() {
        adapter = RoomAdapter(accountManager.getAccount().name, ::openRoomDetailScreen, viewModel::removeRoom)
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

    private fun handleState(state: RoomsState) {
        adapter.items = state.rooms
    }

    private fun handleEvent(event: RoomsEvent) {
        when (event) {
            is LoadingEvent -> showOrHideLoading(event.loading)
        }
    }

    companion object {
        fun newInstance() = RoomsFragment()
    }

}