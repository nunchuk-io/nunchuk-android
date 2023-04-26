/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.messages.components.direct

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.bindRoomWallet
import com.nunchuk.android.messages.components.direct.ChatInfoEvent.*
import com.nunchuk.android.messages.databinding.ActivityChatInfoBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatInfoActivity : BaseActivity<ActivityChatInfoBinding>() {

    private val viewModel: ChatInfoViewModel by viewModels()

    private val args: ChatInfoArgs by lazy { ChatInfoArgs.deserializeFrom(intent) }

    private lateinit var walletBinding: ItemWalletBinding

    override fun initializeBinding() = ActivityChatInfoBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.initialize(args.roomId)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.joinWallet.setOnClickListener { viewModel.createWalletOrTransaction() }
        walletBinding = ItemWalletBinding.bind(binding.walletContainer.root)
    }

    private fun openCreateSharedWalletScreen() {
        navigator.openCreateSharedWalletScreen(this)
    }

    private fun openInputAmountScreen(roomId: String, walletId: String, amount: Double) {
        navigator.openInputAmountScreen(
            activityContext = this,
            roomId = roomId,
            walletId = walletId,
            availableAmount = amount,
        )
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: ChatInfoState) {
        binding.joinWalletLabel.isGone = state.isSupportRoom
        binding.joinWalletContainer.isGone = state.isSupportRoom
        binding.avatar.isVisible = state.isSupportRoom
        if (state.isSupportRoom) {
            binding.name.text = getString(R.string.nc_support)
        } else {
            state.contact?.let {
                binding.name.text = it.name
                binding.email.text = it.email
                binding.avatarHolder.text = it.name.shorten()
            }
            state.wallet?.let { wallet ->
                walletBinding.root.isVisible = true
                walletBinding.bindRoomWallet(wallet)
                binding.joinWalletLabel.text =
                    getString(R.string.nc_message_spend_from_shared_wallet)
                binding.joinWallet.setImageResource(R.drawable.ic_spend)
                walletBinding.root.setOnClickListener { openWalletDetailsScreen(wallet.id) }
            } ?: run {
                walletBinding.root.isVisible = false
                binding.joinWalletLabel.text = getString(R.string.nc_message_create_shared_wallet)
                binding.joinWallet.setImageResource(R.drawable.ic_joint_wallet)
            }
        }
    }

    private fun openWalletDetailsScreen(id: String) {
        navigator.openWalletDetailsScreen(this, id)
    }

    private fun handleEvent(event: ChatInfoEvent) {
        when (event) {
            RoomNotFoundEvent -> NCToastMessage(this).showError(getString(R.string.nc_message_room_not_found))
            CreateSharedWalletEvent -> openCreateSharedWalletScreen()
            is CreateTransactionEvent -> openInputAmountScreen(
                roomId = event.roomId,
                walletId = event.walletId,
                amount = event.availableAmount
            )
        }
    }

    companion object {
        fun start(activityContext: Context, roomId: String) {
            activityContext.startActivity(ChatInfoArgs(roomId = roomId).buildIntent(activityContext))
        }
    }

}