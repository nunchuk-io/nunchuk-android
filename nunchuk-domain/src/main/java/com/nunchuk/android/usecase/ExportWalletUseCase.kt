package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.ExportFormat
import javax.inject.Inject

interface ExportWalletUseCase {
    suspend fun execute(
        walletId: String,
        filePath: String,
        format: ExportFormat
    ): Result<Boolean>
}

internal class ExportWalletUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), ExportWalletUseCase {

    override suspend fun execute(
        walletId: String,
        filePath: String,
        format: ExportFormat
    ) = exe {
        nativeSdk.exportWallet(
            walletId = walletId,
            filePath = filePath,
            format = format
        )
    }

}