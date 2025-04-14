package com.nunchuk.android.core.wallet

import android.os.Bundle
import android.os.Parcelable
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import kotlinx.parcelize.Parcelize

enum class WalletSecurityType {
    CREATE_PIN,
    CREATE_DECOY_WALLET,
    CREATE_DECOY_SUCCESS
}

@Parcelize
class WalletSecurityArgs(
    val type: WalletSecurityType = WalletSecurityType.CREATE_PIN,
    val quickWalletParam: QuickWalletParam? = null,
) : Parcelable {
    companion object {
        private const val EXTRA_TYPE = "EXTRA_TYPE"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"

        fun fromBundle(bundle: Bundle): WalletSecurityArgs {
            return WalletSecurityArgs(
                type = bundle.serializable<WalletSecurityType>(EXTRA_TYPE)!!,
                quickWalletParam = bundle.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM),
            )
        }
    }

    fun buildBundle(): Bundle {
        return Bundle().apply {
            putSerializable(EXTRA_TYPE, type)
            putParcelable(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
        }
    }
}