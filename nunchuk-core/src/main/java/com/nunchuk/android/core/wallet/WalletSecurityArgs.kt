package com.nunchuk.android.core.wallet

import android.os.Bundle
import android.os.Parcelable
import com.nunchuk.android.utils.serializable
import kotlinx.parcelize.Parcelize

enum class WalletSecurityType {
    CREATE_PIN,
    CREATE_DECOY_WALLET,
    CREATE_DECOY_SUCCESS
}

@Parcelize
class WalletSecurityArgs(
    val type: WalletSecurityType = WalletSecurityType.CREATE_PIN
) : Parcelable {
    companion object {
        private const val EXTRA_TYPE = "EXTRA_TYPE"

        fun fromBundle(bundle: Bundle): WalletSecurityArgs {
            return WalletSecurityArgs(
                type = bundle.serializable<WalletSecurityType>(EXTRA_TYPE)!!
            )
        }
    }

    fun buildBundle(): Bundle {
        return Bundle().apply {
            putSerializable(EXTRA_TYPE, type)
        }
    }
}