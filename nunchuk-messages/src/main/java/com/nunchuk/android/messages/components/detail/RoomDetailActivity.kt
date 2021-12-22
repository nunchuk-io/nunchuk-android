package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.hideKeyboard
import com.nunchuk.android.core.util.observable
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.databinding.ActivityRoomDetailBinding
import com.nunchuk.android.messages.databinding.ViewWalletStickyBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnEnterListener
import com.nunchuk.android.widget.util.smoothScrollToLastItem
import javax.inject.Inject

class RoomDetailActivity : BaseActivity<ActivityRoomDetailBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    private val viewModel: RoomDetailViewModel by viewModels { factory }

    private val args: RoomDetailArgs by lazy { RoomDetailArgs.deserializeFrom(intent) }

    private var adapter: MessagesAdapter? = null
    private lateinit var stickyBinding: ViewWalletStickyBinding
    private var selectMessageActionView: View? = null

    private var selectMode: Boolean by observable(false, {
        setupViewForSelectMode(it)
    })

    override fun initializeBinding() = ActivityRoomDetailBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.initialize(args.roomId)
        viewModel.checkShowBannerNewChat()
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
        binding.toolbarTitle.text = state.roomInfo.roomName
        val membersCount = "${state.roomInfo.memberCount} members"
        binding.memberCount.text = membersCount

        adapter?.update(state.messages.groupByDate(), state.transactions, state.roomWallet, state.roomInfo.memberCount)
        if (state.messages.isNotEmpty()) {
            binding.recyclerView.scrollToPosition((adapter?.itemCount ?: 0) - 1)
        }
        stickyBinding.root.isVisible = state.roomWallet != null
        state.roomWallet?.let {
            stickyBinding.bindRoomWallet(it, viewModel::viewConfig)
        }
    }

    private fun handleEvent(event: RoomDetailEvent) {
        when (event) {
            RoomNotFoundEvent -> finishWithMessage(getString(R.string.nc_message_room_not_found))
            ContactNotFoundEvent -> finishWithMessage(getString(R.string.nc_message_contact_not_found))
            CreateNewSharedWallet -> navigator.openCreateSharedWalletScreen(this)
            is CreateNewTransaction -> navigator.openInputAmountScreen(
                activityContext = this,
                roomId = event.roomId,
                walletId = event.walletId,
                availableAmount = event.availableAmount
            )
            OpenChatGroupInfoEvent -> navigator.openChatGroupInfoScreen(this, args.roomId)
            OpenChatInfoEvent -> navigator.openChatInfoScreen(this, args.roomId)
            RoomWalletCreatedEvent -> NCToastMessage(this).show(R.string.nc_message_wallet_created)
            DontShowBannerNewChatEvent -> adapter?.removeBannerNewChat()
            is ViewWalletConfigEvent -> navigator.openSharedWalletConfigScreen(this, event.roomWalletData)
        }
    }

    private fun finishWithMessage(message: String) {
        NCToastMessage(this).showError(message)
        finish()
    }

    private fun setupViews() {
        selectMessageActionView = binding.viewStubSelectMessageAction.inflate()
        selectMessageActionView?.findViewById<ImageView>(R.id.btnCopy)?.setOnClickListener {
            copyMessageText( adapter?.getSelectedMessage()?.joinToString("\n") { it.content }.orEmpty())
            selectMode = false
        }
        selectMode = false
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
            viewWalletConfig = viewModel::viewConfig,
            finalizeWallet = viewModel::finalizeWallet,
            viewTransaction = ::openTransactionDetails,
            dismissBannerNewChatListener = { viewModel.dontShowBannerNewChat()},
            createSharedWalletListener = {viewModel.handleAddEvent()},
            senderLongPressListener = { message, position ->
                showSelectMessageBottomSheet(message, position)
            },
            countCheckedChangeListener = {
                binding.tvSelectedMessageCount.text = getString(R.string.nc_text_count_selected_message, it)
            }
        )
        binding.recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager

        binding.toolbar.setNavigationOnClickListener {
            if (selectMode) {
                selectMode = false
            } else {
                finish()
            }
        }
        binding.toolbar.setOnClickListener {
            viewModel.handleTitleClick()
        }
        binding.add.setOnClickListener {
            viewModel.handleAddEvent()
        }

        binding.recyclerView.smoothScrollToLastItem()
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (RecyclerView.SCROLL_STATE_DRAGGING == newState) {
                    binding.recyclerView.hideKeyboard()
                }

                if (!recyclerView.isLastItemVisible()) {
                    viewModel.handleLoadMore()
                }
            }
        })
    }

    private fun showSelectMessageBottomSheet(message: Message, position: Int) {
        val bottomSheet = EditPhotoUserBottomSheet.show(
            fragmentManager = this.supportFragmentManager
        )
        bottomSheet.listener = {
            when (it) {
                SelectMessageOption.Select -> {
                    selectMode = true
                    adapter?.updateSelectedPosition(
                        selectedPosition = position,
                        checked = true,
                        refreshList = false
                    )
                }

                SelectMessageOption.Copy -> {
                    copyMessageText(message.content)
                    selectMode = false
                }

            }
        }
    }

    private fun copyMessageText(text: String) {
        this.copyToClipboard(
            label = "Nunchuk",
            text = text
        )
        NCToastMessage(this).showMessage(getString(R.string.nc_text_copied_to_clipboard))
    }

    private fun setupViewForSelectMode(selectMode: Boolean) {
        adapter?.selectMode = selectMode
        selectMessageActionView?.isVisible = selectMode
        binding.tvSelectedMessageCount.isVisible = selectMode
        binding.editText.isVisible = !selectMode
        binding.add.isVisible = !selectMode
        binding.send.isVisible = !selectMode
        binding.memberCount.isVisible = !selectMode
        binding.toolbarTitle.isVisible = !selectMode
        binding.toolbar.navigationIcon = if (selectMode) {
            ContextCompat.getDrawable(this, R.drawable.ic_close)
        } else {
            ContextCompat.getDrawable(this, R.drawable.ic_back)
        }
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
        viewModel.cleanUp()
        super.onDestroy()
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
