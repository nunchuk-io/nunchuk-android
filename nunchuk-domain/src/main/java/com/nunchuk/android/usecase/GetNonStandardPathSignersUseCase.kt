package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

/**
 * Returns the `xfp + derivation path` keys ([SingleSigner.pathKey]) of the signers whose BIP32 path
 * isn't the default one for the wallet's address type / wallet type / chain, per
 * `Utils::IsDefaultBip32Path`. Signers without a path are skipped.
 */
class GetNonStandardPathSignersUseCase @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : UseCase<GetNonStandardPathSignersUseCase.Param, Set<String>>(ioDispatcher) {

    override suspend fun execute(parameters: Param): Set<String> {
        return parameters.signers
            .filter { it.derivationPath.isNotEmpty() }
            .filterNot { signer ->
                runCatching {
                    nativeSdk.isDefaultBip32Path(
                        path = signer.derivationPath,
                        addressType = parameters.addressType,
                        walletType = parameters.walletType,
                    )
                }.onFailure { Timber.e(it) }.getOrDefault(true)
            }
            .map { it.pathKey() }
            .toSet()
    }

    data class Param(
        val signers: List<SingleSigner>,
        val addressType: AddressType,
        val walletType: WalletType,
    )
}

fun SingleSigner.pathKey(): String = pathKey(masterFingerprint, derivationPath)

fun pathKey(xfp: String, derivationPath: String): String =
    "${xfp.lowercase()}|${derivationPath.replace('\'', 'h')}"
