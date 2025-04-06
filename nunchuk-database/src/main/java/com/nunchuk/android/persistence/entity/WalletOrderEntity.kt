package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.model.wallet.WalletOrder
import com.nunchuk.android.persistence.TABLE_WALLET_ORDER
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_WALLET_ORDER)
data class WalletOrderEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "wallet_id")
    val walletId: String,
    @ColumnInfo(name = "order")
    val order: Int,
    @ColumnInfo(name = "chat_id")
    val chatId: String,
    @ColumnInfo(name = "chain")
    val chain: Chain
)

fun WalletOrderEntity.toDomain(): WalletOrder {
    return WalletOrder(
        walletId = walletId,
        order = order
    )
}

fun WalletOrder.toEntity(chatId: String, chain: Chain): WalletOrderEntity {
    return WalletOrderEntity(
        walletId = walletId,
        order = order,
        chatId = chatId,
        chain = chain
    )
}