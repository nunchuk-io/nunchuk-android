package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface JoinWalletUseCase {
    fun execute(roomId: String, signers: List<SingleSigner>): Flow<NunchukMatrixEvent>
}

internal class JoinWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : JoinWalletUseCase {

    override fun execute(roomId: String, signers: List<SingleSigner>) = flow {
        emit(
            signers.mapNotNull {
                try {
                    nativeSdk.joinSharedWallet(roomId, it)
                } catch (t: Throwable) {
                    null
                }
            }.last()
        )
    }

}