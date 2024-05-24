package com.nunchuk.android.core.domain.key

import com.nunchuk.android.core.persistence.NcDataStore
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class SetReplacingKeyXfpUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val ncDataStore: NcDataStore
) : UseCase<String, Unit>(dispatcher) {
    override suspend fun execute(parameters: String) {
        ncDataStore.setReplacingXfp(parameters)
    }
}