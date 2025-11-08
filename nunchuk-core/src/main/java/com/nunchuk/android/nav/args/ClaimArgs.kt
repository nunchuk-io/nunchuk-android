package com.nunchuk.android.nav.args

import android.content.Intent
import android.os.Bundle

data class ClaimArgs(
    val bsms: String? = null,
) {
    fun buildBundle() = Bundle().apply {
        putSerializable(EXTRA_BSMS, bsms)
    }

    companion object {
        private const val EXTRA_BSMS = "extra_bsms"

        fun deserializeFrom(intent: Intent): ClaimArgs = ClaimArgs(
            bsms = intent.extras?.getString(EXTRA_BSMS)
        )
    }
}