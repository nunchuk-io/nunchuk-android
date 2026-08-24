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
import com.nunchuk.android.model.SignedMessage
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Turns a signature a BitBox produced into the [SignedMessage] the other signers return —
 * Confluence "4. Sign message":
 *
 * ```
 * ExportBitcoinSignedMessage(BitcoinSignedMessage{message, signing_address, signature})
 * ```
 *
 * The address is **not** the signer's own. BitBox signs with the compact-signature key below it
 * ([GetBitBoxSignMessagePathUseCase]), and `GetBitBoxSignMessageAddress` is the address of that
 * key — the one a verifier has to check the signature against. [GetSignedMessageUseCase] derives
 * the address from the signer instead, which is right for Ledger (it signs at the signer's own
 * path) and would name the wrong address here, producing a block that verifies nowhere.
 *
 * The RFC2440 block is assembled here rather than natively because no binding takes an explicit
 * address — `getSignedMessage` only takes a signer. The delimiters are the ones libnunchuk itself
 * emits, so the output matches what every other signer produces; if an
 * `ExportBitcoinSignedMessage` binding is ever added, this should call it and drop the assembly.
 */
class GetBitBoxSignedMessageUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
) : UseCase<GetBitBoxSignedMessageUseCase.Param, SignedMessage>(dispatcher) {

    override suspend fun execute(parameters: Param): SignedMessage {
        val address = nunchukNativeSdk.getBitBoxSignMessageAddress(parameters.signer)
        require(address.isNotBlank()) { "BitBox sign-message address is unavailable" }
        return SignedMessage(
            address = address,
            signature = parameters.signature,
            rfc2440 = rfc2440(
                message = parameters.message,
                address = address,
                signature = parameters.signature,
            ),
        )
    }

    private fun rfc2440(message: String, address: String, signature: String): String = buildString {
        appendLine(BEGIN_MESSAGE)
        appendLine(message)
        appendLine(BEGIN_SIGNATURE)
        appendLine(address)
        appendLine(signature)
        append(END_SIGNATURE)
    }

    data class Param(
        val signer: SingleSigner,
        val message: String,
        val signature: String,
    )

    private companion object {
        private const val BEGIN_MESSAGE = "-----BEGIN BITCOIN SIGNED MESSAGE-----"
        private const val BEGIN_SIGNATURE = "-----BEGIN BITCOIN SIGNATURE-----"
        private const val END_SIGNATURE = "-----END BITCOIN SIGNATURE-----"
    }
}
