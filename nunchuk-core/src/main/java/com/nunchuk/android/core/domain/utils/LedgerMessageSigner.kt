/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.core.domain.utils

import com.nunchuk.android.domain.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Confluence "3. Sign message", encapsulated as an injectable coordinator so the transport UI
 * (the Ledger sheet owning the BLE/USB connection) only has to provide a connected
 * [LedgerCommandExecutor]. Reusable by any caller that has a connected Ledger.
 *
 * Steps:
 *  1. Read the connected device's master fingerprint and confirm it is the key the message is
 *     being signed with ([expectedXfp]) — the address shown next to the signature is derived
 *     from that signer, so a signature from another device would be an invalid proof.
 *  2. Sign the message with the key at the signer's derivation path.
 *
 * The health check signs its own fixed message through the transport directly (it verifies the
 * signature instead of the device, and needs no address), so it does not go through here.
 */
class LedgerMessageSigner @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * @param executor a connected Ledger session for the device the user picked.
     * @param expectedXfp master fingerprint of the signer the message is signed with.
     * @param derivationPath path of the key to sign with (the signer's own path).
     * @param message the message the user typed.
     * @return the base64 signature the device produced.
     * @throws LedgerWrongDeviceException if the connected device is not [expectedXfp].
     */
    suspend fun sign(
        executor: LedgerCommandExecutor,
        expectedXfp: String,
        derivationPath: String,
        message: String,
    ): String = withContext(ioDispatcher) {
        Timber.tag(TAG).d("sign start expectedXfp=$expectedXfp path=$derivationPath")
        val deviceFingerprint = executor.getMasterFingerprint()
        Timber.tag(TAG).d("device fingerprint=$deviceFingerprint")
        if (!deviceFingerprint.equals(expectedXfp, ignoreCase = true)) {
            throw LedgerWrongDeviceException(expected = expectedXfp, actual = deviceFingerprint)
        }

        executor.signMessage(derivationPath, message)
            .also { Timber.tag(TAG).d("signed (signatureLen=${it.length})") }
    }

    private companion object {
        private const val TAG = "LedgerSignMessage"
    }
}
