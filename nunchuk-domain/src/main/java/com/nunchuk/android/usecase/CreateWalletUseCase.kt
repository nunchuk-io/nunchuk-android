package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.LibNunchukFacade
import com.nunchuk.android.type.AddressType
import javax.inject.Inject

interface CreateWalletUseCase {
    suspend fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String = ""
    ): Result<Wallet>
}

internal class CreateWalletUseCaseImpl @Inject constructor(
    private val nunchukFacade: LibNunchukFacade
) : BaseUseCase(), CreateWalletUseCase {

    override suspend fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String
    ) = exe {
        nunchukFacade.createWallet(
            name = name,
            totalRequireSigns = totalRequireSigns,
            signers = signers,
            addressType = addressType,
            isEscrow = isEscrow,
            description = description
        )
    }
}