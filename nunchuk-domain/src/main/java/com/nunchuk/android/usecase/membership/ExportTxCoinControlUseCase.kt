package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class ExportTxCoinControlUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<ExportTxCoinControlUseCase.Param, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Param) {
        try {
            val file = File(parameters.filePath)
            val fileWriter = FileWriter(file)
            val data = nunchukNativeSdk.exportCoinControlData(walletId = parameters.walletId)
            fileWriter.write(data)
            fileWriter.close()
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    class Param(val walletId: String, val filePath: String)
}