package com.nunchuk.android.usecase

import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Deprecated("Should migrate to Kotlin Flow")
abstract class BaseUseCase {

    protected suspend fun <T> exe(func: suspend () -> T) = withContext(Dispatchers.IO) {
        try {
            Success(func())
        } catch (e: Exception) {
            Error(e)
        }
    }

}