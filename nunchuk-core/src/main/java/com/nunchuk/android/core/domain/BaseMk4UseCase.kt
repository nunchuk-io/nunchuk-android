package com.nunchuk.android.core.domain

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.IOException

abstract class BaseMk4UseCase<P : BaseMk4UseCase.Data>(
    dispatcher: CoroutineDispatcher,
) : UseCase<P, Unit>(dispatcher) {
    final override suspend fun execute(parameters: P) {
        parameters.ndef.connect()
        try {
            if (parameters.ndef.isConnected) {
                val result = executeNfc(parameters)
                if (result.isNotEmpty()) {
                    parameters.ndef.writeNdefMessage(NdefMessage(result))
                }
            } else {
                throw IOException("Can not connect nfc card")
            }
        } catch (e: Throwable) {
            throw e
        } finally {
            runCatching { parameters.ndef.close() }
        }
    }

    protected abstract suspend fun executeNfc(parameters: P): Array<NdefRecord>

    open class Data(val ndef: Ndef)
}