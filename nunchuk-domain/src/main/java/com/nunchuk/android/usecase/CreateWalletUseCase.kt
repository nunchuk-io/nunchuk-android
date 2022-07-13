package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.properties.Delegates

interface CreateWalletUseCase {
    fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String = ""
    ): Flow<Wallet>
}

internal class CreateWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), CreateWalletUseCase {

    override fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String
    ) = flow {
        emit(
            nativeSdk.createWallet(
                name = name,
                totalRequireSigns = totalRequireSigns,
                signers = signers,
                addressType = addressType,
                isEscrow = isEscrow,
                description = description
            )
        )
    }
}