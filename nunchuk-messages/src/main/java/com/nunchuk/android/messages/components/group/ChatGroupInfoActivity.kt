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

package com.nunchuk.android.messages.components.group

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.bindRoomWallet
import com.nunchuk.android.messages.components.group.ChatGroupInfoEvent.*
import com.nunchuk.android.messages.components.group.ChatGroupInfoOption.*
import com.nunchuk.android.messages.components.group.action.AddMembersBottomSheet
import com.nunchuk.android.messages.components.group.action.EditGroupNameBottomSheet
import com.nunchuk.android.messages.components.group.members.GroupMembersActivity
import com.nunchuk.android.messages.components.list.getMembersCount
import com.nunchuk.android.messages.databinding.ActivityGroupChatInfoBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatGroupInfoActivity : BaseActivity<ActivityGroupChatInfoBinding>() {

    private val viewModel: ChatGroupInfoViewModel by viewModels()

    private val args: ChatGroupInfoArgs by lazy { ChatGroupInfoArgs.deserializeFrom(intent) }

    private lateinit var walletBinding: ItemWalletBinding

    override fun initializeBinding() = ActivityGroupChatInfoBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.initialize(args.roomId)
    }

    private fun setupViews() {
        binding.more.setOnClickListener { onMoreSelected() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.joinWallet.setOnClickListener { viewModel.createWalletOrTransaction() }
        binding.membersContainer.setOnClickListener { GroupMembersActivity.start(this, args.roomId) }
        walletBinding = ItemWalletBinding.bind(binding.walletContainer.root)
    }

    private fun openCreateSharedWalletScreen() {
        navigator.openCreateSharedWalletScreen(this)
    }

    private fun openInputAmountScreen(roomId: String, walletId: String, amount: Double) {
        navigator.openInputAmountScreen(
            activityContext = this,
            walletId = walletId,
            availableAmount = amount,
        )
    }

    private fun onMoreSelected() {
        val bottomSheet = ChatGroupInfoBottomSheet.show(fragmentManager = supportFragmentManager)
        bottomSheet.listener = {
            when (it) {
                EDIT -> openEditGroupName()
                ADD -> openAddMembers()
                LEAVE -> viewModel.handleLeaveGroup()
            }
        }
    }

    private fun openAddMembers() {
        AddMembersBottomSheet.show(supportFragmentManager, args.roomId)
    }

    private fun openEditGroupName() {
        val bottomSheet = EditGroupNameBottomSheet.show(
            fragmentManager = supportFragmentManager,
            signerName = binding.name.text.toString()
        )
        bottomSheet.setListener(viewModel::handleEditName)
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: ChatGroupInfoState) {
        state.summary?.let {
            binding.name.text = it.name
            val count = it.getMembersCount()
            binding.membersCountTop.text = resources.getQuantityString(R.plurals.nc_message_members, count, count)
            binding.members.text = "Members ($count)"
            binding.badge.text = "$count"
        }
        state.wallet?.let { wallet ->
            walletBinding.root.isVisible = true
            walletBinding.bindRoomWallet(wallet)
            walletBinding.root.setOnClickListener { openWalletDetailsScreen(wallet.id) }
            binding.joinWalletContainer.isVisible = false
            binding.createTransactionContainer.isVisible = true
        } ?: run {
            binding.joinWalletContainer.isVisible = true && !args.isByzantineChat && args.isShowCollaborativeWallet
            binding.createTransactionContainer.isVisible = false
            walletBinding.root.isVisible = false
        }
    }

    private fun openWalletDetailsScreen(id: String) {
        navigator.openWalletDetailsScreen(this, id)
    }

    private fun handleEvent(event: ChatGroupInfoEvent) {
        when (event) {
            RoomNotFoundEvent -> NCToastMessage(this).showError(getString(R.string.nc_message_room_not_found))
            is UpdateRoomNameError -> NCToastMessage(this).showError(event.message)
            is UpdateRoomNameSuccess -> updateRoomName(event)
            is LeaveRoomError -> NCToastMessage(this).showError(event.message)
            LeaveRoomSuccess -> navigator.openMainScreen(this)
            CreateSharedWalletEvent -> openCreateSharedWalletScreen()
            is CreateTransactionEvent -> openInputAmountScreen(roomId = event.roomId, walletId = event.walletId, amount = event.availableAmount)
        }
    }

    private fun updateRoomName(event: UpdateRoomNameSuccess) {
        binding.name.text = event.name
        NCToastMessage(this).showMessage(getString(R.string.nc_message_room_name_updated))
    }

    companion object {
        fun start(activityContext: Context, roomId: String, isByzantineChat: Boolean, isShowCollaborativeWallet: Boolean) {
            activityContext.startActivity(ChatGroupInfoArgs(roomId = roomId, isByzantineChat = isByzantineChat, isShowCollaborativeWallet = isShowCollaborativeWallet).buildIntent(activityContext))
        }
    }

}