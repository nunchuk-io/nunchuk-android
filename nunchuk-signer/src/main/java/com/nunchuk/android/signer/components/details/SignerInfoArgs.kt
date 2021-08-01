package com.nunchuk.android.signer.components.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getStringValue

data class SignerInfoArgs(
    val id: String,
    val name: String,
    val justAdded: Boolean = false,
    val software: Boolean = false,
    val setPassphrase: Boolean = false
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, SignerInfoActivity::class.java).apply {
        putExtra(EXTRA_SIGNER_ID, id)
        putExtra(EXTRA_SIGNER_NAME, name)
        putExtra(EXTRA_SIGNER_ADDED, justAdded)
        putExtra(EXTRA_SIGNER_SOFTWARE, software)
        putExtra(EXTRA_SIGNER_SET_PASS_PHRASE, setPassphrase)
    }

    companion object {
        private const val EXTRA_SIGNER_ID = "EXTRA_SIGNER_ID"
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"
        private const val EXTRA_SIGNER_ADDED = "EXTRA_SIGNER_ADDED"
        private const val EXTRA_SIGNER_SOFTWARE = "EXTRA_SIGNER_SOFTWARE"
        private const val EXTRA_SIGNER_SET_PASS_PHRASE = "EXTRA_SIGNER_SET_PASS_PHRASE"

        fun deserializeFrom(intent: Intent): SignerInfoArgs {
            val bundle = intent.extras
            return SignerInfoArgs(
                id = bundle.getStringValue(EXTRA_SIGNER_ID),
                name = bundle.getStringValue(EXTRA_SIGNER_NAME),
                justAdded = bundle.getBooleanValue(EXTRA_SIGNER_ADDED),
                software = bundle.getBooleanValue(EXTRA_SIGNER_SOFTWARE),
                setPassphrase = bundle.getBooleanValue(EXTRA_SIGNER_SET_PASS_PHRASE)
            )
        }
    }
}