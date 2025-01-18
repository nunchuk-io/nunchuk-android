package com.nunchuk.android.core.portal

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import kotlinx.parcelize.Parcelize

@Keep
enum class PortalDeviceFlow {
    SETUP,
    RECOVER,
    EXPORT,
    RESCAN,
}

@Parcelize
data class PortalDeviceArgs(
    val type: PortalDeviceFlow = PortalDeviceFlow.SETUP,
    val isMembershipFlow: Boolean = false,
    val walletId: String = "",
    val signer: SignerModel? = null,
    val newIndex: Int = 0,
    val groupId: String = "",
) : Parcelable {
    companion object {
        private const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_IS_MEMBERSHIP_FLOW = "EXTRA_IS_MEMBERSHIP_FLOW"
        private const val EXTRA_XFP = "EXTRA_XFP"
        private const val EXTRA_NEW_INDEX = "EXTRA_NEW_INDEX"
        const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"

        fun fromBundle(bundle: Bundle): PortalDeviceArgs {
            return PortalDeviceArgs(
                type = bundle.serializable<PortalDeviceFlow>(EXTRA_TYPE)!!,
                walletId = bundle.getString(EXTRA_WALLET_ID, ""),
                isMembershipFlow = bundle.getBoolean(EXTRA_IS_MEMBERSHIP_FLOW, false),
                signer = bundle.parcelable(EXTRA_XFP),
                newIndex = bundle.getInt(EXTRA_NEW_INDEX, 0),
                groupId = bundle.getString(EXTRA_GROUP_ID, "")
            )
        }
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putSerializable(EXTRA_TYPE, type)
            putString(EXTRA_WALLET_ID, walletId)
            putBoolean(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
            putParcelable(EXTRA_XFP, signer)
            putInt(EXTRA_NEW_INDEX, newIndex)
            putString(EXTRA_GROUP_ID, groupId)
        }
    }
}