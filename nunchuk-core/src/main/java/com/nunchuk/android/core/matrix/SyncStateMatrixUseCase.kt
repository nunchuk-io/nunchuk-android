package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.data.model.SyncStateMatrixResponse
import com.nunchuk.android.core.repository.MatrixAPIRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SyncStateMatrixUseCase {
    fun execute(): Flow<SyncStateMatrixResponse>
}

internal class SyncStateMatrixUseCaseImpl @Inject constructor(
    private val matrixAPIRepository: MatrixAPIRepository
) : SyncStateMatrixUseCase {

    override fun execute() = matrixAPIRepository.syncState()
}
