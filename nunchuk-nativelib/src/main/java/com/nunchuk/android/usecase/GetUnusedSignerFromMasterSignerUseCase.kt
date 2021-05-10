package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.LibNunchukFacade
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import javax.inject.Inject

interface GetUnusedSignerFromMasterSignerUseCase {
    suspend fun execute(
        masterSignerId: String,
        walletType: WalletType,
        addressType: AddressType
    ): Result<SingleSigner>
}

internal class GetUnusedSignerFromMasterSignerUseCaseImpl @Inject constructor(
    private val nunchukAndroid: LibNunchukFacade
) : BaseUseCase(), GetUnusedSignerFromMasterSignerUseCase {

    override suspend fun execute(
        masterSignerId: String,
        walletType: WalletType,
        addressType: AddressType
    ) = exe {
        nunchukAndroid.getUnusedSignerFromMasterSigner(
            masterSignerId,
            walletType,
            addressType
        )
    }

}