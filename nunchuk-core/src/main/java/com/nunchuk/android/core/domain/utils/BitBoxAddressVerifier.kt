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
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Confluence "5. Show address on device" for BitBox, as an injectable coordinator so the
 * transport UI (the sheet owning the BLE/USB connection) only has to hand over a connected
 * [BitBoxCommandExecutor]. The BitBox counterpart of [LedgerAddressVerifier].
 *
 * Steps:
 *  1. Resolve the address' index within the wallet.
 *  2. Register the wallet policy on the device if it isn't already ([BitBoxWalletRegistrar]) —
 *     the device only derives addresses for a policy it has approved.
 *  3. Ask the device to show that address, and compare what it derived with what we display.
 *
 * Unlike signing there is no device check up front: any BitBox holding a key of this wallet
 * derives the same address, and one that doesn't simply can't register the wallet.
 */
class BitBoxAddressVerifier @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
    private val walletRegistrar: BitBoxWalletRegistrar,
) {
    /**
     * @param executor a connected BitBox session for the device the user picked.
     * @param walletId the wallet the address belongs to.
     * @param address the receive address shown in the app, to check against the device.
     * @return true when the device derived — and displayed — the same address.
     */
    suspend fun verify(
        executor: BitBoxCommandExecutor,
        walletId: String,
        address: String,
    ): Boolean = withContext(ioDispatcher) {
        Timber.tag(TAG).d("verify start walletId=$walletId address=$address")
        val addressIndex = nativeSdk.getAddressIndex(walletId, address)
        require(addressIndex >= 0) { "Address does not belong to this wallet" }

        val wallet = nativeSdk.getWallet(walletId)
        val deviceAddress = walletRegistrar.withRegisteredWallet(executor, wallet) { policyWallet ->
            Timber.tag(TAG).d("getWalletAddress index=$addressIndex")
            // Only receive addresses are verified from the address list, so never the change
            // chain — Confluence §5 wants the same `change` that produced the address.
            executor.getWalletAddress(policyWallet, addressIndex, change = false)
        }

        Timber.tag(TAG).d("device address=$deviceAddress")
        // Bech32 addresses are case-insensitive, and the device may render them in either case.
        deviceAddress.trim().equals(address.trim(), ignoreCase = true)
    }

    private companion object {
        private const val TAG = "BitBoxVerifyAddress"
    }
}
