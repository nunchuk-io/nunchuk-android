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

package com.nunchuk.android.nav.args

import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.utils.serializable

// Data class for named signers that can be properly serialized
data class NamedSigner(
    val keyName: String,
    val signer: SignerModel?
) : android.os.Parcelable {
    constructor(parcel: android.os.Parcel) : this(
        parcel.readString() ?: "",
        parcel.readParcelable<SignerModel>(SignerModel::class.java.classLoader)
    )

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(keyName)
        parcel.writeParcelable(signer, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : android.os.Parcelable.Creator<NamedSigner> {
        override fun createFromParcel(parcel: android.os.Parcel): NamedSigner {
            return NamedSigner(parcel)
        }

        override fun newArray(size: Int): Array<NamedSigner?> {
            return arrayOfNulls(size)
        }
    }
}

data class ReviewWalletArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val totalRequireSigns: Int,
    val signers: List<SingleSigner>,
    val decoyPin: String = "",
    val groupId: String = "",
    val isValueKeySetEnable: Boolean = false,
    val quickWalletParam: QuickWalletParam? = null,
    val scriptNode: ScriptNode? = null,
    val scriptNodeMuSig: ScriptNode? = null,
    val keyPath: List<String> = emptyList(),
    val namedSigners: List<NamedSigner> = emptyList(),
    val supportedTypes: List<SupportedSigner> = emptyList(),
) {

    fun buildBundle() = Bundle().apply {
        putString(EXTRA_WALLET_NAME, walletName)
        putSerializable(EXTRA_WALLET_TYPE, walletType)
        putSerializable(EXTRA_ADDRESS_TYPE, addressType)
        putInt(EXTRA_TOTAL_REQUIRED_SIGNS, totalRequireSigns)
        putParcelableArrayList(EXTRA_SIGNERS, ArrayList(signers))
        putString(EXTRA_DECOY_PIN, decoyPin)
        putString(EXTRA_GROUP_ID, groupId)
        putBoolean(EXTRA_VALUE_KEY_SET_ENABLE, isValueKeySetEnable)
        putParcelable(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
        // Miniscript fields
        putParcelable(EXTRA_SCRIPT_NODE, scriptNode)
        putParcelable(EXTRA_SCRIPT_NODE_MU_SIG, scriptNodeMuSig)
        putStringArrayList(EXTRA_KEY_PATH, ArrayList(keyPath))
        putParcelableArrayList(EXTRA_NAMED_SIGNERS, ArrayList(namedSigners))
        putParcelableArrayList(EXTRA_SUPPORTED_TYPES, ArrayList(supportedTypes))
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_TOTAL_REQUIRED_SIGNS = "EXTRA_TOTAL_REQUIRED_SIGNS"
        private const val EXTRA_SIGNERS = "EXTRA_SIGNERS"
        private const val EXTRA_DECOY_PIN = "EXTRA_DECOY_PIN"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        private const val EXTRA_VALUE_KEY_SET_ENABLE = "EXTRA_VALUE_KEY_SET_ENABLE"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"
        private const val EXTRA_SCRIPT_NODE = "EXTRA_SCRIPT_NODE"
        private const val EXTRA_SCRIPT_NODE_MU_SIG = "EXTRA_SCRIPT_NODE_MU_SIG"
        private const val EXTRA_KEY_PATH = "EXTRA_KEY_PATH"
        private const val EXTRA_NAMED_SIGNERS = "EXTRA_NAMED_SIGNERS"
        private const val EXTRA_SUPPORTED_TYPES = "EXTRA_SUPPORTED_TYPES"

        fun deserializeFrom(intent: Intent): ReviewWalletArgs = ReviewWalletArgs(
            intent.extras.getStringValue(EXTRA_WALLET_NAME),
            intent.serializable<WalletType>(EXTRA_WALLET_TYPE)!!,
            intent.serializable<AddressType>(EXTRA_ADDRESS_TYPE)!!,
            intent.getIntExtra(EXTRA_TOTAL_REQUIRED_SIGNS, 0),
            intent.parcelableArrayList<SingleSigner>(EXTRA_SIGNERS).orEmpty(),
            intent.extras.getStringValue(EXTRA_DECOY_PIN),
            intent.extras.getStringValue(EXTRA_GROUP_ID),
            intent.extras.getBooleanValue(EXTRA_VALUE_KEY_SET_ENABLE, false),
            intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM),
            // Miniscript fields
            intent.parcelable<com.nunchuk.android.model.ScriptNode>(EXTRA_SCRIPT_NODE),
            intent.parcelable<com.nunchuk.android.model.ScriptNode>(EXTRA_SCRIPT_NODE_MU_SIG),
            intent.getStringArrayListExtra(EXTRA_KEY_PATH)?.toList() ?: emptyList(),
            intent.parcelableArrayList<NamedSigner>(EXTRA_NAMED_SIGNERS).orEmpty(),
            intent.parcelableArrayList<com.nunchuk.android.model.signer.SupportedSigner>(EXTRA_SUPPORTED_TYPES) ?: emptyList(),
        )
    }

}