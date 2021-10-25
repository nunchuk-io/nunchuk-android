package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CreateSignerUseCase {
    fun execute(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ): Flow<SingleSigner>
}

internal class CreateSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CreateSignerUseCase {

    override fun execute(
        name: String,
        xpub: String,
        publicKey: String,
        derivationPath: String,
        masterFingerprint: String
    ) = flow {
        emit(
            nativeSdk.createSigner(
                name = name,
                xpub = xpub,
                publicKey = publicKey,
                derivationPath = derivationPath,
                masterFingerprint = masterFingerprint
            )
        )
    }.catch { CrashlyticsReporter.recordException(it) }
}