package com.nunchuk.android.usecase

import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.AddressType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface DraftWalletUseCase {
    fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String = ""
    ): Flow<String>
}

internal class DraftWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : DraftWalletUseCase {

    override fun execute(
        name: String,
        totalRequireSigns: Int,
        signers: List<SingleSigner>,
        addressType: AddressType,
        isEscrow: Boolean,
        description: String
    ) = flow {
        emit(
            nativeSdk.draftWallet(
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