package com.nunchuk.android.auth.components.recover

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

internal data class RecoverPasswordArgs(val email: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, RecoverPasswordActivity::class.java).apply {
        putExtra(EXTRA_EMAIL_ADDRESS, email)
    }

    companion object {
        private const val EXTRA_EMAIL_ADDRESS = "EXTRA_EMAIL_ADDRESS"

        fun deserializeFrom(intent: Intent) = RecoverPasswordArgs(intent.extras?.getString(EXTRA_EMAIL_ADDRESS, "").orEmpty())
    }

}