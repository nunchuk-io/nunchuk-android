package com.nunchuk.android.nav.args

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainComposeArgs(
    val type: Int,
) : Parcelable {
    fun buildBundle() = Bundle().apply {
        putInt(EXTRA_TYPE, type)
    }

    companion object {
        const val TYPE_ARCHIVE = 1
        const val TYPE_GUEST_WALLET_NOTICE = 2

        private const val EXTRA_TYPE = "EXTRA_TYPE"

        fun deserializeFrom(bundle: Bundle): MainComposeArgs = MainComposeArgs(
            bundle.getInt(EXTRA_TYPE, TYPE_ARCHIVE)
        )
    }
}