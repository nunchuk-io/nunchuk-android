package com.nunchuk.android.usecase.transaction

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.TransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SaveTaprootKeySetSelectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: TransactionRepository,
) : UseCase<SaveTaprootKeySetSelectionUseCase.Param, Unit>(ioDispatcher) {
    override suspend fun execute(parameters: Param) {
        repository.saveTaprootKeySetSelection(parameters.transactionId, parameters.keySetIndex)
    }

    data class Param(val transactionId: String, val keySetIndex: Int)
}