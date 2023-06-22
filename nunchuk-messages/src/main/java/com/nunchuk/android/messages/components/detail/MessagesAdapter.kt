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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.messages.components.detail.holder.*
import com.nunchuk.android.messages.databinding.*
import com.nunchuk.android.model.RoomWallet

internal class MessagesAdapter(
    private val imageLoader: ImageLoader,
    private val cancelWallet: () -> Unit,
    private val denyWallet: () -> Unit,
    private val viewWalletConfig: () -> Unit,
    private val finalizeWallet: () -> Unit,
    private val viewTransaction: (walletId: String, txId: String, initEventId: String) -> Unit,
    private val dismissBannerNewChatListener: () -> Unit,
    private val createSharedWalletListener: () -> Unit,
    private val senderLongPressListener: (message: Message, position: Int) -> Unit,
    private val onMessageRead: (eventId: String) -> Unit,
    private val toggleSelected: (localId: Long) -> Unit,
    private val onOpenMediaViewer: (eventId: String) -> Unit,
    private val onDownloadOrOpen: (media: NunchukFileMessage) -> Unit
) : ListAdapter<AbsChatModel, ViewHolder>(ChatMessageDiffCallback) {

    private var roomWallet: RoomWallet? = null
    private var memberCounts: Int? = null
    private var isShowNewBanner: Boolean = true

    internal fun update(
        chatModels: List<AbsChatModel>,
        roomWallet: RoomWallet?,
        memberCounts: Int
    ) {
        this.roomWallet = roomWallet
        this.memberCounts = memberCounts
        initListChatMessages(chatModels)
    }

    internal fun updateSelectedPosition(
        selectedPosition: Int,
    ) {
        ((getItem(selectedPosition) as? MessageModel)?.message as? MatrixMessage)?.let {
            toggleSelected(it.timelineEvent.localId)
        }
    }

    internal fun getSelectedMessage(): List<MatrixMessage> {
        return currentList.filter {
            if (it !is MessageModel || it.message !is MatrixMessage) {
                false
            } else {
                it.message.selected
            }
        }.map {
            ((it as MessageModel).message as MatrixMessage)
        }
    }


    private fun initListChatMessages(
        chatModels: List<AbsChatModel>
    ) {
        if (roomWallet == null && isShowNewBanner) {
            submitList(initChatListWithNewBanner(chatModels))
        } else {
            submitList(chatModels)
        }
    }

    internal fun removeBannerNewChat() {
        isShowNewBanner = false

        val chatModels = currentList.filter { it.getType() != MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index }
        initListChatMessages(
            chatModels = chatModels
        )
    }

    private fun initChatListWithNewBanner(chatModels: List<AbsChatModel>): MutableList<AbsChatModel> {
        val newList = mutableListOf<AbsChatModel>()
        newList.add(BannerNewChatModel)
        newList.addAll(chatModels)
        return newList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        MessageType.TYPE_CHAT_MINE.index -> MessageMineViewHolder(
            ItemMessageMeBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            longPressListener = senderLongPressListener,
            checkedChangeListener = { position ->
                updateSelectedPosition(position)
            },
        )
        MessageType.TYPE_CHAT_PARTNER.index -> MessagePartnerHolder(
            imageLoader,
            ItemMessagePartnerBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            (memberCounts ?: 0) > 2,
            longPressListener = senderLongPressListener,
            checkedChangeListener = { position ->
                updateSelectedPosition(position)
            },
            onMessageRead = onMessageRead
        )
        MessageType.TYPE_NOTIFICATION.index -> MessageNotificationHolder(
            ItemMessageNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        MessageType.TYPE_DATE.index -> MessageDateHolder(
            ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        MessageType.TYPE_NUNCHUK_WALLET_CARD.index -> NunchukWalletCardHolder(
            binding = ItemWalletCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            denyWallet = denyWallet,
            cancelWallet = cancelWallet,
            viewConfig = viewWalletConfig
        )
        MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index -> NunchukTransactionCardHolder(
            binding = ItemTransactionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            viewTransaction = viewTransaction,
        )
        MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index -> NunchukWalletNotificationHolder(
            binding = ItemNunchukNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            viewConfig = viewWalletConfig,
            finalizeWallet = finalizeWallet
        )
        MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index -> NunchukTransactionNotificationHolder(
            binding = ItemNunchukNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            viewTransaction = viewTransaction
        )
        MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index -> NunchukBannerNewChatHolder(
            binding = ItemNunchukBannerNewChatBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            dismissBannerNewChatListener = dismissBannerNewChatListener,
            createSharedWalletListener = createSharedWalletListener
        )
        MessageType.TYPE_IMAGE_AND_VIDEO.index -> MessageMediaViewHolder(
            binding = ItemMessageMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onOpenMediaViewer = onOpenMediaViewer,
        )
        MessageType.TYPE_FILE.index -> MessageFileHolder(
            binding = ItemFileAttachmentBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onDownloadOrOpen = onDownloadOrOpen
        )
        else -> throw IllegalArgumentException("Invalid type")
    }

    override fun getItemViewType(position: Int): Int = getItem(position).getType()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageData = getItem(position)
        when (getItemViewType(position)) {
            MessageType.TYPE_CHAT_MINE.index -> {
                (holder as MessageMineViewHolder).bind(((messageData as MessageModel).message as MatrixMessage), position)
            }
            MessageType.TYPE_CHAT_PARTNER.index -> {
                (holder as MessagePartnerHolder).bind(((messageData as MessageModel).message as MatrixMessage), position)
            }
            MessageType.TYPE_NOTIFICATION.index -> {
                (holder as MessageNotificationHolder).bind((messageData as MessageModel).message as NotificationMessage)
            }
            MessageType.TYPE_DATE.index -> {
                (holder as MessageDateHolder).bind(messageData as DateModel)
            }
            MessageType.TYPE_NUNCHUK_WALLET_CARD.index -> {
                (holder as NunchukWalletCardHolder).bind((messageData as MessageModel).message as NunchukWalletMessage)
            }
            MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index -> {
                (holder as NunchukTransactionCardHolder).bind((messageData as MessageModel).message as NunchukTransactionMessage)
            }
            MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index -> {
                (holder as NunchukWalletNotificationHolder).bind((messageData as MessageModel).message as NunchukWalletMessage)
            }
            MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index -> {
                (holder as NunchukTransactionNotificationHolder).bind(
                    (messageData as MessageModel).message as NunchukTransactionMessage
                )
            }
            MessageType.TYPE_IMAGE_AND_VIDEO.index -> {
                (holder as MessageMediaViewHolder).bind(
                    (messageData as MessageModel).message as NunchukMediaMessage
                )
            }
            MessageType.TYPE_FILE.index -> {
                (holder as MessageFileHolder).bind(
                    (messageData as MessageModel).message as NunchukFileMessage
                )
            }
            MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index -> {
                // Do nothing
            }
        }
    }

}
