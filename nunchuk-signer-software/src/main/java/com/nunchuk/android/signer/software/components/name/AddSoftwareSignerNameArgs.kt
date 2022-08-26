package com.nunchuk.android.signer.software.components.name

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.signer.PrimaryKeyFlow

data class AddSoftwareSignerNameArgs(
    val mnemonic: String,
    val primaryKeyFlow: Int,
    val username: String?,
    val passphrase: String,
    val address: String?
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, AddSoftwareSignerNameActivity::class.java).apply {
            putExtra(EXTRA_MNEMONIC, mnemonic)
            putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
            putExtra(EXTRA_PRIMARY_KEY_USERNAME, username)
            putExtra(EXTRA_PRIMARY_KEY_PASSPHRASE, passphrase)
            putExtra(EXTRA_PRIMARY_KEY_ADDRESS, address)
        }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PRIMARY_KEY_USERNAME = "EXTRA_PRIMARY_KEY_USERNAME"
        private const val EXTRA_PRIMARY_KEY_PASSPHRASE = "EXTRA_PRIMARY_KEY_PASSPHRASE"
        private const val EXTRA_PRIMARY_KEY_ADDRESS = "EXTRA_PRIMARY_KEY_ADDRESS"

        fun deserializeFrom(intent: Intent) = AddSoftwareSignerNameArgs(
            mnemonic = intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
            primaryKeyFlow = intent.extras?.getInt(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
                ?: PrimaryKeyFlow.NONE,
            username = intent.extras?.getString(EXTRA_PRIMARY_KEY_USERNAME, "").orEmpty(),
            passphrase = intent.extras?.getString(EXTRA_PRIMARY_KEY_PASSPHRASE, "").orEmpty(),
            address = intent.extras?.getString(EXTRA_PRIMARY_KEY_ADDRESS, "").orEmpty(),
        )
    }
}