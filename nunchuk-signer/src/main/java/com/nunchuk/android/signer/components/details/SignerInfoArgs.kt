/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.signer.components.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.type.SignerType

data class SignerInfoArgs(
    val id: String,
    val masterFingerprint: String,
    val name: String,
    val signerType: SignerType,
    val derivationPath: String,
    val justAdded: Boolean = false,
    val isNfc: Boolean = false,
    val setPassphrase: Boolean = false,
    val isInWallet: Boolean = false,
    val isReplacePrimaryKey: Boolean = false
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, SignerInfoActivity::class.java).apply {
        putExtra(EXTRA_SIGNER_ID, id)
        putExtra(EXTRA_SIGNER_NAME, name)
        putExtra(EXTRA_SIGNER_ADDED, justAdded)
        putExtra(EXTRA_SIGNER_TYPE, signerType)
        putExtra(EXTRA_SIGNER_SET_PASS_PHRASE, setPassphrase)
        putExtra(EXTRA_IS_IN_WALLET, isInWallet)
        putExtra(EXTRA_DERIVATION_PATH, derivationPath)
        putExtra(EXTRA_MASTER_FINGERPRINT, masterFingerprint)
        putExtra(EXTRA_IS_REPLACE_PRIMARY_KEY, isReplacePrimaryKey)
    }

    companion object {
        private const val EXTRA_SIGNER_ID = "EXTRA_SIGNER_ID"
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"
        private const val EXTRA_SIGNER_ADDED = "EXTRA_SIGNER_ADDED"
        private const val EXTRA_SIGNER_TYPE = "EXTRA_SIGNER_SOFTWARE"
        private const val EXTRA_SIGNER_SET_PASS_PHRASE = "EXTRA_SIGNER_SET_PASS_PHRASE"
        private const val EXTRA_IS_IN_WALLET = "EXTRA_IS_IN_WALLET"
        private const val EXTRA_DERIVATION_PATH = "EXTRA_DERIVATION_PATH"
        private const val EXTRA_MASTER_FINGERPRINT = "EXTRA_MASTER_FINGERPRINT"
        private const val EXTRA_IS_REPLACE_PRIMARY_KEY = "EXTRA_IS_REPLACE_PRIMARY_KEY"

        fun deserializeFrom(intent: Intent): SignerInfoArgs {
            val bundle = intent.extras
            return SignerInfoArgs(
                id = bundle.getStringValue(EXTRA_SIGNER_ID),
                name = bundle.getStringValue(EXTRA_SIGNER_NAME),
                justAdded = bundle.getBooleanValue(EXTRA_SIGNER_ADDED),
                signerType = intent.getSerializableExtra(EXTRA_SIGNER_TYPE) as SignerType,
                setPassphrase = bundle.getBooleanValue(EXTRA_SIGNER_SET_PASS_PHRASE),
                isInWallet = bundle.getBooleanValue(EXTRA_IS_IN_WALLET),
                derivationPath = bundle.getStringValue(EXTRA_DERIVATION_PATH),
                masterFingerprint = bundle.getStringValue(EXTRA_MASTER_FINGERPRINT),
                isReplacePrimaryKey = bundle.getBooleanValue(EXTRA_IS_REPLACE_PRIMARY_KEY)
            )
        }
    }
}