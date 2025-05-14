package com.nunchuk.android.usecase

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTaprootKeySetSelectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: TransactionRepository,
) : UseCase<GetTaprootKeySetSelectionUseCase.Param, Int?>(ioDispatcher) {
    override suspend fun execute(parameters: Param): Int? {
        return repository.getTaprootKeySetSelection(parameters.transactionId)
    }

    data class Param(val transactionId: String)
} 