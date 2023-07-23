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

package com.nunchuk.android.messages.components.detail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.core.media.NcMediaManager
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.RoomDetailEvent.*
import com.nunchuk.android.messages.databinding.FragmentRoomDetailBinding
import com.nunchuk.android.messages.databinding.ViewWalletStickyBinding
import com.nunchuk.android.messages.util.safeStartActivity
import com.nunchuk.android.model.TransactionExtended
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RoomDetailFragment : BaseCameraFragment<FragmentRoomDetailBinding>(),
    BottomSheetOptionListener {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var ncMediaManager: NcMediaManager

    private var handleRoomAction: Boolean = false

    private val viewModel: RoomDetailViewModel by activityViewModels()

    private val args: RoomDetailFragmentArgs by navArgs()

    private val adapter: MessagesAdapter by lazy(LazyThreadSafetyMode.NONE) {
        MessagesAdapter(
            imageLoader = imageLoader,
            cancelWallet = viewModel::cancelWallet,
            denyWallet = viewModel::denyWallet,
            viewWalletConfig = viewModel::viewConfig,
            finalizeWallet = viewModel::finalizeWallet,
            viewTransaction = ::openTransactionDetails,
            dismissBannerNewChatListener = viewModel::hideBannerNewChat,
            createSharedWalletListener = ::sendBTCAction,
            senderLongPressListener = ::showSelectMessageBottomSheet,
            onMessageRead = viewModel::markMessageRead,
            toggleSelected = viewModel::toggleSelected,
            onOpenMediaViewer = ::onOpenMediaViewer,
            onDownloadOrOpen = ::downloadOrOpen
        )
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.getTransactions()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            currentCaptureUri?.takeIf { isSuccess }?.let { uri ->
                viewModel.sendMedia(listOf(uri))
            }
        }

    private val captureVideoLauncher =
        registerForActivityResult(ActivityResultContracts.CaptureVideo()) { isSuccess ->
            currentCaptureUri?.takeIf { isSuccess }?.let { uri ->
                viewModel.sendMedia(listOf(uri))
            }
        }

    private val selectPhotoAndVideoLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) {
            if (it.isNotEmpty()) {
                viewModel.sendMedia(it)
            }
        }

    private val selectFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            if (it.isNotEmpty()) {
                viewModel.sendMedia(it)
            }
        }
    private lateinit var stickyBinding: ViewWalletStickyBinding

    private var currentCaptureUri: Uri? = null
    private var currentAction: Int = -1

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRoomDetailBinding = FragmentRoomDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            currentCaptureUri = it.parcelable(KEY_CURRENT_PHOTO_PATH)
            currentAction = it.getInt(KEY_CURRENT_ACTION)
        }

        setupViews()
        observeEvent()
        viewModel.initialize(args.roomId)
        viewModel.checkShowBannerNewChat()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_CURRENT_PHOTO_PATH, currentCaptureUri)
        outState.putInt(KEY_CURRENT_ACTION, currentAction)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionClicked(option: SheetOption) {
        currentAction = option.type
        when (option.type) {
            SheetOptionType.CHAT_ACTION_TAKE_PHOTO -> requestCameraPermissionOrExecuteAction()
            SheetOptionType.CHAT_ACTION_CAPTURE_VIDEO -> requestCameraPermissionOrExecuteAction()
            SheetOptionType.CHAT_ACTION_SELECT_PHOTO_VIDEO -> handleSelectPhotoAndVideo()
            SheetOptionType.CHAT_ACTION_SELECT_FILE -> handleSelectFile()
        }
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        when (currentAction) {
            SheetOptionType.CHAT_ACTION_TAKE_PHOTO -> handlePhoto(isTakePhoto = true)
            SheetOptionType.CHAT_ACTION_CAPTURE_VIDEO -> handlePhoto(isTakePhoto = false)
        }
    }

    private fun handleSelectFile() {
        selectFileLauncher.launch(arrayOf("text/*", "application/*"))
    }

    private fun handleSelectPhotoAndVideo() {
        selectPhotoAndVideoLauncher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(
                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                ).build()
        )
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleState(state: RoomDetailState) {
        setupViewForSelectMode(state.isSelectEnable)
        val count = state.roomInfo.memberCount
        if (state.isSupportRoom || args.isGroupChat) {
            adapter.removeBannerNewChat()
            binding.memberCount.text = resources.getString(R.string.nc_message_transaction_view_details)
        } else {
            binding.memberCount.text = resources.getQuantityString(R.plurals.nc_message_members, count, count)
        }
        binding.toolbarTitle.text = state.roomInfo.roomName
        binding.tvSelectedMessageCount.text =
            getString(R.string.nc_text_count_selected_message, state.selectedEventIds.size)

        adapter.update(state.messages.groupByDate(), state.roomWallet, count)
        val hasRoomWallet = state.roomWallet != null
        stickyBinding.root.isVisible = hasRoomWallet
        binding.sendAction.isVisible = state.isSupportRoom || args.isGroupChat
        binding.addWallet.isVisible = !hasRoomWallet && !state.isSupportRoom && !args.isGroupChat
        binding.sendBTC.isVisible = hasRoomWallet
        binding.receiveBTC.isVisible = hasRoomWallet
        binding.expand.isVisible = hasRoomWallet
        expandChatBar()
        state.roomWallet?.let {
            val transactions = state.transactions.map(TransactionExtended::transaction)
            val pendingTransaction = transactions.firstOrNull { it.status.isPending() }
            val walletId = it.walletId
            val coins = pendingTransaction?.outputs?.filter { viewModel.isMyCoin(walletId = walletId, it.first) == pendingTransaction.isReceive }
            stickyBinding.bindRoomWallet(
                wallet = it,
                transactions = state.transactions.map(TransactionExtended::transaction),
                numSendingAddress = coins?.size,
                onClick = viewModel::viewConfig,
                onClickViewTransactionDetail = { txId ->
                    openTransactionDetails(walletId = it.walletId, txId = txId, initEventId = "")
                }
            )
        }
    }

    private fun handleEvent(event: RoomDetailEvent) {
        when (event) {
            RoomNotFoundEvent -> finishWithMessage(getString(R.string.nc_message_room_not_found))
            ContactNotFoundEvent -> finishWithMessage(getString(R.string.nc_message_contact_not_found))
            CreateNewSharedWallet -> navigator.openCreateSharedWalletScreen(requireActivity())
            is CreateNewTransaction -> navigator.openInputAmountScreen(
                activityContext = requireActivity(),
                roomId = event.roomId,
                walletId = event.walletId,
                availableAmount = event.availableAmount,
            )
            OpenChatGroupInfoEvent -> navigator.openChatGroupInfoScreen(
                requireActivity(),
                args.roomId
            )
            OpenChatInfoEvent -> navigator.openChatInfoScreen(requireActivity(), args.roomId)
            RoomWalletCreatedEvent -> showSuccess(getString(R.string.nc_message_wallet_created))
            HideBannerNewChatEvent -> adapter.removeBannerNewChat()
            is ViewWalletConfigEvent -> navigator.openSharedWalletConfigScreen(
                requireActivity(),
                event.roomWalletData
            )
            is ReceiveBTCEvent -> navigator.openReceiveTransactionScreen(
                requireActivity(),
                event.walletId
            )
            HasUpdatedEvent -> scrollToLastItem()
            GetRoomWalletSuccessEvent -> args.roomAction.let(::handleRoomAction)
            LeaveRoomEvent -> requireActivity().finish()
            is ShowError -> showError(event.message)
            is Loading -> showOrHideLoading(event.isLoading)
            None -> Unit
            is OpenFile -> openFile(event)
            OnSendMediaSuccess -> scrollToLastItem()
        }
        viewModel.clearEvent()
    }

    private fun openFile(action: OpenFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(action.uri, action.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        requireActivity().safeStartActivity(intent)
    }

    private fun handlePhoto(isTakePhoto: Boolean) {
        val file = runCatching {
            if (isTakePhoto) ncMediaManager.createImageFile() else ncMediaManager.createVideoFile()
        }.getOrNull()
        file?.let {
            val uri: Uri = FileProvider.getUriForFile(
                requireActivity(),
                "${requireActivity().packageName}.provider",
                it
            )
            currentCaptureUri = uri
            if (isTakePhoto) {
                takePictureLauncher.launch(uri)
            } else {
                captureVideoLauncher.launch(uri)
            }
        }
    }

    private fun scrollToLastItem() {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500L)
            val itemCount = adapter.itemCount
            if (itemCount > 0) {
                binding.recyclerView.smoothScrollToLastItem()
            }
        }
    }

    private fun finishWithMessage(message: String) {
        showError(message)
        requireActivity().finish()
    }

    private fun setupViews() {
        binding.btnCopy.setOnClickListener {
            copyMessageText(
                adapter.getSelectedMessage().joinToString("\n", transform = MatrixMessage::content)
            )
            viewModel.applySelected(false)
        }
        stickyBinding = ViewWalletStickyBinding.bind(binding.walletStickyContainer.root)

        binding.send.setOnClickListener { sendMessage() }
        binding.editText.setOnEnterListener(::sendMessage)
        binding.editText.addTextChangedCallback {
            collapseChatBar()
            enableButton(it.isNotEmpty())
        }
        binding.recyclerView.adapter = adapter

        val layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager

        binding.toolbar.setNavigationOnClickListener {
            if (viewModel.isSelectedEnable()) {
                viewModel.applySelected(false)
            } else {
                requireActivity().finish()
            }
        }
        binding.toolbar.setOnClickListener {
            viewModel.handleTitleClick()
        }
        binding.addWallet.setOnDebounceClickListener {
            sendBTCAction()
        }
        binding.sendAction.setOnDebounceClickListener {
            BottomSheetOption.newInstance(
                listOf(
                    SheetOption(
                        resId = R.drawable.ic_camera,
                        type = SheetOptionType.CHAT_ACTION_TAKE_PHOTO,
                        label = getString(R.string.nc_take_photo)
                    ),
                    SheetOption(
                        resId = R.drawable.ic_camera,
                        type = SheetOptionType.CHAT_ACTION_CAPTURE_VIDEO,
                        label = getString(R.string.nc_capture_video)
                    ),
                    SheetOption(
                        resId = R.drawable.ic_image,
                        type = SheetOptionType.CHAT_ACTION_SELECT_PHOTO_VIDEO,
                        label = getString(R.string.nc_photo_and_video)
                    ),
                    SheetOption(
                        resId = R.drawable.ic_attach_file,
                        type = SheetOptionType.CHAT_ACTION_SELECT_FILE,
                        label = getString(R.string.nc_upload_file)
                    ),
                )
            ).show(childFragmentManager, "BottomSheetOption")
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (RecyclerView.SCROLL_STATE_DRAGGING == newState) {
                    binding.recyclerView.hideKeyboard()
                } else if (RecyclerView.SCROLL_STATE_IDLE == newState) {
                    if (recyclerView.isFirstItemVisible()) {
                        viewModel.handleLoadMore()
                    }
                }
            }
        })

        binding.sendBTC.setOnDebounceClickListener { sendBTCAction() }
        binding.receiveBTC.setOnDebounceClickListener { receiveBTCAction() }
        setupAnimationForChatBar()
    }

    private fun receiveBTCAction() {
        viewModel.handleReceiveEvent()
    }

    private fun sendBTCAction() {
        viewModel.handleAddEvent()
    }

    private fun setupAnimationForChatBar() {
        binding.editText.setOnFocusChangeListener { _, _ ->
            collapseChatBar()
        }

        binding.editText.setOnClickListener {
            collapseChatBar()
        }
        binding.expand.setOnClickListener {
            if (it.isVisible) {
                expandChatBar()
            } else {
                collapseChatBar()
            }
        }
    }

    private fun collapseChatBar() {
        binding.expand.isVisible = viewModel.isSupportRoom.not()
    }

    private fun expandChatBar() {
        binding.expand.isVisible = false
    }

    private fun showSelectMessageBottomSheet(message: Message, position: Int) {
        val bottomSheet = EditPhotoUserBottomSheet.show(
            fragmentManager = this.childFragmentManager
        )
        bottomSheet.listener = {
            when (it) {
                SelectMessageOption.Select -> {
                    viewModel.applySelected(true)
                    adapter.updateSelectedPosition(
                        selectedPosition = position,
                    )
                }

                SelectMessageOption.Copy -> {
                    copyMessageText(message.content)
                    viewModel.applySelected(true)
                }

                SelectMessageOption.Dismiss -> {
                    viewModel.applySelected(true)
                }
            }
        }
    }

    private fun copyMessageText(text: String) {
        requireActivity().copyToClipboard(label = "Nunchuk", text = text)
        showSuccess(getString(R.string.nc_text_copied_to_clipboard))
    }

    private fun setupViewForSelectMode(selectMode: Boolean) {
        binding.clMessageAction.isVisible = selectMode
        binding.tvSelectedMessageCount.isVisible = selectMode
        binding.editText.isVisible = !selectMode
        binding.addWallet.isVisible = !selectMode
        binding.send.isVisible = !selectMode
        binding.memberCount.isVisible = !selectMode
        binding.toolbarTitle.isVisible = !selectMode
        binding.toolbar.navigationIcon = if (selectMode) {
            ContextCompat.getDrawable(requireActivity(), R.drawable.ic_close)
        } else {
            ContextCompat.getDrawable(requireActivity(), R.drawable.ic_back)
        }
    }

    private fun onOpenMediaViewer(eventId: String) {
        findNavController().navigate(
            RoomDetailFragmentDirections.actionRoomDetailFragmentToRoomMediaViewerFragment(eventId)
        )
    }

    private fun downloadOrOpen(media: NunchukFileMessage) {
        viewModel.downloadOrOpen(media)
    }

    private fun openTransactionDetails(walletId: String, txId: String, initEventId: String) {
        navigator.openTransactionDetailsScreen(
            launcher = launcher,
            activityContext = requireActivity(),
            walletId = walletId,
            txId = txId,
            initEventId = initEventId,
            roomId = args.roomId
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
            viewLifecycleOwner.lifecycleScope.launch {
                delay(300L)
                binding.editText.setText("")
            }
        }
    }

    private fun handleRoomAction(roomAction: RoomAction) {
        if (handleRoomAction) return
        when (roomAction) {
            RoomAction.SEND -> sendBTCAction()
            RoomAction.RECEIVE -> receiveBTCAction()
            RoomAction.NONE -> Unit
        }
        handleRoomAction = true
    }

    companion object {
        private const val KEY_CURRENT_PHOTO_PATH = "_a"
        private const val KEY_CURRENT_ACTION = "_b"
    }
}


