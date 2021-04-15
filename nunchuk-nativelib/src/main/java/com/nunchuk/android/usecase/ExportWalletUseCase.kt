package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.LibNunchukFacade
import com.nunchuk.android.type.ExportFormat
import javax.inject.Inject

interface ExportWalletUseCase {
    suspend fun execute(
        walletId: String,
        filePath: String,
        format: ExportFormat = ExportFormat.COLDCARD
    ): Result<Boolean>
}

internal class ExportWalletUseCaseImpl @Inject constructor(
    private val libNunchukFacade: LibNunchukFacade
) : BaseUseCase(), ExportWalletUseCase {

    override suspend fun execute(
        walletId: String,
        filePath: String,
        format: ExportFormat
    ) = exe {
        libNunchukFacade.exportWallet(
            walletId = walletId,
            filePath = filePath,
            format = format
        )
    }

}