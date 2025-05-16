package com.nunchuk.android.usecase.transaction

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.repository.TransactionRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class GetTaprootKeySetSelectionUseCase @Inject constructor(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val repository: TransactionRepository,
) : UseCase<String, Int?>(ioDispatcher) {
    override suspend fun execute(parameters: String): Int? {
        return repository.getTaprootKeySetSelection(parameters)
    }
}