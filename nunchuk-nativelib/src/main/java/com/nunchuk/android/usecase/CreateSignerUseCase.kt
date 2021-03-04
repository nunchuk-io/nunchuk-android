package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukAndroid
import com.nunchuk.android.nativelib.LibNunchukFacade
import io.reactivex.Single
import javax.inject.Inject

interface CreateSignerUseCase {
    fun execute(
            name: String,
            xpub: String,
            publicKey: String,
            derivationPath: String,
            masterFingerprint: String
    ): Single<SingleSigner>
}

internal class CreateSignerUseCaseImpl @Inject constructor(
        private val nunchukFacade: LibNunchukFacade
) : CreateSignerUseCase {

    override fun execute(
            name: String,
            xpub: String,
            publicKey: String,
            derivationPath: String,
            masterFingerprint: String
    ) = Single.fromCallable {
        nunchukFacade.createSigner(
                name = name,
                xpub = xpub,
                publicKey = publicKey,
                derivationPath = derivationPath,
                masterFingerprint = masterFingerprint
        )
    }
}

fun createCreateSignerUseCase(): CreateSignerUseCase = CreateSignerUseCaseImpl(LibNunchukFacade(LibNunchukAndroid()))