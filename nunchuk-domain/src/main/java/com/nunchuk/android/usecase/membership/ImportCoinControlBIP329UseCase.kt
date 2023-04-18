package com.nunchuk.android.usecase.membership

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject

class ImportCoinControlBIP329UseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<ImportCoinControlBIP329UseCase.Param, Unit>(ioDispatcher) {

    override suspend fun execute(parameters: Param) {
        try {
            val reader = File(parameters.data)
            val jsonString = reader.readText()
            nunchukNativeSdk.importCoinControlBIP329(walletId = parameters.walletId, data = jsonString)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    class Param(val walletId: String, val data: String)
}