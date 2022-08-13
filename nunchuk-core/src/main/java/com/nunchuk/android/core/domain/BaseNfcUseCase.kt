package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.util.NFC_CARD_TIMEOUT
import com.nunchuk.android.exception.NCNativeException
import com.nunchuk.android.model.TapProtocolException
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import java.io.IOException

abstract class BaseNfcUseCase<P : BaseNfcUseCase.Data, R>(
    dispatcher: CoroutineDispatcher,
    private val waitAutoCardUseCase: WaitAutoCardUseCase
) : UseCase<P, R>(dispatcher) {
    final override suspend fun execute(parameters: P): R {
        parameters.isoDep.timeout = NFC_CARD_TIMEOUT
        parameters.isoDep.connect()
        try {
            if (parameters.isoDep.isConnected) {
                if (waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] == true) {
                    val result = waitAutoCardUseCase(parameters.isoDep)
                    if (result.isFailure) {
                        Timber.d("waitAutoCardUseCase ${result.exceptionOrNull()?.message.orEmpty()}")
                    }
                    result.isSuccess
                }
                val result = executeNfc(parameters)
                if (isAutoRemoveRateLimit) {
                    waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] = false
                }
                return result
            }
        } catch (e: Throwable) {
            if (e is NCNativeException && e.message.contains(TapProtocolException.RATE_LIMIT.toString())) {
                Timber.d("NFC Rate limit")
                waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] = true
            } else if (e is NCNativeException
                && e.message.contains(TapProtocolException.BAD_AUTH.toString())
                && waitAutoCardUseCase.needWaitUnlockTap[String(parameters.isoDep.tag.id)] == true
            ) {
                // if bad_auth in case tap signer still rate limit, should throw rate_limit instead
                throw NCNativeException(TapProtocolException.RATE_LIMIT.toString())
            }
            throw e
        } finally {
            runCatching { parameters.isoDep.close() }
        }
        throw IOException("Can not connect nfc card")
    }

    protected abstract suspend fun executeNfc(parameters: P): R

    open class Data(val isoDep: IsoDep)

    protected open val isAutoRemoveRateLimit: Boolean = true
}