package com.nunchuk.android.main.membership.key

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.core.domain.signer.GetSignerFromTapsignerMasterSignerByPathUseCase
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetUnusedSignerFromMasterSignerV2UseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Turns a TAPSIGNER the app already holds into the key to put on the wallet.
 *
 * A TAPSIGNER is a master signer: the key at the wallet's derivation path has to be derived from
 * it, and the xpub for that path is often not cached on the phone — the only way to read it is
 * another tap on the card. That round trip is identical on both assisted key lists, so it lives
 * here rather than being written twice.
 */
class TapSignerKeyResolver @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val getUnusedSignerFromMasterSignerV2UseCase: GetUnusedSignerFromMasterSignerV2UseCase,
    private val getSignerFromTapsignerMasterSignerByPathUseCase: GetSignerFromTapsignerMasterSignerByPathUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
) {

    sealed interface Outcome {
        data class Resolved(val signer: SingleSigner) : Outcome

        /** The xpub is not on the phone; the card has to be tapped before it can be read. */
        data object NeedsCardTap : Outcome

        data class Failed(val message: String) : Outcome
    }

    /** The card whose xpub is being waited on, kept between [resolve] and [resolveAfterCardTap]. */
    private var pendingMasterSignerId: String? = null

    suspend fun resolve(masterSignerId: String): Outcome {
        val masterSigner = getAllSignersUseCase(false).getOrNull()
            ?.first
            ?.firstOrNull { it.id == masterSignerId }
            ?: return Outcome.Failed("Master signer not found with id=$masterSignerId")

        return getUnusedSignerFromMasterSignerV2UseCase(
            GetUnusedSignerFromMasterSignerV2UseCase.Params(
                masterSigners = masterSigner,
                walletType = WalletType.MULTI_SIG,
                addressType = AddressType.NATIVE_SEGWIT,
            )
        ).fold(
            onSuccess = { Outcome.Resolved(it) },
            onFailure = { error ->
                if (error.nativeErrorCode() == NativeErrorCode.XPUB_NOT_CACHED) {
                    pendingMasterSignerId = masterSignerId
                    Outcome.NeedsCardTap
                } else {
                    Outcome.Failed(error.message.orUnknownError())
                }
            }
        )
    }

    suspend fun resolveAfterCardTap(isoDep: IsoDep, cvc: String): Outcome {
        val masterSignerId = pendingMasterSignerId
            ?: return Outcome.Failed("No TAPSIGNER is waiting to be read")
        return getSignerFromTapsignerMasterSignerByPathUseCase(
            GetSignerFromTapsignerMasterSignerByPathUseCase.Data(
                isoDep = isoDep,
                masterSignerId = masterSignerId,
                path = multisigPath(),
                cvc = cvc,
            )
        ).fold(
            onSuccess = {
                pendingMasterSignerId = null
                Outcome.Resolved(it)
            },
            onFailure = {
                pendingMasterSignerId = null
                Outcome.Failed(it.message.orUnknownError())
            }
        )
    }

    /** The first account of the recommended multisig path, which is what an assisted wallet uses. */
    private suspend fun multisigPath(): String {
        val isTestNet = getChainSettingFlowUseCase(Unit)
            .map { it.getOrDefault(Chain.MAIN) }
            .first() == Chain.TESTNET
        return if (isTestNet) "m/48h/1h/0h/2h" else "m/48h/0h/0h/2h"
    }
}
