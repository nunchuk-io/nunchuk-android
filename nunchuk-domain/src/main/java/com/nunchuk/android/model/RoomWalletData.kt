package com.nunchuk.android.model

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
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
    @SerializedName("m")
    val requireSigners: Int,
    @SerializedName("members")
    val members: List<String>? = emptyList(),
    @SerializedName("n")
    val totalSigners: Int,
    @SerializedName("name")
    val name: String
) : Serializable, Parcelable

@Parcelize
data class JoinKey(
    val chatId: String,
    val name: String,
    @SerializedName("derivation_path")
    val derivationPath: String,
    @SerializedName("master_fingerprint")
    val masterFingerprint: String,
    @SerializedName("signer_type")
    val signerType: String,
) : Serializable, Parcelable

fun String.toRoomWalletData(gson: Gson): RoomWalletData = gson.fromJson(this, RoomWalletData::class.java)

fun String.toRoomWalletData(): RoomWalletData = toRoomWalletData(Gson())

fun RoomWallet.joinKeys(): List<JoinKey> = try {
    val retVal = ArrayList<JoinKey>()
    val joinsData = JSONObject(jsonContent).get("joins") as JSONObject
    val keys: Iterator<*> = joinsData.keys()
    while (keys.hasNext()) {
        try {
            val key = keys.next() as String
            val elements: List<JoinKey> = Gson().fromJson(joinsData.getJSONArray(key).toString(), object : TypeToken<List<JoinKey>>() {}.type)
            retVal.addAll(elements.map { it.copy(chatId = key, name = "") })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    retVal
} catch (t: Throwable) {
    t.printStackTrace()
    emptyList()
}
