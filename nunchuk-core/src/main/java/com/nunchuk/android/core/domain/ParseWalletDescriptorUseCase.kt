package com.nunchuk.android.core.domain

import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface ParseWalletDescriptorUseCase {
    fun execute(
        content: String
    ): Flow<Wallet>
}

internal class ParseWalletDescriptorUseCaseImpl @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk
) : ParseWalletDescriptorUseCase {

    override fun execute(
        content: String
    ) = flow {
        emit(
            nunchukNativeSdk.parseWalletDescriptor(content)
        )
    }

}