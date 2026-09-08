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

import com.nunchuk.android.core.bitbox.BitBoxCommandException
import com.nunchuk.android.core.bitbox.BitBoxCommandExecutor
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.BitBoxErrorCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * "Register the wallet policy on the device if it doesn't already have it" — the step both
 * signing (Confluence §3) and showing an address (§5) open with, since the device only works
 * with a policy it has approved.
 *
 * The BitBox counterpart of [LedgerWalletRegistrar], and much thinner: BitBox self-reports
 * registration, so there is no HMAC to hand back, nothing to cache per wallet, and no special
 * case for a wallet with no local storage to cache into.
 */
class BitBoxWalletRegistrar @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
) {
    /**
     * Registers the wallet with [walletId] on the connected device as an end in itself — the
     * "BitBox" entry in the wallet's export options, which exists so the user can put the wallet
     * on a device without waiting for a transaction to sign.
     *
     * Unlike the Ledger entry point this still asks the device first: BitBox self-reports
     * registration, so "does this device hold the policy" has a real answer here, and a second
     * registration would only put another approval in front of a device that is already done.
     */
    suspend fun registerWallet(executor: BitBoxCommandExecutor, walletId: String) {
        withContext(ioDispatcher) {
            withRegisteredWallet(executor, nativeSdk.getWallet(walletId)) { }
        }
    }

    /**
     * Ensures [wallet] is registered, then runs [block] with the wallet the device actually knows
     * it by — which is not always the one passed in, see the policy-name handling below.
     */
    suspend fun <T> withRegisteredWallet(
        executor: BitBoxCommandExecutor,
        wallet: Wallet,
        block: suspend (Wallet) -> T,
    ): T {
        // The policy name is shown on the device and has to be there: a wallet parsed from a BSMS
        // has no name, since the format doesn't carry one. Such a wallet is registered under a
        // name carrying its id from the start — every sign-in via BSMS would otherwise register a
        // second policy under the same fixed name, which is the collision below.
        val policyWallet = if (wallet.name.isBlank()) {
            wallet.copy(name = uniqueName(DEFAULT_POLICY_NAME, wallet.id))
        } else {
            wallet
        }

        // Ask first, register only if the device doesn't already know the policy: registering is
        // a user-confirmed step on the device, so re-doing it every time would put an extra
        // approval in front of every signature and every address check.
        val registeredWallet = if (executor.isWalletRegistered(policyWallet)) {
            policyWallet
        } else {
            Timber.tag(TAG).d("wallet not registered; registering policy")
            register(executor, policyWallet)
        }
        return block(registeredWallet)
    }

    /**
     * Registers [policyWallet], retrying under a name the device cannot already hold.
     *
     * The device answers "is this registered" by policy — what [BitBoxCommandExecutor.isWalletRegistered]
     * asks — but it also keeps the names of its registrations unique, and rejects a policy under
     * a name it already holds with "duplicate entry". So a wallet whose
     * name is on the device against a different policy — one re-created after being deleted, two
     * wallets that share a name, a wallet the device knows from another of its accounts — fails
     * the registration the check said was needed, and fails it after the user has confirmed it on
     * the device, so it reads as the signature or the address check itself failing.
     *
     * The wallet id disambiguates: it is the descriptor checksum, eight printable ASCII
     * characters, the same every time for the same wallet and different for every other one.
     *
     * @return the wallet under the name the device now holds the policy by.
     */
    private suspend fun register(
        executor: BitBoxCommandExecutor,
        policyWallet: Wallet,
    ): Wallet {
        try {
            executor.registerWallet(policyWallet)
            return policyWallet
        } catch (e: BitBoxCommandException) {
            val fallbackName = uniqueName(policyWallet.name, policyWallet.id)
            // Nothing left to try when the name already carries the id, or when the failure is
            // anything other than the device refusing the name.
            if (e.code != BitBoxErrorCode.DEVICE_DUPLICATE || fallbackName == policyWallet.name) {
                throw e
            }
            Timber.tag(TAG).d("policy name is taken; registering as $fallbackName")
            val fallbackWallet = policyWallet.copy(name = fallbackName)
            executor.registerWallet(fallbackWallet)
            return fallbackWallet
        }
    }

    /**
     * [name] with [id] appended, trimmed to what the device accepts — it takes at most
     * [MAX_POLICY_NAME_LENGTH] characters and rejects a longer one outright. Idempotent, so a
     * name that already ends in the id is left alone rather than carrying it twice.
     */
    private fun uniqueName(name: String, id: String): String {
        if (id.isBlank()) return name
        val suffix = " $id"
        if (name.endsWith(suffix)) return name
        val room = (MAX_POLICY_NAME_LENGTH - suffix.length).coerceAtLeast(0)
        return name.take(room).trimEnd() + suffix
    }

    private companion object {
        private const val TAG = "BitBoxRegister"

        /** Wallet policy name registered on the device when the wallet itself has none. */
        private const val DEFAULT_POLICY_NAME = "Nunchuk"

        /** The device's limit on a registration name, `ValidateWalletName` in libnunchuk. */
        private const val MAX_POLICY_NAME_LENGTH = 30
    }
}
