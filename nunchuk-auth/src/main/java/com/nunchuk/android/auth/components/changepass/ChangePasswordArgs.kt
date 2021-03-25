package com.nunchuk.android.auth.components.changepass

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getStringValue

data class ChangePasswordArgs(
    val userActivated: Boolean = true,
    val emailAddress: String? = null
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ChangePasswordActivity::class.java)

    companion object {
        private const val EXTRA_USER_ACTIVATED = "EXTRA_USER_ACTIVATED"
        private const val EXTRA_EMAIL_ADDRESS = "EXTRA_EMAIL_ADDRESS"

    }
}