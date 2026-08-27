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

import com.nunchuk.android.core.bitbox.BitBoxCommandExecutor
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
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
     * it by — which is not always the one passed in, see the policy-name fallback below.
     */
    suspend fun <T> withRegisteredWallet(
        executor: BitBoxCommandExecutor,
        wallet: Wallet,
        block: suspend (Wallet) -> T,
    ): T {
        // The policy name is shown on the device and has to be there: a wallet parsed from a BSMS
        // has no name, since the format doesn't carry one.
        val policyWallet = if (wallet.name.isBlank()) {
            wallet.copy(name = DEFAULT_POLICY_NAME)
        } else {
            wallet
        }

        // Ask first, register only if the device doesn't already know the policy: registering is
        // a user-confirmed step on the device, so re-doing it every time would put an extra
        // approval in front of every signature and every address check.
        if (!executor.isWalletRegistered(policyWallet)) {
            Timber.tag(TAG).d("wallet not registered; registering policy")
            executor.registerWallet(policyWallet)
        }
        return block(policyWallet)
    }

    private companion object {
        private const val TAG = "BitBoxRegister"

        /** Wallet policy name registered on the device when the wallet itself has none. */
        private const val DEFAULT_POLICY_NAME = "Nunchuk"
    }
}
