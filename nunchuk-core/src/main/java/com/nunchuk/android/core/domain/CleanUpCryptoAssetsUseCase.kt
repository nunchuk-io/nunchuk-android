package com.nunchuk.android.core.domain

import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CleanUpCryptoAssetsUseCase {
    fun execute(): Flow<Unit>
}

internal class CleanUpCryptoAssetsUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CleanUpCryptoAssetsUseCase {

    override fun execute() = flow {
        try {
            nativeSdk.getWallets().forEach { nativeSdk.deleteWallet(it.id) }
            nativeSdk.getMasterSigners().forEach { nativeSdk.deleteMasterSigner(it.id) }
            nativeSdk.getRemoteSigners().forEach { nativeSdk.deleteRemoteSigner(it.masterSignerId, it.masterFingerprint) }
        } catch (t: Throwable) {
            CrashlyticsReporter.recordException(t)
        }
        emit(Unit)
    }

}
