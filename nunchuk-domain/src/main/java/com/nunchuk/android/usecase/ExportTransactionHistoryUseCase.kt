package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.ExportFormat
import javax.inject.Inject

interface ExportTransactionHistoryUseCase {
    suspend fun execute(
        walletId: String,
        filePath: String,
        format: ExportFormat = ExportFormat.CSV
    ): Result<Unit>
}

internal class ExportTransactionHistoryUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk
) : BaseUseCase(), ExportTransactionHistoryUseCase {
    override suspend fun execute(
        walletId: String,
        filePath: String,
        format: ExportFormat
    ) = exe {
        nativeSdk.exportTransactionHistory(walletId = walletId, filePath = filePath, format = format)
    }
}