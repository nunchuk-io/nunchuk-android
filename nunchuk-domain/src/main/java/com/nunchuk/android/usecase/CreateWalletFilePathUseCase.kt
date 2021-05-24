package com.nunchuk.android.usecase

import android.os.Environment
import com.nunchuk.android.model.Result
import javax.inject.Inject

interface CreateWalletFilePathUseCase {
    suspend fun execute(walletId: String): Result<String>
}

internal class CreateWalletFilePathUseCaseImpl @Inject constructor() : BaseUseCase(), CreateWalletFilePathUseCase {

    override suspend fun execute(walletId: String) = exe {
        "${Environment.getExternalStorageDirectory()}/$walletId"
    }

}