package com.nunchuk.android.core.domain

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface HasSignerUseCase {
    fun execute(
        signer: SingleSigner
    ): Flow<Boolean>
}

internal class HasSignerUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : HasSignerUseCase {

    override fun execute(
        signer: SingleSigner
    ) = flow {
        emit(
            nunchukNativeSdk.hasSigner(signer)
        )
    }

}