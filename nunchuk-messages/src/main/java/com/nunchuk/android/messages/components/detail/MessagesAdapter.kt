package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.core.loader.ImageLoader
import com.nunchuk.android.messages.components.detail.holder.*
import com.nunchuk.android.messages.databinding.*
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.TransactionExtended

internal class MessagesAdapter(
    val context: Context,
    private val imageLoader: ImageLoader,
    private val cancelWallet: () -> Unit,
    private val denyWallet: () -> Unit,
    private val viewWalletConfig: () -> Unit,
    private val finalizeWallet: () -> Unit,
    private val viewTransaction: (walletId: String, txId: String, initEventId: String) -> Unit,
    private val dismissBannerNewChatListener: () -> Unit,
    private val createSharedWalletListener: () -> Unit
) : Adapter<ViewHolder>() {

    private var chatModels: List<AbsChatModel> = ArrayList()
    private var transactions: List<TransactionExtended> = ArrayList()
    private var roomWallet: RoomWallet? = null
    private var memberCounts: Int? = null

    internal fun update(chatModels: List<AbsChatModel>, transactions: List<TransactionExtended>, roomWallet: RoomWallet?, memberCounts: Int) {
        this.chatModels = chatModels
        this.transactions = transactions
        this.roomWallet = roomWallet
        this.memberCounts = memberCounts
        notifyDataSetChanged()
    }

    internal fun removeBannerNewChat() {
        if (chatModels.isEmpty() || chatModels.first().getType() != MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index) {
            return
        }

        val removedBannerList = chatModels.subList(1, chatModels.size - 1)

        chatModels = removedBannerList
        notifyDataSetChanged()
    }

    internal fun addBannerNewChat() {
        if (chatModels.isNotEmpty() && chatModels.first().getType() == MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index) {
            return
        }

        val newList = mutableListOf<AbsChatModel>()
        newList.add(BannerNewChatModel())
        newList.addAll(chatModels)

        chatModels = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        MessageType.TYPE_CHAT_MINE.index -> MessageMineViewHolder(
            ItemMessageMeBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_CHAT_PARTNER.index -> MessagePartnerHolder(
            imageLoader,
            ItemMessagePartnerBinding.inflate(LayoutInflater.from(context), parent, false),
            (memberCounts ?: 0) > 2
        )
        MessageType.TYPE_NOTIFICATION.index -> MessageNotificationHolder(
            ItemMessageNotificationBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_DATE.index -> MessageDateHolder(
            ItemDateBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_NUNCHUK_WALLET_CARD.index -> NunchukWalletCardHolder(
            binding = ItemWalletCardBinding.inflate(LayoutInflater.from(context), parent, false),
            denyWallet = denyWallet,
            cancelWallet = cancelWallet,
            viewConfig = viewWalletConfig
        )
        MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index -> NunchukTransactionCardHolder(
            binding = ItemTransactionCardBinding.inflate(LayoutInflater.from(context), parent, false),
            viewTransaction = viewTransaction,
        )
        MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index -> NunchukWalletNotificationHolder(
            binding = ItemNunchukNotificationBinding.inflate(LayoutInflater.from(context), parent, false),
            viewConfig = viewWalletConfig,
            finalizeWallet = finalizeWallet
        )
        MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index -> NunchukTransactionNotificationHolder(
            binding = ItemNunchukNotificationBinding.inflate(LayoutInflater.from(context), parent, false),
            viewTransaction = viewTransaction
        )
        MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index -> NunchukBannerNewChatHolder(
            binding = ItemNunchukBannerNewChatBinding.inflate(LayoutInflater.from(context), parent, false),
            dismissBannerNewChatListener = dismissBannerNewChatListener,
            createSharedWalletListener = createSharedWalletListener
        )
        else -> throw IllegalArgumentException("Invalid type")
    }

    override fun getItemCount(): Int = chatModels.size

    override fun getItemViewType(position: Int): Int = chatModels[position].getType()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val messageData = chatModels[position]
        when (getItemViewType(position)) {
            MessageType.TYPE_CHAT_MINE.index -> {
                (holder as MessageMineViewHolder).bind((messageData as MessageModel).message)
            }
            MessageType.TYPE_CHAT_PARTNER.index -> {
                (holder as MessagePartnerHolder).bind((messageData as MessageModel).message)
            }
            MessageType.TYPE_NOTIFICATION.index -> {
                (holder as MessageNotificationHolder).bind((messageData as MessageModel).message as NotificationMessage)
            }
            MessageType.TYPE_DATE.index -> {
                (holder as MessageDateHolder).bind(messageData as DateModel)
            }
            MessageType.TYPE_NUNCHUK_WALLET_CARD.index -> {
                (holder as NunchukWalletCardHolder).bind(roomWallet, (messageData as MessageModel).message as NunchukWalletMessage)
            }
            MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index -> {
                (holder as NunchukTransactionCardHolder).bind(transactions, (messageData as MessageModel).message as NunchukTransactionMessage)
            }
            MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index -> {
                (holder as NunchukWalletNotificationHolder).bind((messageData as MessageModel).message as NunchukWalletMessage)
            }
            MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index -> {
                (holder as NunchukTransactionNotificationHolder).bind(roomWallet, transactions, (messageData as MessageModel).message as NunchukTransactionMessage)
            }
            MessageType.TYPE_NUNCHUK_BANNER_NEW_CHAT.index -> {
                (holder as NunchukBannerNewChatHolder).bind()
            }
        }
    }

}
