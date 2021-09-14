package com.nunchuk.android.messages.components.direct

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.databinding.ItemWalletBinding
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.messages.components.detail.bindRoomWallet
import com.nunchuk.android.messages.databinding.ActivityChatInfoBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ChatInfoActivity : BaseActivity<ActivityChatInfoBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: ChatInfoViewModel by viewModels { factory }

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
        binding.joinWallet.setOnClickListener { navigator.openCreateSharedWalletScreen(this) }
        walletBinding = ItemWalletBinding.bind(binding.walletContainer.root)
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: ChatInfoState) {
        state.contact?.let {
            binding.name.text = it.name
            binding.email.text = it.email
            binding.avatarHolder.text = it.name.shorten()
        }
        state.roomWallet?.let {
            walletBinding.root.isVisible = true
            walletBinding.bindRoomWallet(it)
        }
    }

    private fun handleEvent(event: ChatInfoEvent) {
    }

    companion object {
        fun start(activityContext: Context, roomId: String) {
            activityContext.startActivity(ChatInfoArgs(roomId = roomId).buildIntent(activityContext))
        }
    }

}