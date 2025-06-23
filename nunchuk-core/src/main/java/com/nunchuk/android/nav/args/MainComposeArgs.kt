package com.nunchuk.android.nav.args

import android.os.Bundle
import android.os.Parcelable
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.utils.parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainComposeArgs(
    val type: Int,
    val btcUri: BtcUri? = null
) : Parcelable {
    fun buildBundle() = Bundle().apply {
        putInt(EXTRA_TYPE, type)
        putParcelable(EXTRA_BTC_URI, btcUri)
    }

    companion object {
        const val TYPE_ARCHIVE = 1
        const val TYPE_GUEST_WALLET_NOTICE = 2
        const val TYPE_CHOOSE_WALLET_TO_SEND = 3

        private const val EXTRA_TYPE = "EXTRA_TYPE"
        private const val EXTRA_BTC_URI = "EXTRA_BTC_URI"

        fun deserializeFrom(bundle: Bundle): MainComposeArgs = MainComposeArgs(
            bundle.getInt(EXTRA_TYPE, TYPE_ARCHIVE),
            bundle.parcelable<BtcUri>(EXTRA_BTC_URI)
        )
    }
}