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

package com.nunchuk.android.messages.components.group.members

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.messages.components.group.members.GroupMembersEvent.RoomNotFoundEvent
import com.nunchuk.android.messages.databinding.ActivityGroupMembersBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupMembersActivity : BaseActivity<ActivityGroupMembersBinding>() {

    private val viewModel: GroupMembersViewModel by viewModels()

    private val args: GroupMembersArgs by lazy { GroupMembersArgs.deserializeFrom(intent) }

    private lateinit var adapter: GroupMembersAdapter

    override fun initializeBinding() = ActivityGroupMembersBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.initialize(args.roomId)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        adapter = GroupMembersAdapter()

        binding.recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.recyclerView.adapter = adapter
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: GroupMembersState) {
        adapter.items = state.roomMembers
    }

    private fun handleEvent(event: GroupMembersEvent) {
        when (event) {
            RoomNotFoundEvent -> finish()
        }
    }

    companion object {
        fun start(activityContext: Context, roomId: String) {
            activityContext.startActivity(GroupMembersArgs(roomId = roomId).buildIntent(activityContext))
        }
    }

}
