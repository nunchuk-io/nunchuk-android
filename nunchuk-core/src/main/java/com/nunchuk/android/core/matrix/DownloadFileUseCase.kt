package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.repository.MatrixAPIRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/*
* Android Matrix SDK does not have a native upload func, so we have to call rest api
* */
class DownloadFileUseCase @Inject constructor(
    private val matrixAPIRepository: MatrixAPIRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : UseCase<DownloadFileUseCase.Params, String>(dispatcher) {

    override suspend fun execute(parameters: Params): String {
        return matrixAPIRepository.download(parameters.serverName, parameters.mediaId)
    }

    data class Params(val serverName: String, val mediaId: String)
}
