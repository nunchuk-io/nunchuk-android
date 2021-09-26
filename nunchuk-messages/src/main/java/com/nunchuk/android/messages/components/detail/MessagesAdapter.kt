package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.nunchuk.android.messages.components.detail.holder.*
import com.nunchuk.android.messages.databinding.*
import com.nunchuk.android.model.Transaction

internal class MessagesAdapter(
    val context: Context,
    private val cancelWallet: () -> Unit,
    private val denyWallet: () -> Unit,
    private val viewConfig: () -> Unit,
    private val finalizeWallet: () -> Unit,
    private val viewDetails: (walletId: String, txId: String, initEventId: String) -> Unit,
    private val getRoomTransaction: (initEventId: String, walletId: String, callback: (Transaction) -> Unit) -> Unit,
) : Adapter<ViewHolder>() {

    internal var chatModels: List<AbsChatModel> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        MessageType.TYPE_CHAT_MINE.index -> MessageMineViewHolder(
            ItemMessageMeBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_CHAT_PARTNER.index -> MessagePartnerHolder(
            ItemMessagePartnerBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_NOTIFICATION.index -> MessageNotificationHolder(
            ItemMessageNotificationBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_DATE.index -> MessageDateHolder(
            ItemDateBinding.inflate(LayoutInflater.from(context), parent, false)
        )
        MessageType.TYPE_NUNCHUK_WALLET_CARD.index -> NunchukWalletCardHolder(
            ItemWalletInfoBinding.inflate(LayoutInflater.from(context), parent, false),
            denyWallet = denyWallet,
            cancelWallet = cancelWallet,
            viewConfig = viewConfig
        )
        MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index -> NunchukTransactionCardHolder(
            ItemTransactionInfoBinding.inflate(LayoutInflater.from(context), parent, false),
            viewDetails = viewDetails,
            getRoomTransaction = getRoomTransaction
        )
        MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index -> NunchukWalletNotificationHolder(
            ItemNunchukNotificationBinding.inflate(LayoutInflater.from(context), parent, false),
            viewConfig = viewConfig,
            finalizeWallet = finalizeWallet
        )
        MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index -> NunchukTransactionNotificationHolder(
            ItemNunchukNotificationBinding.inflate(LayoutInflater.from(context), parent, false)
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
                (holder as NunchukWalletCardHolder).bind((messageData as MessageModel).message as NunchukWalletMessage)
            }
            MessageType.TYPE_NUNCHUK_TRANSACTION_CARD.index -> {
                (holder as NunchukTransactionCardHolder).bind((messageData as MessageModel).message as NunchukTransactionMessage)
            }
            MessageType.TYPE_NUNCHUK_WALLET_NOTIFICATION.index -> {
                (holder as NunchukWalletNotificationHolder).bind((messageData as MessageModel).message as NunchukWalletMessage)
            }
            MessageType.TYPE_NUNCHUK_TRANSACTION_NOTIFICATION.index -> {
                (holder as NunchukTransactionNotificationHolder).bind((messageData as MessageModel).message as NunchukTransactionMessage)
            }
        }
    }

}
