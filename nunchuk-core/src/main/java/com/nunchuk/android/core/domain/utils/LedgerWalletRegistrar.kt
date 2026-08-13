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

import com.nunchuk.android.core.domain.utils.LedgerCommandException.Companion.SW_INVALID_SIGNATURE_OR_HMAC
import com.nunchuk.android.model.Wallet
import timber.log.Timber
import javax.inject.Inject

/**
 * The wallet-registration step every Ledger command that speaks about a wallet needs
 * (Confluence "2. Sign transaction" and "4. Show address on device" both open with it):
 *
 *  1. Reuse the cached registration HMAC ([GetLedgerWalletHmacUseCase]), registering the wallet
 *     on the device and persisting the returned HMAC when there isn't one yet.
 *  2. Run the command. If the device rejects the HMAC ([SW_INVALID_SIGNATURE_OR_HMAC]) the
 *     stored registration is stale — clear it, re-register and retry once (Confluence §6).
 */
class LedgerWalletRegistrar @Inject constructor(
    private val getLedgerWalletHmacUseCase: GetLedgerWalletHmacUseCase,
    private val setLedgerWalletHmacUseCase: SetLedgerWalletHmacUseCase,
) {
    /**
     * Runs [block] with a registration HMAC valid for [wallet] on the connected device.
     *
     * @param executor a connected Ledger session for the device the user picked.
     * @param cacheRegistration whether the HMAC can be read from / written to the wallet's local
     * storage. False for a wallet that isn't stored locally (the sign-in dummy transaction, whose
     * wallet is parsed from a BSMS): there is no wallet row to cache in, so it registers every time.
     * @param block the device command to run; may be invoked twice (once per registration).
     */
    suspend fun <T> withRegisteredWallet(
        executor: LedgerCommandExecutor,
        wallet: Wallet,
        cacheRegistration: Boolean = true,
        block: suspend (hmac: String) -> T,
    ): T {
        var hmac = if (cacheRegistration) getLedgerWalletHmacUseCase(wallet.id).getOrThrow() else ""
        Timber.tag(TAG).d(
            "cached hmac ${if (hmac.isBlank()) "empty -> registering wallet" else "present (len=${hmac.length})"}",
        )
        if (hmac.isBlank()) hmac = register(executor, wallet, cacheRegistration)

        return try {
            block(hmac)
        } catch (e: LedgerCommandException) {
            Timber.tag(TAG).e("command failed statusWord=0x%04X msg=%s", e.statusWord, e.message)
            if (e.statusWord != SW_INVALID_SIGNATURE_OR_HMAC) throw e
            // Stored HMAC is stale (another device / changed policy): re-register and retry.
            Timber.tag(TAG).d("stale hmac -> clearing + re-registering")
            if (cacheRegistration) {
                setLedgerWalletHmacUseCase(SetLedgerWalletHmacUseCase.Param(wallet.id, ""))
                    .getOrThrow()
            }
            block(register(executor, wallet, cacheRegistration))
        }
    }

    private suspend fun register(
        executor: LedgerCommandExecutor,
        wallet: Wallet,
        cacheRegistration: Boolean,
    ): String {
        val hmac = executor.registerWallet(wallet)
        Timber.tag(TAG).d("registerWallet done hmacLen=${hmac.length}")
        if (cacheRegistration) {
            setLedgerWalletHmacUseCase(SetLedgerWalletHmacUseCase.Param(wallet.id, hmac)).getOrThrow()
        }
        return hmac
    }

    private companion object {
        private const val TAG = "LedgerRegister"
    }
}
