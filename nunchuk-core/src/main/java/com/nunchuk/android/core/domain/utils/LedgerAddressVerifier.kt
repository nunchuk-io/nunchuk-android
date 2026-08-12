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
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Confluence "4. Show address on device", encapsulated as an injectable coordinator so the
 * transport UI (the Ledger sheet owning the BLE/USB connection) only has to provide a connected
 * [LedgerCommandExecutor]. Reusable by any caller that has a connected Ledger.
 *
 * Steps:
 *  1. Resolve the address' index within the wallet.
 *  2. Register the wallet on the device if it isn't already ([LedgerWalletRegistrar]) — the
 *     device only derives addresses for a wallet policy it has approved.
 *  3. Ask the device to show that address, and compare what it derived with what we display.
 *
 * Unlike signing there is no device check up front: any Ledger holding a key of this wallet
 * derives the same address, and one that doesn't simply can't register the wallet.
 */
class LedgerAddressVerifier @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
    private val walletRegistrar: LedgerWalletRegistrar,
) {
    /**
     * @param executor a connected Ledger session for the device the user picked.
     * @param walletId the wallet the address belongs to.
     * @param address the receive address shown in the app, to check against the device.
     * @return true when the device derived the same address.
     */
    suspend fun verify(
        executor: LedgerCommandExecutor,
        walletId: String,
        address: String,
    ): Boolean = withContext(ioDispatcher) {
        Timber.tag(TAG).d("verify start walletId=$walletId address=$address")
        val addressIndex = nativeSdk.getAddressIndex(walletId, address)
        require(addressIndex >= 0) { "Address does not belong to this wallet" }

        val wallet = nativeSdk.getWallet(walletId)
        val deviceAddress = walletRegistrar.withRegisteredWallet(executor, wallet) { hmac ->
            Timber.tag(TAG).d("getWalletAddress index=$addressIndex (hmacLen=${hmac.length})")
            // Only receive addresses are verified from the address list, so never the change chain.
            executor.getWalletAddress(wallet, hmac, addressIndex, change = false)
        }

        Timber.tag(TAG).d("device address=$deviceAddress")
        // Bech32 addresses are case-insensitive, and the device may render them in either case.
        deviceAddress.trim().equals(address.trim(), ignoreCase = true)
    }

    private companion object {
        private const val TAG = "LedgerVerifyAddress"
    }
}
