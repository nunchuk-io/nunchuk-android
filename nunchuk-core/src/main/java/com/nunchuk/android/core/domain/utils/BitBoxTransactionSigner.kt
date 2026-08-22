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
import com.nunchuk.android.core.bitbox.BitBoxWrongDeviceException
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.transaction.ImportPsbtUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Confluence "3. Sign transaction" for BitBox, as an injectable coordinator so the transport UI
 * (a sheet owning the BLE/USB connection) only has to hand over a connected
 * [BitBoxCommandExecutor]. The BitBox counterpart of [LedgerTransactionSigner], minus the part
 * Ledger needs and BitBox does not: there is no registration HMAC to cache, because the device
 * self-reports whether it already knows the wallet.
 *
 * Steps:
 *  1. Read the connected device's fingerprint and confirm it is the key being signed with —
 *     unlike add-key, signing targets a specific signer, not whatever device is connected.
 *  2. Register the wallet policy on the device if it doesn't already have it
 *     ([BitBoxWalletRegistrar]).
 *  3. Sign the PSBT and return it.
 */
class BitBoxTransactionSigner @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
    private val walletRegistrar: BitBoxWalletRegistrar,
    private val importPsbtUseCase: ImportPsbtUseCase,
) {
    /**
     * The three steps above for a real wallet transaction, plus importing the signed PSBT back
     * into the wallet — unlike a dummy transaction, whose signature the caller extracts itself.
     *
     * @param executor a connected BitBox session for the device the user picked.
     * @param walletId the wallet whose pending transaction is being signed.
     * @param txId the pending transaction to sign.
     * @param expectedXfp master fingerprint of the signer the user tapped "Sign" for.
     * @throws BitBoxWrongDeviceException if the connected device is not [expectedXfp].
     */
    suspend fun sign(
        executor: BitBoxCommandExecutor,
        walletId: String,
        txId: String,
        expectedXfp: String,
    ): Transaction = withContext(ioDispatcher) {
        Timber.tag(TAG).d("sign start walletId=$walletId txId=$txId expectedXfp=$expectedXfp")
        val psbt = nativeSdk.getTransaction(walletId = walletId, txId = txId).psbt
        val signedPsbt = signPsbt(
            executor = executor,
            walletId = walletId,
            psbt = psbt,
            expectedXfp = expectedXfp,
        )

        Timber.tag(TAG).d("importing signed psbt")
        importPsbtUseCase(ImportPsbtUseCase.Param(psbt = signedPsbt, walletId = walletId))
            .getOrThrow()
            .also { Timber.tag(TAG).d("import done; tx status=${it.status} signers=${it.signers}") }
    }

    /**
     * @param executor a connected BitBox session for the device the user picked.
     * @param walletId wallet whose policy the device signs under, loaded from local storage.
     * @throws BitBoxWrongDeviceException if the connected device is not [expectedXfp].
     */
    suspend fun signPsbt(
        executor: BitBoxCommandExecutor,
        walletId: String,
        psbt: String,
        expectedXfp: String,
    ): String = withContext(ioDispatcher) {
        Timber.tag(TAG).d("signPsbt start walletId=$walletId expectedXfp=$expectedXfp")
        signPsbt(
            executor = executor,
            wallet = nativeSdk.getWallet(walletId),
            psbt = psbt,
            expectedXfp = expectedXfp,
        )
    }

    /**
     * Same for a [wallet] that is not in local storage — the sign-in dummy transaction, whose
     * wallet is parsed from the BSMS the user pasted rather than loaded by id.
     *
     * @throws BitBoxWrongDeviceException if the connected device is not [expectedXfp].
     */
    suspend fun signPsbt(
        executor: BitBoxCommandExecutor,
        wallet: Wallet,
        psbt: String,
        expectedXfp: String,
    ): String = withContext(ioDispatcher) {
        val deviceFingerprint = executor.getMasterFingerprint()
        Timber.tag(TAG).d("device fingerprint=$deviceFingerprint")
        if (!deviceFingerprint.equals(expectedXfp, ignoreCase = true)) {
            throw BitBoxWrongDeviceException(expected = expectedXfp, actual = deviceFingerprint)
        }

        Timber.tag(TAG).d(
            "wallet id=${wallet.id} addressType=${wallet.addressType} " +
                "totalRequireSigns=${wallet.totalRequireSigns} signers=${wallet.signers.size} " +
                "psbtLength=${psbt.length}",
        )
        require(psbt.isNotBlank()) { "Transaction has no PSBT to sign" }

        walletRegistrar.withRegisteredWallet(executor, wallet) { policyWallet ->
            executor.signPsbt(policyWallet, psbt)
                .also { Timber.tag(TAG).d("signed psbt length=${it.length}") }
        }
    }

    private companion object {
        private const val TAG = "BitBoxSign"
    }
}
