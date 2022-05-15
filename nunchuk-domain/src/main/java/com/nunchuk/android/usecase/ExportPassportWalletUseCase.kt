package com.nunchuk.android.usecase

import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

interface ExportPassportWalletUseCase {
    fun execute(walletId: String): Flow<List<String>>
}

internal class ExportPassportWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : ExportPassportWalletUseCase {

    override fun execute(walletId: String) = flow {
        emit(nativeSdk.exportPassportWallet(walletId))
    }.flowOn(Dispatchers.IO)

}