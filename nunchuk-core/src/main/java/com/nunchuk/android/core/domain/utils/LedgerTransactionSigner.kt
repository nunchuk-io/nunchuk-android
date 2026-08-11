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
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.transaction.ImportPsbtUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Confluence "2. Sign transaction" flow, encapsulated as an injectable coordinator so the
 * transport UI (a thin activity/sheet owning the BLE/USB connection) only has to provide a
 * connected [LedgerCommandExecutor]. Reusable by any caller that has a connected Ledger.
 *
 * Steps:
 *  1. Read the connected device's master fingerprint and confirm it is the key we intend
 *     to sign with ([expectedXfp]) — unlike add-key, signing must target a specific signer,
 *     not whatever device happens to be connected.
 *  2. Reuse the cached wallet-registration HMAC ([GetLedgerWalletHmacUseCase]); register the
 *     wallet on the device if there isn't one yet and persist the returned HMAC.
 *  3. Sign the PSBT. If the device rejects the HMAC ([SW_INVALID_SIGNATURE_OR_HMAC]) the
 *     stored registration is stale — clear it, re-register and retry once (Confluence §6).
 *  4. Import the signed PSBT back into the wallet and return the updated [Transaction].
 */
class LedgerTransactionSigner @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeSdk: NunchukNativeSdk,
    private val getLedgerWalletHmacUseCase: GetLedgerWalletHmacUseCase,
    private val setLedgerWalletHmacUseCase: SetLedgerWalletHmacUseCase,
    private val importPsbtUseCase: ImportPsbtUseCase,
) {
    /**
     * @param executor a connected Ledger session for the device the user picked.
     * @param walletId the wallet whose pending transaction is being signed.
     * @param txId the pending transaction to sign.
     * @param expectedXfp master fingerprint of the signer the user tapped "Sign" for.
     * @throws LedgerWrongDeviceException if the connected device is not [expectedXfp].
     */
    suspend fun sign(
        executor: LedgerCommandExecutor,
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

        Timber.tag(TAG).d("signed psbt length=${signedPsbt.length}; importing")
        importPsbtUseCase(ImportPsbtUseCase.Param(psbt = signedPsbt, walletId = walletId)).getOrThrow()
            .also { Timber.tag(TAG).d("import done; tx status=${it.status} signers=${it.signers}") }
    }

    /**
     * Steps 1-3 of the flow above for a PSBT that isn't a wallet transaction — a dummy
     * transaction, whose signed PSBT is turned into a signature by the caller instead of being
     * imported into [walletId].
     *
     * @param psbt the PSBT to hand the device.
     * @return the signed PSBT.
     * @throws LedgerWrongDeviceException if the connected device is not [expectedXfp].
     */
    suspend fun signPsbt(
        executor: LedgerCommandExecutor,
        walletId: String,
        psbt: String,
        expectedXfp: String,
    ): String = withContext(ioDispatcher) {
        Timber.tag(TAG).d("signPsbt start walletId=$walletId expectedXfp=$expectedXfp")
        val deviceFingerprint = executor.getMasterFingerprint()
        Timber.tag(TAG).d("device fingerprint=$deviceFingerprint")
        if (!deviceFingerprint.equals(expectedXfp, ignoreCase = true)) {
            throw LedgerWrongDeviceException(expected = expectedXfp, actual = deviceFingerprint)
        }

        val wallet = nativeSdk.getWallet(walletId)
        Timber.tag(TAG).d(
            "wallet loaded id=${wallet.id} addressType=${wallet.addressType} totalRequireSigns=${wallet.totalRequireSigns} signers=${wallet.signers.size} psbtLength=${psbt.length}",
        )
        require(psbt.isNotBlank()) { "Transaction has no PSBT to sign" }

        var hmac = getLedgerWalletHmacUseCase(walletId).getOrThrow()
        Timber.tag(TAG).d("cached hmac ${if (hmac.isBlank()) "empty -> registering wallet" else "present (len=${hmac.length})"}")
        if (hmac.isBlank()) {
            hmac = executor.registerWallet(wallet)
            Timber.tag(TAG).d("registerWallet done hmacLen=${hmac.length}")
            setLedgerWalletHmacUseCase(SetLedgerWalletHmacUseCase.Param(walletId, hmac)).getOrThrow()
        }

        try {
            Timber.tag(TAG).d("signPsbt (hmacLen=${hmac.length})")
            executor.signPsbt(wallet, hmac, psbt)
        } catch (e: LedgerCommandException) {
            Timber.tag(TAG).e("signPsbt failed statusWord=0x%04X msg=%s", e.statusWord, e.message)
            if (e.statusWord != SW_INVALID_SIGNATURE_OR_HMAC) throw e
            // Stored HMAC is stale (another device / changed policy): re-register and retry.
            Timber.tag(TAG).d("stale hmac -> clearing + re-registering")
            setLedgerWalletHmacUseCase(SetLedgerWalletHmacUseCase.Param(walletId, "")).getOrThrow()
            hmac = executor.registerWallet(wallet)
            setLedgerWalletHmacUseCase(SetLedgerWalletHmacUseCase.Param(walletId, hmac)).getOrThrow()
            executor.signPsbt(wallet, hmac, psbt)
        }
    }

    private companion object {
        private const val TAG = "LedgerSign"
    }
}

/**
 * The connected Ledger's master fingerprint does not match the signer the user is trying to
 * sign with — they connected the wrong device. Surfaced so the UI can ask for the right one.
 */
class LedgerWrongDeviceException(
    val expected: String,
    val actual: String,
) : Exception("Connected Ledger ($actual) does not match the selected key ($expected)")
