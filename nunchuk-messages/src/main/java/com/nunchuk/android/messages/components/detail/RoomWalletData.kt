package com.nunchuk.android.messages.components.detail

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RoomWalletData(
    @SerializedName("address_type")
    val addressType: String,
    @SerializedName("chain")
    val chain: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("is_escrow")
    val isEscrow: Boolean,
    @SerializedName("m")
    val requireSigners: Int,
    @SerializedName("members")
    val members: List<String>,
    @SerializedName("n")
    val totalSigners: Int,
    @SerializedName("name")
    val name: String
) : Serializable

fun String.toRoomWalletData(gson: Gson): RoomWalletData = gson.fromJson(this, RoomWalletData::class.java)

fun String.toRoomWalletData(): RoomWalletData = toRoomWalletData(Gson())