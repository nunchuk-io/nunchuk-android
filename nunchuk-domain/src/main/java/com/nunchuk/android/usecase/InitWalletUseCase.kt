package com.nunchuk.android.usecase

import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface InitWalletUseCase {
    fun execute(
        roomId: String,
        name: String,
        totalSigns: Int,
        requireSigns: Int,
        addressType: AddressType,
        isEscrow: Boolean,
        des: String,
        signers: List<SingleSigner>
    ): Flow<NunchukMatrixEvent>
}

internal class InitWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : InitWalletUseCase {

    override fun execute(
        roomId: String,
        name: String,
        totalSigns: Int,
        requireSigns: Int,
        addressType: AddressType,
        isEscrow: Boolean,
        des: String,
        signers: List<SingleSigner>
    ) = flow {
        emit(
            nativeSdk.initSharedWallet(
                roomId = roomId,
                name = name,
                totalSigns = totalSigns,
                requireSigns = requireSigns,
                addressType = addressType,
                isEscrow = isEscrow,
                des = des,
                signers = signers
            )
        )
    }
}