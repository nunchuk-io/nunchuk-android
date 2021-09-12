package com.nunchuk.android.model

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class RoomWalletData(
    @SerializedName("address_type")
    val addressType: String,
    @SerializedName("chain")
    val chain: String? = "",
    @SerializedName("description")
    val description: String,
    @SerializedName("is_escrow")
    val isEscrow: Boolean,
    @SerializedName("n")
    val requireSigners: Int,
    @SerializedName("members")
    val members: List<String>? = emptyList(),
    @SerializedName("m")
    val totalSigners: Int,
    @SerializedName("name")
    val name: String
) : Serializable, Parcelable

fun String.toRoomWalletData(gson: Gson): RoomWalletData = gson.fromJson(this, RoomWalletData::class.java)

fun String.toRoomWalletData(): RoomWalletData = toRoomWalletData(Gson())