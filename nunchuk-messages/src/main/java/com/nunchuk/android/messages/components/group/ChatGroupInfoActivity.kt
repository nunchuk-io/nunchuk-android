package com.nunchuk.android.messages.components.group

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.messages.components.group.ChatGroupInfoEvent.*
import com.nunchuk.android.messages.components.group.ChatGroupInfoOption.*
import com.nunchuk.android.messages.components.group.action.AddMembersBottomSheet
import com.nunchuk.android.messages.components.group.action.EditGroupNameBottomSheet
import com.nunchuk.android.messages.databinding.ActivityGroupChatInfoBinding
import com.nunchuk.android.messages.util.getMembersCount
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ChatGroupInfoActivity : BaseActivity<ActivityGroupChatInfoBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: ChatGroupInfoViewModel by viewModels { factory }

    private val args: ChatGroupInfoArgs by lazy { ChatGroupInfoArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityGroupChatInfoBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.initialize(args.roomId)
    }

    private fun setupViews() {
        binding.more.setOnClickListener {
            onMoreSelected()
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
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
            binding.membersCountTop.text = "$count Members"
            binding.members.text = "Members ($count)"
            binding.badge.text = "$count"
        }
    }

    private fun handleEvent(event: ChatGroupInfoEvent) {
        when (event) {
            RoomNotFoundEvent -> NCToastMessage(this).showError("Room not found")
            is UpdateRoomNameError -> NCToastMessage(this).showError(event.message)
            is UpdateRoomNameSuccess -> updateRoomName(event)
            is LeaveRoomError -> NCToastMessage(this).showError(event.message)
            LeaveRoomSuccess -> navigator.openMainScreen(this)
        }
    }

    private fun updateRoomName(event: UpdateRoomNameSuccess) {
        binding.name.text = event.name
        NCToastMessage(this).showMessage("Room name has been updated")
    }

    companion object {
        fun start(activityContext: Context, roomId: String) {
            activityContext.startActivity(ChatGroupInfoArgs(roomId = roomId).buildIntent(activityContext))
        }
    }

}