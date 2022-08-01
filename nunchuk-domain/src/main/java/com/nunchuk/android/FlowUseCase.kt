package com.nunchuk.android

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

abstract class FlowUseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {
    operator fun invoke(parameters: P): Flow<Result<R>> = execute(parameters)
        .map { Result.success(it) }
        .catch { e -> emit(Result.failure(e)) }
        .flowOn(coroutineDispatcher)

    protected abstract fun execute(parameters: P): Flow<R>
}