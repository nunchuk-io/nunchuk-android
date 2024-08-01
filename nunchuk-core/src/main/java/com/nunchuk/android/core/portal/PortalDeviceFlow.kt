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
    val isMembershipFlow: Boolean = false,
    val walletId: String = ""
) : Parcelable {
    companion object {
        private const val EXTRA_TYPE = "EXTRA_TYPE"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_IS_MEMBERSHIP_FLOW = "EXTRA_IS_MEMBERSHIP_FLOW"

        fun fromBundle(bundle: Bundle): PortalDeviceArgs {
            return PortalDeviceArgs(
                type = bundle.serializable<PortalDeviceFlow>(EXTRA_TYPE)!!,
                walletId = bundle.getString(EXTRA_WALLET_ID, ""),
                isMembershipFlow = bundle.getBoolean(EXTRA_IS_MEMBERSHIP_FLOW, false)
            )
        }
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putSerializable(EXTRA_TYPE, type)
            putString(EXTRA_WALLET_ID, walletId)
            putBoolean(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
        }
    }
}