package com.nunchuk.android.messages.components.group.members

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.messages.components.group.members.GroupMembersEvent.RoomNotFoundEvent
import com.nunchuk.android.messages.databinding.ActivityGroupMembersBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class GroupMembersActivity : BaseActivity<ActivityGroupMembersBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: GroupMembersViewModel by viewModels { factory }

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
