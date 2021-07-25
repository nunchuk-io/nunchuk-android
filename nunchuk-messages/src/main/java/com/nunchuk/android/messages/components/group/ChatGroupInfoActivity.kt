package com.nunchuk.android.messages.components.group

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.messages.components.group.ChatGroupInfoOption.*
import com.nunchuk.android.messages.databinding.ActivityGroupChatInfoBinding
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
                EDIT -> showToast("Edit")
                ADD -> showToast("Add")
                LEAVE -> showToast("Leave")
            }
        }
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: ChatGroupInfoState) {
        state.summary?.let {
            binding.name.text = it.name
            binding.avatarHolder.text = it.name.shorten()
            val count = it.joinedMembersCount?.or(0)
            binding.membersCountTop.text = "$count Members"
            binding.members.text = "Members ($count)"
        }
    }

    private fun handleEvent(event: ChatGroupInfoEvent) {

    }

    companion object {
        fun start(activityContext: Context, roomId: String) {
            activityContext.startActivity(ChatGroupInfoArgs(roomId = roomId).buildIntent(activityContext))
        }
    }

}