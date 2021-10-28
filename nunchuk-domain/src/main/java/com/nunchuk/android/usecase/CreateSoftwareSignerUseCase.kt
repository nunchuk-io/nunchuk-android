package com.nunchuk.android.usecase

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface CreateSoftwareSignerUseCase {
    fun execute(name: String, mnemonic: String, passphrase: String): Flow<MasterSigner>
}

internal class CreateSoftwareSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : CreateSoftwareSignerUseCase {

    override fun execute(name: String, mnemonic: String, passphrase: String) = flow {
        emit(
            nativeSdk.createSoftwareSigner(
                name = name,
                mnemonic = mnemonic,
                passphrase = passphrase
            )
        )
    }.catch { CrashlyticsReporter.recordException(it) }

}