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

package com.nunchuk.android.model

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.nunchuk.android.type.SignerType
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
    val members: List<MemberData>? = emptyList(),
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

@Parcelize
data class MemberData(
    @SerializedName("xpub")
    var xpub: String = "",

    @SerializedName("public_key")
    var publicKey: String = "",

    @SerializedName("derivation_path")
    var derivationPath: String = "",

    @SerializedName("master_fingerprint")
    var masterFingerprint: String = "",
): Parcelable

fun MemberData.toSingleSigner() = SingleSigner(
    xpub = xpub,
    publicKey = publicKey,
    derivationPath = derivationPath,
    masterFingerprint = masterFingerprint
)

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
