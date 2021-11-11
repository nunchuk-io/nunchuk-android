package com.nunchuk.android.messages.components.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.messages.components.list.RoomsEvent.LoadingEvent
import com.nunchuk.android.messages.databinding.FragmentMessagesBinding
import com.nunchuk.android.messages.util.shouldShow
import com.nunchuk.android.model.RoomWallet
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.initsync.InitialSyncProgressService
import org.matrix.android.sdk.api.session.initsync.InitSyncStep
import javax.inject.Inject
import timber.log.Timber

class RoomsFragment : BaseFragment<FragmentMessagesBinding>() {

    private val viewModel: RoomsViewModel by viewModels { factory }

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
        observeInitialMatrixSync()
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

    private fun observeInitialMatrixSync() {
        SessionHolder.activeSession?.getInitialSyncProgressStatus()?.observe(viewLifecycleOwner, Observer {
            Timber.d("Matrix sync status, ${it}")
            when (val status = it) {
                is InitialSyncProgressService.Status.Progressing -> {
                    if (status.initSyncStep == InitSyncStep.ImportingAccount && status.percentProgress == 100) {
                        viewModel.retrieveMessages()
                    }
                }
            }
        })
    }

    private fun handleState(state: RoomsState) {
        Timber.d("HUGOLOG handleState, state.rooms.isEmpty() = ${state.rooms.isEmpty()}")
        adapter.roomWallets = state.roomWallets.map(RoomWallet::roomId)
        adapter.roomSummaries = state.rooms.filter(RoomSummary::shouldShow)
        binding.skeletonContainer.root.isVisible = state.rooms.isEmpty()
        hideLoading()
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