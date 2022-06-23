package com.nunchuk.android.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.contracts.contract

abstract class UseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {
    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute(parameters).let {
                    Result.success(it)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}