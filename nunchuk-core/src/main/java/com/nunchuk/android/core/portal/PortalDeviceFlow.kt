package com.nunchuk.android.core.portal

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import com.nunchuk.android.utils.serializable
import kotlinx.parcelize.Parcelize

@Keep
enum class PortalDeviceFlow {
    SETUP,
    RECOVER
}

@Parcelize
data class PortalDeviceArgs(
    val type: PortalDeviceFlow = PortalDeviceFlow.SETUP
) : Parcelable {
    companion object {
        private const val EXTRA_TYPE = "EXTRA_TYPE"
        fun fromBundle(bundle: Bundle): PortalDeviceArgs {
            return PortalDeviceArgs(
                type = bundle.serializable<PortalDeviceFlow>(EXTRA_TYPE)!!
            )
        }
    }

    fun toBundle(): Bundle {
        return Bundle().apply {
            putSerializable(EXTRA_TYPE, type)
        }
    }
}