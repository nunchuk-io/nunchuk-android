package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_CONTACT

@Entity(tableName = TABLE_CONTACT)
data class ContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "gender")
    val gender: String,

    @ColumnInfo(name = "avatar")
    val avatar: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @ColumnInfo(name = "account_id")
    val accountId: String
)