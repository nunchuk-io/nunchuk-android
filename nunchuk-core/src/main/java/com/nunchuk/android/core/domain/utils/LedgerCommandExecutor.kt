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

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.LedgerTransport

/**
 * Transport-agnostic view of a connected Ledger session, exposing the native
 * commands as suspend functions so orchestration logic (see [LedgerTransactionSigner])
 * can be written linearly without knowing about BLE/USB, frame pumping or the step loop.
 *
 * Implementations own the connected transport and drive the native step loop
 * (WRITE -> onData -> READ_MORE -> ... -> COMPLETE / FAILED), resuming each suspend call
 * with the [com.nunchuk.android.nativelib.NunchukNativeSdk.ledgerResultString] payload on
 * COMPLETE, or failing it with a [LedgerCommandException] on FAILED (carrying the device
 * status word so callers can detect a stale HMAC).
 */
interface LedgerCommandExecutor {
    /** Confluence "Get XPUB" step 1 — the connected device's master fingerprint. */
    suspend fun getMasterFingerprint(): String

    /** Confluence "Sign transaction" — registers [wallet] on the device; returns the wallet HMAC. */
    suspend fun registerWallet(wallet: Wallet): String

    /** Confluence "Sign transaction" — signs [psbt] with the registered [wallet]; returns the signed PSBT. */
    suspend fun signPsbt(wallet: Wallet, hmac: String, psbt: String): String
}

/**
 * Thrown when a Ledger command reaches FAILED. [statusWord] is the raw uint16 status
 * word from the device (0 when unknown / a transport-level failure). Notably
 * [SW_INVALID_SIGNATURE_OR_HMAC] signals the stored wallet HMAC is stale (from another
 * device or a changed wallet policy) and the wallet must be re-registered.
 */
class LedgerCommandException(
    val statusWord: Int,
    message: String,
) : Exception(message) {
    companion object {
        /** 0xB008 — the registered wallet HMAC is invalid for this device / policy. */
        const val SW_INVALID_SIGNATURE_OR_HMAC = 0xB008
    }
}
