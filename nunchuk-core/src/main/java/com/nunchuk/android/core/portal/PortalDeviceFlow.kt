package com.nunchuk.android.core.portal

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import com.nunchuk.android.utils.serializable
import kotlinx.parcelize.Parcelize

@Keep
enum class PortalDeviceFlow {
    SETUP,
    RECOVER,
    EXPORT,
}

@Parcelize
data class PortalDeviceArgs(
    val type: PortalDeviceFlow = PortalDeviceFlow.SETUP,
    val walletId: String = ""
) : Parcelable {
    companion object {
        private const val EXTRA_TYPE = "EXTRA_TYPE"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun fromBundle(bundle: Bundle): PortalDeviceArgs {
            return PortalDeviceArgs(
                type = bundle.serializable<PortalDeviceFlow>(EXTRA_TYPE)!!,
                walletId = bundle.getString(EXTRA_WALLET_ID, "")
            )
        }
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putSerializable(EXTRA_TYPE, type)
            putString(EXTRA_WALLET_ID, walletId)
        }
    }
}