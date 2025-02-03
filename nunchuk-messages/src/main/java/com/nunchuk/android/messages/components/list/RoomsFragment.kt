/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.messages.components.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.list.RoomsEvent.LoadingEvent
import com.nunchuk.android.messages.databinding.FragmentMessagesBinding
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RoomsFragment : BaseFragment<FragmentMessagesBinding>() {

    private val viewModel: RoomsViewModel by activityViewModels()

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var roomShareViewPool: RoomShareViewPool

    @Inject
    lateinit var sessionHolder: SessionHolder

    private lateinit var adapter: RoomAdapter

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMessagesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    private fun setupViews() {
        adapter = RoomAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            sessionHolder = sessionHolder,
            currentName = accountManager.getAccount().name,
            enterRoom = ::openRoomDetailScreen,
            removeRoom = ::handleRemoveRoom
        )
        binding.recyclerView.setRecycledViewPool(roomShareViewPool.recycledViewPool)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), VERTICAL, false)
        binding.recyclerView.adapter = adapter
        binding.fab.setOnClickListener {
            navigator.openCreateRoomScreen(requireActivity().supportFragmentManager)
        }
        setEmptyState()
        binding.viewStubEmptyState.btnAddContacts.setOnClickListener {
            navigator.openAddContactsScreen(childFragmentManager, viewModel::listenRoomSummaries)
        }
        binding.btnContactSupporter.setOnDebounceClickListener {
            viewModel.getOrCreateSupportRom()
        }
    }

    private fun setEmptyState() {
        binding.viewStubEmptyState.tvEmptyStateDes.text =
            getString(R.string.nc_message_empty_messages)
        binding.viewStubEmptyState.ivContactAdd.setImageResource(R.drawable.ic_messages_new)
    }

    private fun openRoomDetailScreen(summary: RoomMessage) {
        if (summary is RoomMessage.MatrixRoom) openRoomDetailScreen(summary.data.roomId)
        else if (summary is RoomMessage.GroupWalletRoom) {
            navigator.openGroupChatScreen(requireContext(), summary.data.walletId)
        }
    }

    private fun openRoomDetailScreen(roomId: String) {
        navigator.openRoomDetailActivity(requireContext(), roomId)
    }


    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        flowObserver(viewModel.plans) {
            handleShowEmptyState()
        }
    }

    private fun handleState(state: RoomsState) {
        adapter.roomWallets.apply {
            clear()
            addAll(state.roomWallets.map(RoomWallet::roomId))
        }
        adapter.groupChatRooms.apply {
            clear()
            putAll(state.groupChatRooms)
        }
        val visibleRooms = state.rooms.filter {
            it is RoomMessage.GroupWalletRoom ||
                    ((it as? RoomMessage.MatrixRoom)?.data?.shouldShow() == true)
        }
        adapter.submitList(visibleRooms)
        handleShowEmptyState()

        hideLoading()
    }

    private fun handleShowEmptyState() {
        val visibleRooms = viewModel.getVisibleRooms()
        val plans = viewModel.plans.value
        binding.viewStubEmptyState.container.isVisible = visibleRooms.isEmpty() && plans.isEmpty()
        binding.containerEmptyPremiumUser.isVisible =
            visibleRooms.isEmpty() && plans.isNotEmpty()
    }

    private fun handleEvent(event: RoomsEvent) {
        when (event) {
            is LoadingEvent -> showOrHideLoading(event.loading)
            is RoomsEvent.CreateSupportRoomSuccess -> openRoomDetailScreen(event.roomId)
            is RoomsEvent.ShowError -> showError(event.message)
            is RoomsEvent.RemoveRoomSuccess -> {
//                deleteRoom(event.roomSummary)
            }
        }
    }

    private fun handleRemoveRoom(roomMessage: RoomMessage, hasSharedWallet: Boolean) {
        if (roomMessage !is RoomMessage.MatrixRoom) return
        val roomSummary = roomMessage.data
        if (hasSharedWallet) {
            NCWarningDialog(requireActivity())
                .showDialog(
                    message = getString(R.string.nc_warning_delete_shared_wallet),
                    onYesClick = {
                        viewModel.removeRoom(roomSummary)
                        deleteRoom(roomMessage)
                    },
                    onNoClick = {
                        val position = viewModel.getVisibleRooms()
                            .indexOfFirst { it is RoomMessage.MatrixRoom && it.data.roomId == roomSummary.roomId }
                        if (position in 0 until adapter.itemCount) {
                            adapter.notifyItemChanged(position)
                        }
                    }
                )
        } else {
            viewModel.removeRoom(roomSummary)
        }
    }

    private fun deleteRoom(roomSummary: RoomMessage) {
        val newList = viewModel.getVisibleRooms().toMutableList().apply {
            remove(roomSummary)
        }
        adapter.submitList(newList)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getGroupMessageAccount()
    }

    companion object {
        fun newInstance() = RoomsFragment()
    }

}