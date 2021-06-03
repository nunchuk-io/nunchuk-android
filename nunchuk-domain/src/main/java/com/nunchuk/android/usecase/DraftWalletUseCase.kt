package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import javax.inject.Inject

interface DraftWalletUseCase {
    suspend fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String = ""
    ): Result<String>
}

internal class DraftWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), DraftWalletUseCase {

    override suspend fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String
    ) = exe {
        nativeSdk.draftWallet(
            name = name,
            totalRequireSigns = totalRequireSigns,
            signers = signers,
            addressType = addressType,
            isEscrow = isEscrow,
            description = description
        )
    }
}