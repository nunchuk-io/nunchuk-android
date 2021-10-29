package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.databinding.ActivityRoomDetailBinding
import com.nunchuk.android.messages.databinding.ViewWalletStickyBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnEnterListener
import com.nunchuk.android.widget.util.smoothScrollToLastItem
import timber.log.Timber
import javax.inject.Inject

class RoomDetailActivity : BaseActivity<ActivityRoomDetailBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    private val viewModel: RoomDetailViewModel by viewModels { factory }

    private val args: RoomDetailArgs by lazy { RoomDetailArgs.deserializeFrom(intent) }

    private lateinit var adapter: MessagesAdapter
    private lateinit var stickyBinding: ViewWalletStickyBinding

    override fun initializeBinding() = ActivityRoomDetailBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.initialize(args.roomId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveTimelineEvents()
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: RoomDetailState) {
        Timber.d("transactions:: ${state.transactions}")
        binding.toolbarTitle.text = state.roomInfo.roomName
        val membersCount = "${state.roomInfo.memberCount} members"
        binding.memberCount.text = membersCount
        adapter.update(state.messages.groupByDate(), state.transactions, state.roomWallet)
        if (state.messages.isNotEmpty()) {
            binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
        stickyBinding.root.isVisible = state.roomWallet != null
        state.roomWallet?.let { stickyBinding.bindRoomWallet(it, viewModel::viewConfig) }
    }

    private fun handleEvent(event: RoomDetailEvent) {
        when (event) {
            RoomNotFoundEvent -> finishWithMessage("Room not found!")
            ContactNotFoundEvent -> finishWithMessage("Contact not found!")
            OpenChatGroupInfoEvent -> navigator.openChatGroupInfoScreen(this, args.roomId)
            OpenChatInfoEvent -> navigator.openChatInfoScreen(this, args.roomId)
            RoomWalletCreatedEvent -> NCToastMessage(this).show(R.string.nc_message_wallet_created)
            is ViewWalletConfigEvent -> navigator.openSharedWalletConfigScreen(this, event.roomWalletData)
            is Loading -> showOrHideLoading(event.loading)
        }
    }

    private fun finishWithMessage(message: String) {
        NCToastMessage(this).showError(message)
        finish()
    }

    private fun setupViews() {
        stickyBinding = ViewWalletStickyBinding.bind(binding.walletStickyContainer.root)
        stickyBinding.root.setOnClickListener { }

        binding.send.setOnClickListener { sendMessage() }
        binding.editText.setOnEnterListener(::sendMessage)
        binding.editText.addTextChangedCallback {
            enableButton(it.isNotEmpty())
        }

        adapter = MessagesAdapter(
            context = this,
            imageLoader = imageLoader,
            cancelWallet = viewModel::cancelWallet,
            denyWallet = viewModel::denyWallet,
            viewConfig = viewModel::viewConfig,
            finalizeWallet = viewModel::finalizeWallet,
            viewDetails = ::openTransactionDetails
        )
        binding.recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.setOnClickListener {
            viewModel.handleTitleClick()
        }
        binding.add.setOnClickListener {
            navigator.openCreateSharedWalletScreen(this)
        }

        binding.recyclerView.smoothScrollToLastItem()
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (!recyclerView.isLastItemVisible()) {
                    viewModel.handleLoadMore()
                }
            }
        })
    }

    private fun openTransactionDetails(walletId: String, txId: String, initEventId: String) {
        navigator.openTransactionDetailsScreen(
            activityContext = this,
            walletId = walletId,
            txId = txId,
            initEventId = initEventId
        )
    }

    private fun enableButton(isEnabled: Boolean) {
        binding.send.isEnabled = isEnabled
        binding.send.isClickable = isEnabled
        if (isEnabled) {
            binding.send.setImageResource(R.drawable.ic_send)
        } else {
            binding.send.setImageResource(R.drawable.ic_send_disabled)
        }
    }

    private fun sendMessage() {
        val content = binding.editText.text.toString()
        if (content.trim().isNotBlank()) {
            viewModel.handleSendMessage(content)
            runOnUiThread {
                binding.editText.setText("")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanUp()
    }

    companion object {
        fun start(activityContext: Context, roomId: String) {
            activityContext.startActivity(RoomDetailArgs(roomId = roomId).buildIntent(activityContext))
        }
    }
}

private fun RecyclerView.isLastItemVisible(): Boolean {
    val adapter = adapter ?: return false
    if (adapter.itemCount != 0) {
        val linearLayoutManager = layoutManager as LinearLayoutManager
        val lastVisibleItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
        if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == adapter.itemCount - 1) return true
    }
    return false
}
