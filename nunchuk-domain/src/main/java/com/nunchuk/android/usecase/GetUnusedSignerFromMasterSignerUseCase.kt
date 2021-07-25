package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GetUnusedSignerFromMasterSignerUseCase {
    suspend fun execute(
        masterSignerId: String,
        walletType: WalletType,
        addressType: AddressType
    ): Flow<SingleSigner>
}

internal class GetUnusedSignerFromMasterSignerUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : GetUnusedSignerFromMasterSignerUseCase {

    override suspend fun execute(
        masterSignerId: String,
        walletType: WalletType,
        addressType: AddressType
    ) = flow {
        emit(
            nativeSdk.getUnusedSignerFromMasterSigner(
                masterSignerId,
                walletType,
                addressType
            )
        )
    }

}